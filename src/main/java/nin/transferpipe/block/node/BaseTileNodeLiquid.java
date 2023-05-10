package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.forge.TileLiquidSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;

import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;

/**
 * 液体ノードの搬入出部分
 */
public abstract class BaseTileNodeLiquid extends BaseTileNode {

    /**
     * 初期化
     */
    public final TileLiquidSlot<BaseTileNodeLiquid> liquidSlot;
    public final ItemStackHandler dummyLiquidItem;
    public final int baseSpeed = 250;
    public final int baseCapacity = 16000;

    public BaseTileNodeLiquid(BlockEntityType<? extends BaseTileNode> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
        dummyLiquidItem = new ItemStackHandler();
        liquidSlot = new TileLiquidSlot<>(baseCapacity, this, dummyLiquidItem);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return orSuper(ForgeCapabilities.FLUID_HANDLER, liquidSlot, cap, side);
    }

    @Override
    public int wi() {
        return super.wi() * baseSpeed;
    }

    @Override
    public void calcUpgrades() {
        super.calcUpgrades();
        liquidSlot.capacityRate = capacityRate;
    }

    /**
     * 液体搬出
     */
    public boolean canExtract(IFluidHandler handler) {
        return IntStream.range(0, handler.getTanks())
                .filter(slot -> shouldReceive(handler.getFluidInTank(slot)))
                .anyMatch(slot -> {
                    var liquid = handler.getFluidInTank(slot);
                    var toDrain = ForgeUtils.copyWithAmount(liquid, getExtractableAmount(liquid, false));
                    var drained = handler.drain(toDrain, SIMULATE);
                    return drained.getAmount() > 0;
                });
    }

    public void tryExtract(IFluidHandler handler) {
        IntStream.range(0, handler.getTanks())
                .filter(slot -> shouldReceive(handler.getFluidInTank(slot)))
                .findFirst().ifPresent(slot -> {
                    var liquid = handler.getFluidInTank(slot);
                    var toDrain = ForgeUtils.copyWithAmount(liquid, getExtractableAmount(liquid, false));
                    var drained = handler.drain(getExtractableAmount(toDrain, false), EXECUTE);
                    liquidSlot.receive(drained);
                });
    }

    public boolean shouldReceive(FluidStack liquid) {
        return liquidSlot.canStack(liquid) && liquidFilter.test(liquid);
    }

    public int getExtractableAmount(FluidStack liquid, boolean byWorldInteraction) {
        var pullableAmount = stackMode ? liquidSlot.getCapacity() : baseSpeed;

        if (byWorldInteraction)
            pullableAmount = Math.max(pullableAmount, wi());

        return Math.min(pullableAmount, getReceivableAmount(liquid, byWorldInteraction));
    }

    public int getReceivableAmount(FluidStack liquid, boolean byWorldInteraction) {
        var receivableAmount = liquidSlot.getCapacity() - liquidSlot.getAmount();

        if (byWorldInteraction)
            receivableAmount = Math.max(receivableAmount, wi() - liquidSlot.getAmount());

        return Math.min(liquid.getAmount(), receivableAmount);
    }

    /**
     * 液体搬入
     */
    public boolean canInsert(IFluidHandler handler) {
        return liquidSlot.getFluid() != insert(handler, SIMULATE);
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
        return ForgeUtils.copyWithAmount(self, filteredAmount + remainder);
    }

    public FluidStack getInsertableFluid(IFluidHandler handler, FluidStack self) {
        if (self.isEmpty())
            return FluidStack.EMPTY;

        var ration = liquidRation - ForgeUtils.countFluid(handler, self);
        return ForgeUtils.copyWithAmount(self, Math.min(ration, self.getAmount()));
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
