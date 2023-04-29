package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.gui.ReferenceCraftingGrid;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.forge.TileItemSlot;
import nin.transferpipe.util.java.JavaUtils;
import nin.transferpipe.util.transferpipe.TPUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class TileTransferNodeItem extends TileBaseTransferNode {

    /**
     * 初期化
     */
    public final TileItemSlot<TileTransferNodeItem> itemSlot;

    public TileTransferNodeItem(BlockPos p_155229_, BlockState p_155230_) {
        super(TPBlocks.TRANSFER_NODE_ITEM.tile(), p_155229_, p_155230_);
        itemSlot = new TileItemSlot<>(this);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return orSuper(ForgeCapabilities.ITEM_HANDLER, itemSlot, cap, side);
    }

    @Override
    public boolean shouldSearch() {
        return !itemSlot.isEmpty();
    }

    @Override
    public Vector3f getColor() {
        return new Vector3f(1, 0, 0);
    }

    /**
     * アイテム搬入
     */
    @Override
    public void facing(BlockPos pos, Direction dir) {
        if (itemSlot.getFreeSpace() > 0)
            if (ForgeUtils.hasItemHandler(level, pos, dir))
                ForgeUtils.forItemHandler(level, pos, dir, this::tryExtract);
            else if (worldInteraction > 0)
                tryWorldInteraction(pos, dir);
    }

    public void tryExtract(IItemHandler inv) {
        IntStream.range(0, inv.getSlots())
                .filter(slot -> shouldReceive(inv.getStackInSlot(slot)))
                .findFirst().ifPresent(slot -> {
                    var extractableAmount = getExtractableAmount(inv.getStackInSlot(slot), false);
                    itemSlot.receive(inv.extractItem(slot, extractableAmount, false));
                });
    }

    public boolean shouldReceive(ItemStack item) {
        return itemSlot.canStack(item) && filteringFunc.test(item);
    }

    public int getExtractableAmount(ItemStack toExtract, boolean byWorldInteraction) {
        var extractionSpeed = stackMode ? itemSlot.getMaxStackSize() : 1;
        if (byWorldInteraction)
            extractionSpeed = Math.max(extractionSpeed, wi());

        return Math.min(extractionSpeed, getReceivableAmount(toExtract, byWorldInteraction));
    }

    public int getReceivableAmount(ItemStack toReceive, boolean byWorldInteraction) {
        var freeSpace = itemSlot.getFreeSpace();
        if (byWorldInteraction)
            freeSpace = Math.max(freeSpace, itemSlot.getFreeSpace(wi()));

        return Math.min(toReceive.getCount(), freeSpace);
    }

    /**
     * アイテム搬出
     */
    @Override
    public boolean canWork(BlockPos pos, Direction d) {
        return ForgeUtils.getItemHandler(level, pos, d).map(this::canInsert).orElse(false);
    }

    public boolean canInsert(IItemHandler inv) {
        return itemSlot.getItem() != insert(inv, true);
    }

    @Override
    public void work(BlockPos pos, Direction dir) {
        if (!itemSlot.isEmpty())
            ForgeUtils.forItemHandler(level, pos, dir, this::tryInsert);
    }

    public void tryInsert(IItemHandler inv) {
        if (canInsert(inv))
            itemSlot.setItem(insert(inv, false));
    }

    public ItemStack insert(IItemHandler inv, boolean simulate) {
        var self = itemSlot.getItem();
        var itemToInsert = getInsertableItem(inv, self);
        if (itemToInsert.isEmpty())
            return self;//failed

        var remainder = ItemHandlerHelper.insertItemStacked(inv, itemToInsert, simulate);
        if (itemToInsert == remainder)
            return self;//failed

        var filteredAmount = self.getCount() - itemToInsert.getCount();
        return self.copyWithCount(filteredAmount + remainder.getCount());//succeeded and return remainder
    }

    public ItemStack getInsertableItem(IItemHandler inv, ItemStack self) {
        if (self.isEmpty())
            return ItemStack.EMPTY;

        //test sort
        if (!sortingFunc.test(ForgeUtils.toItemList(inv), self.getItem()))
            return ItemStack.EMPTY;

        //consider ration
        var ration = itemRation - ForgeUtils.countItem(inv, self);
        return self.copyWithCount(Math.min(ration, self.getCount()));
    }

    /**
     * World Interaction
     */
    public void tryWorldInteraction(BlockPos pos, Direction dir) {
        if (getBlock(pos) == Blocks.AIR)
            tryVacuum(pos, dir.getOpposite());
        else if (isAutoCraftMode())
            tryAutoCraft();
        else if (isProduct(pos))
            tryGenLiquidReactionProduct(pos);
    }

    public void tryVacuum(BlockPos pos, Direction boxDir) {
        var boxSize = 1 + 2 * JavaUtils.log(2, worldInteraction);
        var boxCenter = TPUtils.relative(pos, boxDir, boxSize / 2);
        var box = AABB.ofSize(boxCenter, boxSize, boxSize, boxSize);
        for (ItemEntity dropItem : level.getEntitiesOfClass(ItemEntity.class, box).stream()
                .filter(i -> shouldReceive(i.getItem())).toList()) {
            var item = dropItem.getItem();
            var extraction = item.copyWithCount(getExtractableAmount(item, true));
            itemSlot.receive(extraction);
            var remainder = TPUtils.copyWithSub(item, extraction);
            dropItem.setItem(remainder);

            //end if node cannot suck any more items
            if (!remainder.isEmpty()) {
                if (addParticle)
                    addEdges(boxCenter, (float) boxSize / 2);
                break;
            }
        }
    }

    public List<BlockPos> inventoryPozzes = new ArrayList<>();

    @Override
    public void onUpdateFacing() {
        super.onUpdateFacing();
        updateInventoryPozzes();
        if (level != null)
            updateRecipe();
    }

    public static final Supplier<List<Vector3f>> north = () -> IntStream.rangeClosed(-1, 1).map(i -> -i).boxed().flatMap(y -> IntStream.rangeClosed(-1, 1).boxed().map(x ->
            new Vector3f(x, y, -1))).toList();
    public static final Map<Direction, List<BlockPos>> relativeInventoryPositions = TPUtils.dirMap(d ->
            north.get().stream()
                    .map(v -> TPUtils.rotation(d).transform(v))
                    .map(TPUtils::toPos)
                    .toList());

    public void updateInventoryPozzes() {
        inventoryPozzes = relativeInventoryPositions.get(FACING).stream().map(FACING_POS::offset).toList();
    }

    public ReferenceCraftingGrid craftSlots = new ReferenceCraftingGrid(this);
    public List<BlockPos> itemPositions = new ArrayList<>();
    @Nullable
    public CraftingRecipe recipe = null;

    @Override
    public void beforeTick() {
        if (worldInteraction > 0)
            if (isAutoCraftMode() && JavaUtils.fork(recipe == null, shouldUpdate(), craftSlots.hasInvalidInventories()))
                updateRecipe();
    }

    public boolean isAutoCraftMode() {
        return getBlock(FACING_POS) == Blocks.CRAFTING_TABLE;
    }

    public boolean shouldUpdate() {
        return (level.getGameTime() & 0xF) == 0;
    }

    public CraftingRecipe updateRecipe() {
        craftSlots.clear();
        itemPositions.clear();

        if (inventoryPozzes.size() == 0)
            updateInventoryPozzes();//な　ぜ　か　本当になぜか　上で初期化してるのに、初期化前の状態で来る。どうして？？？

        IntStream.range(0, craftSlots.getContainerSize()).forEach(i ->
                ForgeUtils.forFirstItemSlot(level, inventoryPozzes.get(i), FACING, (inventory, slot) -> {
                    craftSlots.setItem(i, inventory, slot);
                    if (addParticle)
                        itemPositions.add(inventoryPozzes.get(i));
                }));
        recipe = level.getServer().getRecipeManager()
                .getRecipesFor(RecipeType.CRAFTING, craftSlots, level).stream()
                .filter(recipe -> shouldReceive(recipe.assemble(craftSlots, level.registryAccess())))
                .findFirst()
                .orElse(null);
        return recipe;
    }

    public void tryAutoCraft() {
        if (recipe != null && !(craftSlots.getMinAmount() <= 0 && updateRecipe() == null)) {
            var item = recipe.assemble(craftSlots, level.registryAccess());
            var craftableTimes = Math.min(craftSlots.getMinAmount(), wi());
            var craftableItems = TPUtils.copyWithScale(item, craftableTimes);
            var receivableTimes = getReceivableAmount(craftableItems, true) / item.getCount();
            if (receivableTimes > 0) {
                var receivableItems = TPUtils.copyWithScale(item, receivableTimes);
                var remainders = TPUtils.scaleItems(recipe.getRemainingItems(craftSlots), receivableTimes);

                itemSlot.receive(receivableItems);
                craftSlots.consume(receivableTimes, remainders);
                itemPositions.forEach(this::addBlockParticle);
            }
        }
    }

    private boolean isProduct(BlockPos pos) {
        return getBlock(pos) == Blocks.COBBLESTONE;
    }

    public void tryGenLiquidReactionProduct(BlockPos pos) {
        var block = getBlock(pos);
        var item = block.asItem().getDefaultInstance();
        if (shouldReceive(item))
            if (block == Blocks.COBBLESTONE && isBetween(pos, Blocks.WATER, Blocks.LAVA)) {//TODO 丸石以外も
                var generatableItems = item.copyWithCount((int) worldInteraction);
                var receivableItems = item.copyWithCount(getReceivableAmount(generatableItems, true));
                itemSlot.receive(receivableItems);
            }
    }

    public boolean isBetween(BlockPos pos, Block b1, Block b2) {
        return Direction.stream().anyMatch(dir ->
                getBlockState(pos.relative(dir)).is(b1) && getBlockState(pos.relative(dir.getOpposite())).is(b2));
    }

    /**
     * NBT
     */
    public static final String ITEM_SLOT = "ItemSlot";

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(ITEM_SLOT, itemSlot.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(ITEM_SLOT))
            itemSlot.deserializeNBT(tag.getCompound(ITEM_SLOT));
    }
}
