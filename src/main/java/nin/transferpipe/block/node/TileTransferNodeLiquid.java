package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.particle.ColorSquare;
import nin.transferpipe.util.HandlerUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;

public class TileTransferNodeLiquid extends TileBaseTransferNode {

    /**
     * 基本情報
     */

    public final FluidTank liquidSlot;
    public final ItemStackHandler dummyLiquidItem;

    public static final String LIQUID_SLOT = "LiquidSlot";
    private final int baseSpeed = 200;

    public TileTransferNodeLiquid(BlockPos p_155229_, BlockState p_155230_) {
        super(TPBlocks.TRANSFER_NODE_LIQUID.tile(), p_155229_, p_155230_);
        dummyLiquidItem = new ItemStackHandler();
        liquidSlot = new HandlerUtils.TileLiquid<>(8000, this, dummyLiquidItem);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == ForgeCapabilities.FLUID_HANDLER ? LazyOptional.of(() -> liquidSlot).cast() : super.getCapability(cap, side);
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

    /**
     * 機能
     */

    @Override
    public boolean shouldSearch() {
        return !liquidSlot.isEmpty();
    }

    @Override
    public void facing(BlockPos pos, Direction dir) {
        if (liquidSlot.getFluidAmount() <= liquidSlot.getCapacity())
            HandlerUtils.forFluidHandler(level, pos, dir, this::tryPull);
    }

    public void tryPull(IFluidHandler handler) {
        forFirstPullableSlot(handler, slot -> {
            var drained = handler.drain(getPullAmount(handler, slot), EXECUTE);
            liquidSlot.fill(drained, EXECUTE);
        });
    }

    public void forFirstPullableSlot(IFluidHandler handler, IntConsumer func) {
        IntStream.range(0, handler.getTanks())
                .filter(slot -> shouldPull(handler, slot))
                .findFirst().ifPresent(func);
    }

    public boolean shouldPull(IFluidHandler handler, int slot) {
        return getPullAmount(handler, slot) != 0;
    }

    public int getPullAmount(IFluidHandler handler, int slot) {
        var fluid = handler.getFluidInTank(slot);
        var pullableAmount = stackMode ? fluid.getAmount() : baseSpeed;
        var drained = handler.drain(pullableAmount, SIMULATE);
        var filled = liquidSlot.fill(drained, SIMULATE);
        return filled;
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
        var originalFluid = liquidSlot.getFluid().copy();
        var insertedAmount = new AtomicInteger(0);
        IntStream.range(0, handler.getTanks())
                .filter(slot -> handler.isFluidValid(slot, liquidSlot.getFluid()))
                .takeWhile(slot -> !liquidSlot.isEmpty())
                .forEach(slot -> insertedAmount.addAndGet(liquidSlot.drain(handler.fill(liquidSlot.getFluid(), action), EXECUTE).getAmount()));

        if (action == SIMULATE) {
            originalFluid.setAmount(insertedAmount.get());
            liquidSlot.fill(originalFluid, EXECUTE);
        }

        return insertedAmount.get() != 0;
    }

    @Override
    public boolean canWork(BlockPos pos, Direction d) {
        var canInsert = new AtomicBoolean(false);
        HandlerUtils.forFluidHandler(level, pos, d, handler -> canInsert.set(canInsert(handler)));
        return canInsert.get();
    }

    @Override
    public ColorSquare.Option getParticleOption() {
        return new ColorSquare.Option(0, 0, 1, 1);
    }
}
