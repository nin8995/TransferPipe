package nin.transferpipe.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

public class HandlerUtils {

    public static void forItemHandler(Level level, BlockPos pos, Direction dir, NonNullConsumer<? super IItemHandler> func) {
        getItemHandler(level, pos, dir).ifPresent(func);
    }

    public static boolean hasItemHandler(Level level, BlockPos pos, Direction dir) {
        return getItemHandler(level, pos, dir).isPresent();
    }

    public static Map<Container, LazyOptional<IItemHandler>> containerCache = new HashMap<>();

    public static LazyOptional<IItemHandler> getItemHandler(Level level, BlockPos pos, Direction dir) {
        var be = level.getBlockEntity(pos);
        if (be != null) {
            var lo = be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir);
            if (lo.isPresent())
                return lo;
        }

        var container = getContainer(level, pos);
        return container != null ?
                container instanceof WorldlyContainer wc ? containerCache.computeIfAbsent(container, it -> SidedInvWrapper.create(wc, dir)[0].cast())
                        : containerCache.computeIfAbsent(container, it -> LazyOptional.of(() -> new InvWrapper(container)))
                : LazyOptional.empty();
    }

    @Nullable
    public static Container getContainer(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Container c ? c
                : level.getBlockState(pos).getBlock() instanceof WorldlyContainerHolder holder ? holder.getContainer(level.getBlockState(pos), level, pos)
                : null;
    }

    public static void forFirstItemSlot(Level level, BlockPos pos, Direction dir, BiConsumer<IItemHandler, Integer> func) {
        forItemHandler(level, pos, dir, handler -> IntStream
                .range(0, handler.getSlots())
                .filter(i -> !handler.extractItem(i, 1, true).isEmpty())
                .findFirst().ifPresent(i -> func.accept(handler, i))
        );
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

        public void refreshItemFluid() {
            var fluidItem = new FluidHandlerItemStack(Items.ENDER_DRAGON_SPAWN_EGG.getDefaultInstance(), Integer.MAX_VALUE);
            fluidItem.fill(getFluid(), FluidAction.EXECUTE);
            dummyLiquidItem.setStackInSlot(0, fluidItem.getContainer());
        }
    }

    public static void forEnergyStorage(Level level, BlockPos pos, Direction dir, NonNullConsumer<? super IEnergyStorage> func) {
        var optional = getEnergyStorageOptional(level, pos, dir);
        if (optional != null)
            optional.ifPresent(func);
    }

    public static boolean hasEnergyStorage(Level level, BlockPos pos, Direction dir) {
        var optional = getEnergyStorageOptional(level, pos, dir);
        return optional != null && optional.isPresent();
    }

    public static LazyOptional<IEnergyStorage> getEnergyStorageOptional(Level level, BlockPos pos, Direction dir) {
        var be = level.getBlockEntity(pos);
        return be != null ? be.getCapability(ForgeCapabilities.ENERGY, dir) : null;
    }

    public static class TileEnergy<T extends BlockEntity> extends EnergyStorage {

        public final T be;


        public TileEnergy(int capacity, int maxReceive, int maxExtract, T be) {
            super(capacity, maxReceive, maxExtract, 0);
            this.be = be;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            be.setChanged();
            return super.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            be.setChanged();
            return super.extractEnergy(maxExtract, simulate);
        }
    }
}
