package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.forge.RegistryGUIEntityBlock;
import nin.transferpipe.util.java.JavaUtils;
import nin.transferpipe.util.minecraft.BaseBlockMenu;
import nin.transferpipe.util.minecraft.MCUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class TransferNodeItem extends BaseNodeBlock.Facing<TransferNodeItem.Tile> {

    @Override
    public RegistryGUIEntityBlock<Tile> registryWithGUI() {
        return TPBlocks.TRANSFER_NODE_ITEM;
    }

    @Override
    public BaseBlockMenu menu(TransferNodeItem.Tile tile, int id, Inventory inv) {
        return new Menu(tile.itemSlot, tile.upgrades, tile.searchData, id, inv);
    }

    public static class Menu extends BaseNodeMenu.Item {

        //client
        public Menu(int containerId, Inventory inv, FriendlyByteBuf buf) {
            this(new ItemStackHandler(), new ItemStackHandler(6), new SimpleContainerData(4), containerId, inv);
        }

        //server
        public Menu(IItemHandler slot, IItemHandler upgrades, ContainerData searchData, int containerId, Inventory inv) {
            super(TPBlocks.TRANSFER_NODE_ITEM, slot, upgrades, searchData, containerId, inv);
        }
    }

    public static class Screen extends BaseNodeScreen.Item<Menu> {

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
                tryExtract(pos, dir.getOpposite());
            else if (isAutoCraftMode())
                tryAutoCraft();
            else if (isProduct(pos))
                tryGenLiquidReactionProduct(pos, dir);
        }

        public void tryExtract(BlockPos pos, Direction boxDir) {
            var boxSize = 1 + 2 * JavaUtils.log(2, worldInteraction);
            var boxCenter = MCUtils.relative(pos, boxDir, boxSize / 2);
            var box = AABB.ofSize(boxCenter, boxSize, boxSize, boxSize);
            var invEntities = MCUtils.getMappableMappedEntities(level, box, ForgeUtils::getItemHandler);
            var toExtract = itemSlot.hasItem()
                            ? itemSlot.getItem()
                            : ForgeUtils.findFirst(invEntities, itemFilter);
            if (!invEntities.isEmpty() && toExtract != null) {
                var remainingExtractionPower = getExtractionSpeed(toExtract, true);
                for (IItemHandler inv : JavaUtils.filter(invEntities, inv -> canExtract(inv, toExtract, true))) {
                    if (remainingExtractionPower <= 0)
                        break;
                    remainingExtractionPower = tryExtract(inv, toExtract, remainingExtractionPower, true);
                }

                if (addParticle)
                    addEdges(boxCenter, (float) boxSize / 2);
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
            return getBlock(pos) == Blocks.COBBLESTONE || getBlock(pos) == Blocks.STONE;
        }

        public void tryGenLiquidReactionProduct(BlockPos pos, Direction dir) {
            var block = getBlock(pos);
            var item = block.asItem().getDefaultInstance();
            if (shouldReceive(item))//TODO 一般の液体生成物について
                if (block == Blocks.COBBLESTONE && canGenerateCobbleStone(pos, dir)
                        || block == Blocks.STONE && canGenerateStone(pos, dir)) {
                    var generatableItems = item.copyWithCount(wi());
                    var receivableItems = item.copyWithCount(getReceivableAmount(generatableItems, true));
                    itemSlot.receive(receivableItems);
                }
        }

        public boolean canGenerateCobbleStone(BlockPos pos, Direction dir) {
            var blocks = MCUtils.horizontalDirectionsExcept(dir).map(pos::relative).map(this::getBlock).toList();
            return blocks.contains(Blocks.WATER) && blocks.contains(Blocks.LAVA);
        }

        public boolean canGenerateStone(BlockPos pos, Direction dir) {
            return getBlock(pos.relative(Direction.UP)) == Blocks.LAVA
                    && MCUtils.horizontalDirectionsExcept(dir).map(pos::relative).map(this::getBlock).anyMatch(b -> b == Blocks.WATER);
        }

        public boolean isBetween(BlockPos pos, Block b1, Block b2) {
            return Direction.stream().anyMatch(dir ->
                    getBlockState(pos.relative(dir)).is(b1) && getBlockState(pos.relative(dir.getOpposite())).is(b2));
        }
    }
}
