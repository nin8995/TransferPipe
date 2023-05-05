package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.gui.BaseBlockMenu;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.minecraft.MCUtils;
import org.joml.Vector3f;

public class TransferNodeLiquid extends BaseNodeBlock.Facing<TransferNodeLiquid.Tile> {

    @Override
    public TPBlocks.RegistryGUIEntityBlock<TransferNodeLiquid.Tile> registryWithGUI() {
        return TPBlocks.TRANSFER_NODE_LIQUID;
    }

    @Override
    public BaseBlockMenu menu(TransferNodeLiquid.Tile tile, int id, Inventory inv) {
        return new Menu(tile.dummyLiquidItem, tile.upgrades, tile.searchData, id, inv);
    }

    public static class Menu extends BaseNodeMenu.Liquid {

        public Menu(int containerId, Inventory inv, FriendlyByteBuf buf) {
            this(new ItemStackHandler(), new ItemStackHandler(6), new SimpleContainerData(4), containerId, inv);
        }

        public Menu(IItemHandler dummyLiquidItem, IItemHandler upgrades, ContainerData searchData, int containerId, Inventory inv) {
            super(TPBlocks.TRANSFER_NODE_LIQUID, dummyLiquidItem, upgrades, searchData, containerId, inv);
        }
    }

    public static class Screen extends BaseNodeScreen.Liquid<Menu> {

        public Screen(Menu p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }
    }

    public static class Tile extends BaseTileNodeLiquid {

        public Tile(BlockPos p_155229_, BlockState p_155230_) {
            super(TPBlocks.TRANSFER_NODE_LIQUID.tile(), p_155229_, p_155230_);
        }

        @Override
        public boolean shouldSearch() {
            return !liquidSlot.isEmpty();
        }

        @Override
        public Vector3f getColor() {
            return new Vector3f(0, 0, 1);
        }

        @Override
        public void facing(BlockPos pos, Direction dir) {
            if (liquidSlot.hasFreeSpace())
                if (ForgeUtils.hasItemHandler(level, pos, dir))
                    ForgeUtils.forFluidHandler(level, pos, dir, this::tryExtract);
                else if (worldInteraction > 0) {
                    if (isInfiniteLiquid(pos))
                        tryGenInfiniteLiquid(pos);
                }
        }

        public boolean isInfiniteLiquid(BlockPos pos) {
            return level.getFluidState(pos).canConvertToSource(level, pos);
        }

        public void tryGenInfiniteLiquid(BlockPos pos) {
            var fluid = getFluid(pos);
            var stack = new FluidStack(fluid, wi());
            if (shouldReceive(stack) && hasTwoNeighbor(pos, fluid))
                liquidSlot.receive(ForgeUtils.copyWithAmount(stack, getExtractableAmount(stack, true)));
        }

        public boolean hasTwoNeighbor(BlockPos pos, Fluid fluid) {
            return MCUtils.horizontals.stream().filter(d -> level.getFluidState(pos).isSourceOfType(fluid)).count() >= 2;
        }

        @Override
        public boolean canWork(BlockPos pos, Direction d) {
            return ForgeUtils.getFluidHandler(level, pos, d).filter(this::canInsert).isPresent();
        }

        @Override
        public void work(BlockPos pos, Direction dir) {
            ForgeUtils.forFluidHandler(level, pos, dir, this::tryInsert);
        }
    }
}
