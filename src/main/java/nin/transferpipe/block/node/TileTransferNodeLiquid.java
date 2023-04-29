package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.forge.TileLiquidSlot;
import nin.transferpipe.util.transferpipe.TPUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.stream.IntStream;

import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;

public class TileTransferNodeLiquid extends TileBaseTransferNode {

    /**
     * 初期化
     */
    public final TileLiquidSlot<TileTransferNodeLiquid> liquidSlot;
    public final ItemStackHandler dummyLiquidItem;
    public final int baseSpeed = 250;
    public final int baseCapacity = 16000;

    public TileTransferNodeLiquid(BlockPos p_155229_, BlockState p_155230_) {
        super(TPBlocks.TRANSFER_NODE_LIQUID.tile(), p_155229_, p_155230_);
        dummyLiquidItem = new ItemStackHandler();
        liquidSlot = new TileLiquidSlot<>(baseCapacity, this, dummyLiquidItem);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return orSuper(ForgeCapabilities.FLUID_HANDLER, liquidSlot, cap, side);
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
    public int wi() {
        return super.wi() * baseSpeed;
    }

    /**
     * 液体搬出
     */
    @Override
    public void facing(BlockPos pos, Direction dir) {
        if (liquidSlot.getFluidAmount() < liquidSlot.getCapacity())
            if (ForgeUtils.hasItemHandler(level, pos, dir))
                ForgeUtils.forFluidHandler(level, pos, dir, this::tryExtract);
            else if (worldInteraction > 0) {
                if (isInfiniteLiquid(pos))
                    tryGenInfiniteLiquid(pos);
            }
    }

    public void tryExtract(IFluidHandler handler) {
        IntStream.range(0, handler.getTanks())
                .filter(slot -> shouldReceive(handler.getFluidInTank(slot)))
                .findFirst().ifPresent(slot -> {
                    var toDrain = handler.getFluidInTank(slot);
                    var drained = handler.drain(getExtractableAmount(toDrain, false), EXECUTE);
                    liquidSlot.receive(drained);
                });
    }

    public boolean shouldReceive(FluidStack fluid) {
        return liquidSlot.canStack(fluid);
    }

    public int getExtractableAmount(FluidStack fluid, boolean byWorldInteraction) {
        var pullableAmount = stackMode ? liquidSlot.getCapacity() : baseSpeed;

        if (byWorldInteraction)
            pullableAmount = Math.max(pullableAmount, wi());

        return Math.min(pullableAmount, getReceivableAmount(fluid, byWorldInteraction));
    }

    public int getReceivableAmount(FluidStack fluid, boolean byWorldInteraction) {
        var receivableAmount = liquidSlot.getCapacity() - liquidSlot.getAmount();

        if (byWorldInteraction)
            receivableAmount = Math.max(receivableAmount, wi() - liquidSlot.getAmount());

        return Math.min(fluid.getAmount(), receivableAmount);
    }

    public boolean isInfiniteLiquid(BlockPos pos) {
        return level.getFluidState(pos).canConvertToSource(level, pos);
    }

    public void tryGenInfiniteLiquid(BlockPos pos) {
        var fluid = getFluid(pos);
        var stack = new FluidStack(fluid, wi());
        if (shouldReceive(stack) && hasTwoNeighbor(pos, fluid))
            liquidSlot.receive(TPUtils.copyWithAmount(stack, getExtractableAmount(stack, true)));
    }

    public boolean hasTwoNeighbor(BlockPos pos, Fluid fluid) {
        return Direction.stream().filter(d -> level.getFluidState(pos).isSourceOfType(fluid)).count() >= 2;
    }

    @Override
    public boolean canWork(BlockPos pos, Direction d) {
        return ForgeUtils.getFluidHandler(level, pos, d).map(this::canInsert).orElse(false);
    }

    public boolean canInsert(IFluidHandler handler) {
        return liquidSlot.getFluid() != insert(handler, SIMULATE);
    }

    @Override
    public void work(BlockPos pos, Direction dir) {
        ForgeUtils.forFluidHandler(level, pos, dir, this::tryInsert);
    }

    public void tryInsert(IFluidHandler handler) {
        if (canInsert(handler))
            liquidSlot.setFluid(insert(handler, EXECUTE));
    }

    public FluidStack insert(IFluidHandler tanks, IFluidHandler.FluidAction action) {
        var self = liquidSlot.getFluid();
        var fluidToInsert = getInsertableFluid(tanks, self);
        if (fluidToInsert.isEmpty())
            return self;

        var remainder = fluidToInsert.getAmount() - tanks.fill(fluidToInsert, action);
        if (remainder == fluidToInsert.getAmount())
            return self;

        var filteredAmount = self.getAmount() - fluidToInsert.getAmount();
        return TPUtils.copyWithAmount(self, filteredAmount + remainder);
    }

    public FluidStack getInsertableFluid(IFluidHandler handler, FluidStack self) {
        if (self.isEmpty())
            return FluidStack.EMPTY;

        var ration = liquidRation - ForgeUtils.countFluid(handler, self);
        return TPUtils.copyWithAmount(self, Math.min(ration, self.getAmount()));
    }

    /**
     * NBT
     */
    public static final String LIQUID_SLOT = "LiquidSlot";

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
}
