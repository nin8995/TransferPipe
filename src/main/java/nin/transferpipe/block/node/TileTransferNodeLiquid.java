package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.util.HandlerUtils;
import nin.transferpipe.util.TPUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;

public class TileTransferNodeLiquid extends TileBaseTransferNode {

    public final FluidTank liquidSlot;
    public final ItemStackHandler dummyLiquidItem;

    public static final String LIQUID_SLOT = "LiquidSlot";
    private final int baseSpeed = 250;

    public TileTransferNodeLiquid(BlockPos p_155229_, BlockState p_155230_) {
        super(TPBlocks.TRANSFER_NODE_LIQUID.tile(), p_155229_, p_155230_);
        dummyLiquidItem = new ItemStackHandler();
        liquidSlot = new HandlerUtils.TileLiquid<>(16000, this, dummyLiquidItem);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return orSuper(ForgeCapabilities.FLUID_HANDLER, liquidSlot, cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(LIQUID_SLOT, liquidSlot.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(LIQUID_SLOT))
            liquidSlot.readFromNBT(tag.getCompound(LIQUID_SLOT));
    }

    @Override
    public boolean shouldSearch() {
        return !liquidSlot.isEmpty();
    }

    @Override
    public void facing(BlockPos pos, Direction dir) {
        if (liquidSlot.getFluidAmount() < liquidSlot.getCapacity())
            if (HandlerUtils.hasItemHandler(level, pos, dir))
                HandlerUtils.forFluidHandler(level, pos, dir, this::tryPull);
            else if (worldInteraction > 0) {
                var fluid = level.getFluidState(FACING_POS).getType();
                if (isInfiniteLiquid(fluid))
                    trySuckInfiniteLiquid(FACING_POS, fluid);
            }
    }

    public void tryPull(IFluidHandler handler) {
        forFirstPullableSlot(handler, slot -> {
            var drained = handler.drain(getPullableAmount(handler, slot, false), EXECUTE);
            liquidSlot.fill(drained, EXECUTE);
        });
    }

    public void forFirstPullableSlot(IFluidHandler handler, IntConsumer func) {
        IntStream.range(0, handler.getTanks())
                .filter(slot -> shouldPull(handler, slot))
                .findFirst().ifPresent(func);
    }

    public boolean shouldPull(IFluidHandler handler, int slot) {
        return getPullableAmount(handler, slot, false) != 0;
    }

    public int getPullableAmount(IFluidHandler handler, int slot, boolean byWorldInteraction) {
        var fluid = handler.getFluidInTank(slot);
        var pullableAmount = stackMode ? fluid.getAmount() : baseSpeed;

        if (byWorldInteraction)
            pullableAmount = Math.max(pullableAmount, fluidWI());

        var drained = handler.drain(pullableAmount, SIMULATE);
        return liquidSlot.fill(drained, SIMULATE) == 0 ? 0 : Math.min(drained.getAmount(), getReceivableAmount(byWorldInteraction));
    }

    public int getReceivableAmount(boolean byWorldInteraction) {
        var receivableAmount = liquidSlot.getCapacity() - liquidSlot.getFluidAmount();

        if (byWorldInteraction)
            receivableAmount = Math.max(receivableAmount, fluidWI() - liquidSlot.getFluidAmount());

        return receivableAmount;
    }

    public boolean isInfiniteLiquid(Fluid fluid) {
        return fluid == Fluids.WATER;
    }

    public void trySuckInfiniteLiquid(BlockPos pos, Fluid fluid) {
        if (Direction.stream().filter(d -> isLiquidSource(pos.relative(d), fluid)).count() >= 2) {
            var water = new FluidTank(Integer.MAX_VALUE);
            water.setFluid(new FluidStack(Fluids.WATER, fluidWI()));
            var amount = getPullableAmount(water, 0, true);
            receive(new FluidStack(Fluids.WATER, amount));
        }
    }

    public void receive(FluidStack fluid) {
        if (liquidSlot.isEmpty())
            liquidSlot.setFluid(fluid);
        else
            liquidSlot.setFluid(TPUtils.copyWithAddition(liquidSlot.getFluid(), fluid.getAmount()));
    }

    public boolean isLiquidSource(BlockPos pos, Fluid fluid) {
        return level.getFluidState(pos).isSourceOfType(fluid);
    }

    public int fluidWI() {
        return (int) (worldInteraction * 250);
    }

    @Override
    public void terminal(BlockPos pos, Direction dir) {
        HandlerUtils.forFluidHandler(level, pos, dir, this::tryPush);
    }

    public void tryPush(IFluidHandler handler) {
        if (canInsert(handler))
            insert(handler, EXECUTE);
    }

    public boolean canInsert(IFluidHandler handler) {
        return insert(handler, SIMULATE);
    }

    public boolean insert(IFluidHandler handler, IFluidHandler.FluidAction action) {
        var remainder = new FluidTank(Integer.MAX_VALUE);
        remainder.setFluid(getRationableFluid(handler));
        var insertedAmount = new AtomicInteger(0);
        IntStream.range(0, handler.getTanks())
                .filter(slot -> handler.isFluidValid(slot, remainder.getFluid()))
                .takeWhile(slot -> !remainder.isEmpty())
                .forEach(slot -> insertedAmount.addAndGet(remainder.drain(handler.fill(remainder.getFluid(), action), EXECUTE).getAmount()));

        if (action != SIMULATE) {
            var insertedFluid = remainder.getFluid();
            insertedFluid.setAmount(insertedAmount.get());
            liquidSlot.drain(insertedFluid, EXECUTE);
        }

        return insertedAmount.get() != 0;
    }

    public FluidStack getRationableFluid(IFluidHandler handler) {
        if (!liquidSlot.isEmpty()) {
            var fluid = liquidSlot.getFluid();
            var liquidAmount = IntStream.range(0, handler.getTanks())
                    .filter(tank -> handler.getFluidInTank(tank).isFluidEqual(fluid))
                    .map(tank -> handler.getFluidInTank(tank).getAmount())
                    .reduce(Integer::sum);

            var maxRation = liquidRation - (liquidAmount.isPresent() ? liquidAmount.getAsInt() : 0);
            if (maxRation > 0) {
                var rationable = fluid.copy();
                rationable.setAmount(Math.min(maxRation, rationable.getAmount()));
                return rationable;
            }
        }

        return FluidStack.EMPTY;
    }

    @Override
    public boolean canWork(BlockPos pos, Direction d) {
        var canInsert = new AtomicBoolean(false);
        HandlerUtils.forFluidHandler(level, pos, d, handler -> canInsert.set(canInsert(handler)));
        return canInsert.get();
    }

    @Override
    public Vector3f getColor() {
        return new Vector3f(0, 0, 1);
    }
}
