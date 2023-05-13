package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.forge.RegistryGUIEntityBlock;
import nin.transferpipe.util.java.JavaUtils;
import nin.transferpipe.util.minecraft.BaseBlockMenu;
import nin.transferpipe.util.minecraft.MCUtils;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        public boolean canFacingWork() {
            return itemSlot.hasFreeSpace();
        }

        @Override
        public void facingWork(BlockPos pos, Direction dir, IItemHandler inv) {
            tryExtract(inv);
        }

        @Override
        public void tryWorldInteraction(BlockPos pos, Direction dir) {
            if (isCrafter(pos))
                tryAutoCraft();
            else if (isProduct(pos))
                tryGenLiquidReactionProduct(pos, dir);
            else
                tryEntityInteraction(pos, dir.getOpposite(), this::tryExtract);
        }

        @Override
        public boolean canWork(IItemHandler inv) {
            return canInsert(inv);
        }

        @Override
        public void work(BlockPos pos, Direction dir, IItemHandler inv) {
            tryInsert(inv);
        }

        @Override
        public Vector3f getColor() {
            return new Vector3f(1, 0, 0);
        }

        /**
         * 自動クラフト
         */
        public boolean isCrafter(BlockPos pos) {
            return getBlock(pos) == Blocks.CRAFTING_TABLE;
        }

        public List<BlockPos> inventoryPozzes = new ArrayList<>();
        public static final Supplier<List<Vector3f>> north = () -> IntStream.rangeClosed(-1, 1).map(i -> -i).boxed().flatMap(y -> IntStream.rangeClosed(-1, 1).boxed().map(x ->
                new Vector3f(x, y, -1))).toList();
        public static final Map<Direction, List<BlockPos>> relativeInventoryPositions = MCUtils.dirMap(d ->
                north.get().stream()
                        .map(v -> MCUtils.rotation(d).transform(v))
                        .map(MCUtils::toPos)
                        .toList());

        @Override
        public void onUpdateFacing() {
            super.onUpdateFacing();
            updateInventoryPozzes();
            if (level != null)
                updateRecipe();
        }

        public void updateInventoryPozzes() {
            inventoryPozzes = relativeInventoryPositions.get(facing).stream().map(facingPos::offset).toList();
        }

        public ReferenceCraftingGrid craftGrid = new ReferenceCraftingGrid(this);
        public Optional<CraftingRecipe> recipeOP = Optional.empty();

        @Override
        public void beforeTick() {
            if (worldInteraction > 0)
                if (isAutoCraftMode() &&
                        JavaUtils.fork(recipeOP.isEmpty(),
                                shouldUpdate(), craftGrid.hasInvalidInventories()))
                    updateRecipe();
        }

        public boolean isAutoCraftMode() {
            return isCrafter(facingPos);
        }

        public boolean shouldUpdate() {
            return (level.getGameTime() & 0xF) == 0;
        }

        public Optional<CraftingRecipe> updateRecipe() {
            if (inventoryPozzes.size() == 0)
                updateInventoryPozzes();//な　ぜ　か　本当になぜか　上で初期化してるのに、初期化前の状態で来る。どうして？？？

            craftGrid.clear();
            IntStream.range(0, craftGrid.getContainerSize()).forEach(i ->
                    ForgeUtils.forFirstItemSlot(level, inventoryPozzes.get(i), facing, (inv, slot) ->
                            craftGrid.setItem(i, inv, slot, inventoryPozzes.get(i))));

            recipeOP = level.getServer().getRecipeManager()
                    .getRecipesFor(RecipeType.CRAFTING, craftGrid, level).stream()
                    .filter(this::canAutoCraft)
                    .findFirst();
            return recipeOP;
        }

        public boolean canAutoCraft(CraftingRecipe recipe) {
            var item = recipe.assemble(craftGrid, level.registryAccess());
            return shouldReceive(item) && getCraftTimes(item) > 0;
        }

        public void tryAutoCraft() {
            if (craftGrid.getMinCount() <= 0)
                updateRecipe();
            recipeOP = recipeOP.filter(this::canAutoCraft);
            recipeOP.ifPresent(recipe -> autoCraft(recipe, getCraftTimes(recipe)));
        }

        private int getCraftTimes(ItemStack item) {
            var craftableTimes = Math.min(craftGrid.getMinCount(), wi());
            var craftableItems = MCUtils.copyWithScale(item, craftableTimes);
            return getReceivableCount(craftableItems) / item.getCount();
        }

        private int getCraftTimes(CraftingRecipe recipe) {
            return getCraftTimes(recipe.assemble(craftGrid, level.registryAccess()));
        }

        private void autoCraft(CraftingRecipe recipe, int craftTimes) {
            var craftItem = MCUtils.copyWithScale(recipe.assemble(craftGrid, level.registryAccess()), craftTimes);
            var remainders = MCUtils.scaleItems(recipe.getRemainingItems(craftGrid), craftTimes);

            itemSlot.insert(craftItem);
            craftGrid.consume(craftTimes, remainders);
            if (addParticle)
                craftGrid.itemPositions.forEach(this::addBlockParticle);
        }

        /**
         * 無限生産
         */
        public boolean isProduct(BlockPos pos) {
            return getBlock(pos) == Blocks.COBBLESTONE || getBlock(pos) == Blocks.STONE;
        }

        public void tryGenLiquidReactionProduct(BlockPos pos, Direction dir) {
            var block = getBlock(pos);
            var item = block.asItem().getDefaultInstance();
            if (shouldReceive(item))//TODO 一般の液体生成物について
                if ((block == Blocks.COBBLESTONE && canGenerateCobbleStone(pos, dir))
                        || (block == Blocks.STONE && canGenerateStone(pos, dir))) {
                    var generatableItems = item.copyWithCount(wi());
                    var receivableItems = item.copyWithCount(getReceivableCount(generatableItems));
                    itemSlot.insert(receivableItems);
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
    }
}
