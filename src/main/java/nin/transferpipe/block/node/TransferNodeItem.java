package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.gui.BaseBlockMenu;
import nin.transferpipe.gui.ReferenceCraftingGrid;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.java.JavaUtils;
import nin.transferpipe.util.minecraft.MCUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class TransferNodeItem extends BaseBlockNode.Facing<TransferNodeItem.Tile> {


    @Override
    public TPBlocks.RegistryGUIEntityBlock<TransferNodeItem.Tile> registryWithGUI() {
        return TPBlocks.TRANSFER_NODE_ITEM;
    }

    @Override
    public BaseBlockMenu menu(TransferNodeItem.Tile tile, int id, Inventory inv) {
        return new Menu(tile.itemSlot, tile.upgrades, tile.searchData, id, inv);
    }

    public static class Menu extends BaseMenuNode.Item {

        //client
        public Menu(int containerId, Inventory inv, FriendlyByteBuf buf) {
            this(new ItemStackHandler(), new ItemStackHandler(6), new SimpleContainerData(4), containerId, inv);
        }

        //server
        public Menu(IItemHandler slot, IItemHandler upgrades, ContainerData data, int containerId, Inventory inv) {
            super(TPBlocks.TRANSFER_NODE_ITEM, slot, upgrades, data, containerId, inv);
        }
    }

    public static class Screen extends BaseScreenNode.Item<Menu> {

        public Screen(Menu p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }
    }


    public static class Tile extends BaseTileNodeItem {

        public Tile(BlockPos p_155229_, BlockState p_155230_) {
            super(TPBlocks.TRANSFER_NODE_ITEM.tile(), p_155229_, p_155230_);
        }

        @Override
        public boolean shouldSearch() {
            return itemSlot.hasItem();
        }

        @Override
        public Vector3f getColor() {
            return new Vector3f(1, 0, 0);
        }

        @Override
        public void facing(BlockPos pos, Direction dir) {
            if (itemSlot.hasFreeSpace())
                if (ForgeUtils.hasItemHandler(level, pos, dir))
                    ForgeUtils.forItemHandler(level, pos, dir, this::tryExtract);
                else if (worldInteraction > 0)
                    tryWorldInteraction(pos, dir);
        }

        @Override
        public boolean canWork(BlockPos pos, Direction d) {
            return ForgeUtils.getItemHandler(level, pos, d).filter(this::canInsert).isPresent();
        }

        @Override
        public void work(BlockPos pos, Direction dir) {
            ForgeUtils.forItemHandler(level, pos, dir, this::tryInsert);
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
            var boxCenter = MCUtils.relative(pos, boxDir, boxSize / 2);
            var box = AABB.ofSize(boxCenter, boxSize, boxSize, boxSize);
            var items = level.getEntitiesOfClass(ItemEntity.class, box);
            var toExtract = itemSlot.isEmpty()
                            ? JavaUtils.findFirst(items, ItemEntity::getItem, filteringFunc)
                            : itemSlot.getItem();
            if (toExtract != null) {
                var remainingExtractionPower = getExtractionSpeed(toExtract, true);
                var toExtracts = JavaUtils.filter(items, i -> ItemHandlerHelper.canItemStacksStack(i.getItem(), toExtract));
                if (!toExtracts.isEmpty()) {
                    for (ItemEntity dropItem : toExtracts) {

                        var item = dropItem.getItem();
                        var extraction = Math.min(getExtractableAmount(item, true), remainingExtractionPower);
                        if (extraction <= 0)
                            break;
                        remainingExtractionPower -= extraction;
                        var extractedItem = item.copyWithCount(extraction);
                        itemSlot.receive(extractedItem);
                        dropItem.setItem(MCUtils.copyWithSub(item, extractedItem));
                    }

                    if (addParticle)
                        addEdges(boxCenter, (float) boxSize / 2);
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
        public static final Map<Direction, List<BlockPos>> relativeInventoryPositions = MCUtils.dirMap(d ->
                north.get().stream()
                        .map(v -> MCUtils.rotation(d).transform(v))
                        .map(MCUtils::toPos)
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
                var craftableItems = MCUtils.copyWithScale(item, craftableTimes);
                var receivableTimes = getReceivableAmount(craftableItems, true) / item.getCount();
                if (receivableTimes > 0) {
                    var receivableItems = MCUtils.copyWithScale(item, receivableTimes);
                    var remainders = MCUtils.scaleItems(recipe.getRemainingItems(craftSlots), receivableTimes);

                    itemSlot.receive(receivableItems);
                    craftSlots.consume(receivableTimes, remainders);
                    itemPositions.forEach(this::addBlockParticle);
                }
            }
        }

        public boolean isProduct(BlockPos pos) {
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
    }
}
