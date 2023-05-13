package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.util.forge.RegistryGUIEntityBlock;
import nin.transferpipe.util.minecraft.BaseBlockMenu;
import org.joml.Vector3f;

public class RetrievalNodeLiquid extends BaseNodeBlock.Facing<RetrievalNodeLiquid.Tile> {

    @Override
    public RegistryGUIEntityBlock<Tile> registryWithGUI() {
        return TPBlocks.RETRIEVAL_NODE_LIQUID;
    }

    @Override
    public BaseBlockMenu menu(Tile tile, int id, Inventory inv) {
        return new Menu(tile.dummyLiquidItem, tile.upgrades, tile.searchData, id, inv);
    }

    public static class Menu extends BaseNodeMenu.Liquid {

        public Menu(int containerId, Inventory inv, FriendlyByteBuf buf) {
            this(new ItemStackHandler(), new ItemStackHandler(6), new SimpleContainerData(4), containerId, inv);
        }

        public Menu(IItemHandler dummyLiquidItem, IItemHandler upgrades, ContainerData searchData, int containerId, Inventory inv) {
            super(TPBlocks.RETRIEVAL_NODE_LIQUID, dummyLiquidItem, upgrades, searchData, containerId, inv);
        }
    }

    public static class Screen extends BaseNodeScreen.Liquid<Menu> {

        public Screen(Menu p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }
    }

    public static class Tile extends BaseTileNodeLiquid {

        public Tile(BlockPos p_155229_, BlockState p_155230_) {
            super(TPBlocks.RETRIEVAL_NODE_LIQUID.tile(), p_155229_, p_155230_);
        }

        @Override
        public void calcUpgrades() {
            super.calcUpgrades();
            pseudoRoundRobin = true;
            stickingSearch = true;
        }

        @Override
        public boolean shouldSearch() {
            return liquidSlot.hasFreeSpace();
        }

        @Override
        public boolean canFacingWork() {
            return liquidSlot.hasLiquid();
        }

        @Override
        public void facingWork(BlockPos pos, Direction dir, IFluidHandler inv) {
            tryInsert(inv);
        }

        @Override
        public void tryWorldInteraction(BlockPos pos, Direction dir) {
            tryEntityInteraction(pos, dir.getOpposite(), this::tryInsert);
        }

        @Override
        public boolean canWork(IFluidHandler inv) {
            return canExtract(inv);
        }

        @Override
        public void work(BlockPos pos, Direction dir, IFluidHandler inv) {
            tryExtract(inv);
        }

        @Override
        public Vector3f getColor() {
            return new Vector3f(0, 1, 1);
        }
    }
}
