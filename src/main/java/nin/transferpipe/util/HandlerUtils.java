package nin.transferpipe.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class HandlerUtils {

    public static void forItemHandler(Level level, BlockPos pos, Direction dir, NonNullConsumer<? super IItemHandler> func) {
        var optional = getItemHandlerOptional(level, pos, dir);
        if (optional != null)
            optional.ifPresent(func);
    }

    public static boolean hasItemHandler(Level level, BlockPos pos, Direction dir) {
        var optional = getItemHandlerOptional(level, pos, dir);
        return optional != null && optional.isPresent();
    }

    public static LazyOptional<IItemHandler> getItemHandlerOptional(Level level, BlockPos pos, Direction dir) {
        var be = level.getBlockEntity(pos);
        return be != null ? be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir) : null;
    }

    public static class TileItem<T extends BlockEntity> extends ItemStackHandler {

        public final T be;

        public TileItem(int size, T be) {
            stacks = NonNullList.withSize(size, ItemStack.EMPTY);
            this.be = be;
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            be.setChanged();
        }
    }

    public static void forFluidHandler(Level level, BlockPos pos, Direction dir, NonNullConsumer<? super IFluidHandler> func) {
        var optional = getFluidHandlerOptional(level, pos, dir);
        if (optional != null)
            optional.ifPresent(func);
    }

    public static boolean hasFluidHandler(Level level, BlockPos pos, Direction dir) {
        var optional = getFluidHandlerOptional(level, pos, dir);
        return optional != null && optional.isPresent();
    }

    public static LazyOptional<IFluidHandler> getFluidHandlerOptional(Level level, BlockPos pos, Direction dir) {
        var be = level.getBlockEntity(pos);
        return be != null ? be.getCapability(ForgeCapabilities.FLUID_HANDLER, dir) : null;
    }

    public static class TileLiquid<T extends BlockEntity> extends FluidTank {

        public final T be;
        private final ItemStackHandler dummyLiquidItem;

        public TileLiquid(int capacity, T be, ItemStackHandler dummyLiquidItem) {
            super(capacity, e -> true);
            this.be = be;
            this.dummyLiquidItem = dummyLiquidItem;
            refreshItemFluid();
        }

        @Override
        protected void onContentsChanged() {
            super.onContentsChanged();
            be.setChanged();
            refreshItemFluid();
        }

        //nbtから読むとき用
        @Override
        public void setFluid(FluidStack stack) {
            super.setFluid(stack);
            refreshItemFluid();
        }

        public void refreshItemFluid(){
            var fluidItem = new FluidHandlerItemStack(Items.ENDER_DRAGON_SPAWN_EGG.getDefaultInstance(), Integer.MAX_VALUE);
             fluidItem.fill(getFluid(), FluidAction.EXECUTE);
            dummyLiquidItem.setStackInSlot(0, fluidItem.getContainer());
        }
    }
}
