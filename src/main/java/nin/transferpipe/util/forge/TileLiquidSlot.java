package nin.transferpipe.util.forge;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.util.transferpipe.TPUtils;

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
        var fluidItem = new FluidHandlerItemStack(Items.ENDER_DRAGON_SPAWN_EGG.getDefaultInstance(), Integer.MAX_VALUE);
        fluidItem.fill(getFluid(), FluidAction.EXECUTE);
        dummyLiquidItem.setStackInSlot(0, fluidItem.getContainer());
    }

    public boolean canStack(FluidStack fluid) {
        return (getFluid().isEmpty() && !fluid.isEmpty()) || fluid.isFluidEqual(getFluid());
    }

    public int getAmount() {
        return getFluidAmount();
    }

    public void receive(FluidStack fluid) {
        if (isEmpty())
            setFluid(fluid);
        else
            setFluid(TPUtils.copyWithAddition(getFluid(), fluid.getAmount()));
    }
}
