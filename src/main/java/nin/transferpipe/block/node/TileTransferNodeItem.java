package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.gui.ReferenceCraftingGrid;
import nin.transferpipe.util.HandlerUtils;
import nin.transferpipe.util.TPUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TileTransferNodeItem extends TileBaseTransferNode {

    private final ItemStackHandler itemSlot;

    public static final String ITEM_SLOT = "ItemSlot";

    public TileTransferNodeItem(BlockPos p_155229_, BlockState p_155230_) {
        super(TPBlocks.TRANSFER_NODE_ITEM.tile(), p_155229_, p_155230_);
        itemSlot = new HandlerUtils.TileItem<>(1, this);
    }

    public IItemHandler getItemSlotHandler() {
        return itemSlot;
    }

    public ItemStack getItemSlot() {
        return itemSlot.getStackInSlot(0);
    }

    public void setItemSlot(ItemStack item) {
        itemSlot.setStackInSlot(0, item);
        setChanged();
    }

    public void receive(ItemStack item) {
        if (!getItemSlot().isEmpty())
            item.setCount(item.getCount() + getItemSlot().getCount());

        setItemSlot(item);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return orSuper(ForgeCapabilities.ITEM_HANDLER, itemSlot, cap, side);
    }

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

    @Override
    public boolean shouldSearch() {
        return !itemSlot.getStackInSlot(0).isEmpty();
    }

    @Override
    public Vector3f getColor() {
        return new Vector3f(1, 0, 0);
    }

    @Override
    public void facing(BlockPos pos, Direction dir) {
        if (getItemSlot().getCount() < getItemSlot().getMaxStackSize())
            if (HandlerUtils.hasItemHandler(level, pos, dir))
                HandlerUtils.forItemHandler(level, pos, dir, this::tryPull);
            else if (worldInteraction > 0) {
                var facingBlock = level.getBlockState(pos).getBlock();
                if (facingBlock == Blocks.AIR)
                    tryVacuum(pos, dir.getOpposite());
                else if (facingBlock == Blocks.CRAFTING_TABLE)
                    tryAutoCraft();
                else if (isProduct(facingBlock))
                    tryGenLiquidReactionProduct(pos, facingBlock);
            }
    }

    public void tryPull(IItemHandler inventory) {
        IntStream.range(0, inventory.getSlots())
                .filter(slot -> shouldPull(inventory.getStackInSlot(slot)))
                .findFirst().ifPresent(slot -> {
                    var extractionAmount = getPullableAmount(inventory.getStackInSlot(slot), false);
                    receive(inventory.extractItem(slot, extractionAmount, false));
                });
    }/*

    public void tryPull(Container container, Direction dir) {
        forFirstPullableSlot(container, dir, slot ->
                receive(container.removeItem(slot, getPullAmount(container.getItem(slot), false))));
    }

    public void forFirstPullableSlot(Container container, Direction dir, IntConsumer func) {
        getSlots(container, dir)
                .filter(slot -> !(container instanceof WorldlyContainer wc && !wc.canTakeItemThroughFace(slot, wc.getItem(slot), dir)))
                .filter(slot -> shouldPull(container.getItem(slot)))
                .findFirst().ifPresent(func);
    }

    //この方角から参照できるスロット番号のstream(WorldlyContainerは方角毎のItemHandlerを持たず、ただ内部コンテナを持っていて、その方角から参照できるコンテナ上のスロット番号を返す。)
    public static IntStream getSlots(Container container, Direction dir) {
        return container instanceof WorldlyContainer wc ? IntStream.of(wc.getSlotsForFace(dir)) : IntStream.range(0, container.getContainerSize());
    }*/

    public boolean shouldPull(ItemStack item) {
        return ((getItemSlot().isEmpty() && !item.isEmpty()) || ItemHandlerHelper.canItemStacksStack(item, getItemSlot()))
                && filteringFunc.test(item);
    }

    /**
     * Calculate ItemStack amount which this node can take in.
     * When ItemStack is pulled by world interaction, pullSpeed and freeSpace can increase, therefore an additional flag is required.
     *
     * @param toPull             ItemStack that is to be pulled in this node
     * @param byWorldInteraction Whether ItemStacks is pulled by world interaction
     */
    public int getPullableAmount(ItemStack toPull, boolean byWorldInteraction) {
        var pullSpeed = stackMode ? toPull.getMaxStackSize() : 1;
        if (byWorldInteraction)
            pullSpeed = Math.max(pullSpeed, (int) worldInteraction);

        return Math.min(pullSpeed, getReceivableAmount(toPull, byWorldInteraction));
    }

    public int getReceivableAmount(ItemStack toReceive, boolean byWorldInteraction) {
        var freeSpace = getItemSlot().getMaxStackSize() - getItemSlot().getCount();

        if (byWorldInteraction)
            freeSpace = Math.max(freeSpace, (int) worldInteraction - getItemSlot().getCount());

        return Math.min(toReceive.getCount(), freeSpace);
    }

    @Override
    public void terminal(BlockPos pos, Direction dir) {
        if (!getItemSlot().isEmpty())
            HandlerUtils.forItemHandler(level, pos, dir, this::tryPush);
    }

    public void tryPush(IItemHandler inventory) {
        if (canInsert(inventory))
            setItemSlot(insert(inventory, false));
    }

    public boolean canInsert(IItemHandler handler) {
        return getItemSlot() != insert(handler, true);
    }

    public ItemStack insert(IItemHandler inventory, boolean simulate) {
        var itemToInsert = getPushableItem(inventory);
        if (itemToInsert.isEmpty())
            return getItemSlot();

        var remainder = ItemHandlerHelper.insertItemStacked(inventory, itemToInsert, simulate);
        return itemToInsert == remainder ? getItemSlot()//failed
                : getItemSlot().copyWithCount(remainder.getCount() + getItemSlot().getCount() - itemToInsert.getCount());//success and remainder
    }/*

    public void tryPush(Container container, Direction dir) {
        if (canInsert(container, dir))
            setItemSlot(insert(container, dir, false));
    }

    public boolean canInsert(Container container, Direction dir) {
        return getItemSlot() != insert(container, dir, true);
    }

    public ItemStack insert(Container container, Direction dir, boolean simulate) {
        var remainder = getPushableItem(container);
        var ration = remainder.getCount();

        for (int slot : getSlots(container, dir)
                .filter(slot -> container.canPlaceItem(slot, remainder)
                        && !(container instanceof WorldlyContainer wc && !wc.canPlaceItemThroughFace(slot, remainder, dir)))
                .toArray()) {
            var item = container.getItem(slot).copy();

            if (shouldAdd(remainder, item)) {
                int addableAmount = item.isEmpty() ? remainder.getMaxStackSize() : item.getMaxStackSize() - item.getCount();
                int addedAmount = Math.min(addableAmount, remainder.getCount());

                if (item.isEmpty()) {
                    var ageea = remainder.copy();
                    ageea.setCount(addedAmount);
                    item = ageea;
                } else {
                    item.setCount(item.getCount() + addedAmount);
                }

                if (!simulate)
                    container.setItem(slot, item);

                remainder.setCount(remainder.getCount() - addedAmount);
            }

            if (remainder.isEmpty())
                break;
        }

        //同じならcopy前のインスタンスを返す（IItemHandler.insertItemと同じ仕様。ItemStack.equalsが多田野参照評価なため、同値性を求める文脈で渡しておいて同一性と同値性を一致させておくが吉）
        if (remainder.getCount() == ration)
            return getItemSlot();
        else {
            var notRationedItem = getItemSlot().copy();
            var notRationedAmount = getItemSlot().getCount() - ration;
            notRationedItem.setCount(remainder.getCount() + notRationedAmount);
            return notRationedItem;
        }
    }*/

    public ItemStack getPushableItem(IItemHandler inventory) {
        var item = getItemSlot();

        //test sort
        var itemList = IntStream.range(0, inventory.getSlots())
                .mapToObj(inventory::getStackInSlot)
                .map(ItemStack::getItem).toList();
        if (!sortingFunc.test(itemList, item.getItem()))
            return ItemStack.EMPTY;

        //consider ration
        var inventoryItemAmount = IntStream.range(0, inventory.getSlots())
                .filter(slot -> ItemHandlerHelper.canItemStacksStack(item, inventory.getStackInSlot(slot)))
                .map(slot -> inventory.getStackInSlot(slot).getCount())
                .reduce(Integer::sum);
        var ration = itemRation - (inventoryItemAmount.isPresent() ? inventoryItemAmount.getAsInt() : 0);
        return ration > 0 ? item.copyWithCount(Math.min(ration, item.getCount()))
                : ItemStack.EMPTY;
    }

    @Override
    public boolean canWork(BlockPos pos, Direction d) {
        return HandlerUtils.getItemHandler(level, pos, d).map(this::canInsert).orElse(false);
    }

    public void tryVacuum(BlockPos pos, Direction boxDir) {
        var boxSize = 1 + 2 * Math.log(worldInteraction) / Math.log(2);
        var boxCenter = pos.getCenter().relative(boxDir, -0.5).add(Vec3.atLowerCornerOf(boxDir.getNormal()).scale(boxSize / 2));
        var box = AABB.ofSize(boxCenter, boxSize, boxSize, boxSize);
        for (ItemEntity dropItem : level.getEntitiesOfClass(ItemEntity.class, box).stream()
                .filter(i -> shouldPull(i.getItem())).toList()) {
            var item = dropItem.getItem();
            var suckAmount = getPullableAmount(item, true);
            var remainderAmount = item.getCount() - suckAmount;

            receive(item.copyWithCount(suckAmount));
            dropItem.setItem(item.copyWithCount(remainderAmount));

            //end if node cannot suck any more items
            if (remainderAmount > 0) {
                if (addParticle)
                    addEdges(boxCenter, (float) boxSize / 2);
                break;
            }
        }
    }

    public static final Supplier<List<Vector3f>> north = () -> IntStream.rangeClosed(-1, 1).map(i -> -i).boxed().flatMap(y ->
            IntStream.rangeClosed(-1, 1).boxed().map(x ->
                    new Vector3f(x, y, -1))).toList();
    public static final Map<Direction, List<Vec3i>> relativeInventoryPositions = Direction.stream().collect(Collectors.toMap(
            d -> d,
            d -> north.get().stream()
                    .map(v -> rotation(d).transform(v))
                    .map(v -> new Vec3i((int) v.x, (int) v.y, (int) v.z))
                    .toList()));

    public static Quaternionf rotation(Direction dir) {
        var q = new Quaternionf();
        var pi = (float) Math.PI;
        return switch (dir) {
            case DOWN -> q.rotationX(-pi / 2);
            case UP -> q.rotationX(pi / 2);
            case NORTH -> q;
            case WEST -> q.rotationY(pi / 2);
            case SOUTH -> q.rotationY(pi);
            case EAST -> q.rotationY(-pi / 2);
        };
    }

    public List<BlockPos> inventoryPozzes = new ArrayList<>();
    public ReferenceCraftingGrid craftSlots = new ReferenceCraftingGrid(this);
    public List<BlockPos> itemPositions = new ArrayList<>();
    @Nullable
    public CraftingRecipe recipe = null;

    @Override
    public void onUpdateFacing() {
        super.onUpdateFacing();
        if (level != null)
            updateRecipe();
    }

    @Override
    public void bodyTick() {
        if (isAutoCraftMode() && (shouldFindRecipe() || craftSlots.hasInvalidInventories()))
            updateRecipe();
        super.bodyTick();
    }

    public boolean isAutoCraftMode() {
        return worldInteraction > 0 && level.getBlockState(FACING_POS).getBlock() == Blocks.CRAFTING_TABLE;
    }

    public boolean shouldFindRecipe() {
        return recipe == null && (level.getGameTime() & 0xF) == 0;
    }

    public CraftingRecipe updateRecipe() {
        craftSlots.clear();
        itemPositions.clear();
        if (inventoryPozzes.size() == 0)
            inventoryPozzes = relativeInventoryPositions.get(FACING).stream().map(FACING_POS::offset).toList();
        IntStream.range(0, craftSlots.getContainerSize()).forEach(i ->
                HandlerUtils.forFirstItemSlot(level, inventoryPozzes.get(i), FACING, (inventory, slot) -> {
                    craftSlots.setItem(i, inventory, slot);
                    if (addParticle)
                        itemPositions.add(inventoryPozzes.get(i));
                }));

        recipe = level.getServer().getRecipeManager()
                .getRecipesFor(RecipeType.CRAFTING, craftSlots, level).stream()
                .filter(recipe -> shouldPull(recipe.assemble(craftSlots, level.registryAccess())))
                .findFirst()
                .orElse(null);
        return recipe;
    }

    public void tryAutoCraft() {
        if (recipe != null && !(craftSlots.getMinAmount() <= 0 && updateRecipe() == null)) {
            var item = recipe.assemble(craftSlots, level.registryAccess());
            var craftableTimes = Math.min(craftSlots.getMinAmount(), (int) worldInteraction);
            var craftableItems = TPUtils.copyWithScale(item, craftableTimes);
            var receivableTimes = getReceivableAmount(craftableItems, true) / item.getCount();
            if (receivableTimes > 0) {
                var receivableItems = TPUtils.copyWithScale(item, receivableTimes);

                receive(receivableItems);
                craftSlots.consume(receivableTimes, recipe.getRemainingItems(craftSlots).stream().map(i -> TPUtils.copyWithScale(i, receivableTimes)).toList());
                itemPositions.stream().map(BlockPos::getCenter).forEach(this::addBlockParticle);
            }
        }
    }

    private boolean isProduct(Block facingBlock) {
        return facingBlock == Blocks.COBBLESTONE;
    }

    public void tryGenLiquidReactionProduct(BlockPos pos, Block product) {
        var item = product.asItem().getDefaultInstance();
        if (shouldPull(item))
            if (product == Blocks.COBBLESTONE && Direction.stream().anyMatch(dir ->
                    level.getBlockState(pos.relative(dir)).is(Blocks.WATER) && level.getBlockState(pos.relative(dir.getOpposite())).is(Blocks.LAVA)))
                receive(item.copyWithCount(getReceivableAmount(item.copyWithCount((int) worldInteraction), true)));
    }
}
