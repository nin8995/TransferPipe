package nin.transferpipe.util.forge;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.util.java.JavaUtils;

public class TileLiquidSlot<T extends BlockEntity> extends FluidTank {

    public final T be;
    private final ItemStackHandler dummyLiquidItem;

    public TileLiquidSlot(int capacity, T be, ItemStackHandler dummyLiquidItem) {
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
        dummyLiquidItem.setStackInSlot(0, ForgeUtils.getFluidItem(getFluid()));
    }

    public boolean canStack(FluidStack fluid) {
        return JavaUtils.fork(getFluid().isEmpty(),
                !fluid.isEmpty(), fluid.isFluidEqual(getFluid()));
    }

    public int getAmount() {
        return getFluidAmount();
    }

    public int getFreeSpace() {
        return getCapacity() - getAmount();
    }

    public int getFreeSpace(int capacity) {
        return capacity - getAmount();
    }

    public boolean hasFreeSpace() {
        return getFreeSpace() > 0;
    }

    public boolean isFull() {
        return getFreeSpace() == 0;
    }

    public boolean hasLiquid() {
        return !isEmpty();
    }

    public void insert(FluidStack fluid) {
        if (isEmpty())
            setFluid(fluid);
        else
            setFluid(ForgeUtils.copyWithAdd(getFluid(), fluid.getAmount()));
    }

    public void extract(FluidStack fluid) {
        setFluid(ForgeUtils.copyWithAdd(getFluid(), fluid.getAmount()));
    }

    public float capacityRate = 1;

    @Override
    public int getCapacity() {
        return (int) (super.getCapacity() * capacityRate);
    }
}
