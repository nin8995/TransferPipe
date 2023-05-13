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
import nin.transferpipe.util.java.JavaUtils;
import nin.transferpipe.util.java.OptionalStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;

/**
 * 液体ノードの搬入出部分
 */
public abstract class BaseTileNodeLiquid extends BaseTileNode<IFluidHandler> {

    /**
     * 初期化
     */
    public final TileLiquidSlot<BaseTileNodeLiquid> liquidSlot;
    public final ItemStackHandler dummyLiquidItem;
    public final int baseSpeed = 250;
    public final int baseCapacity = 16000;

    public BaseTileNodeLiquid(BlockEntityType<? extends BaseTileNode> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_, ForgeUtils::getFluidHandler, ForgeUtils::getFluidHandler);
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
    public boolean canExtract(IFluidHandler inv) {
        return toExtract(inv).isPresent();
    }

    public boolean tryExtract(IFluidHandler inv) {
        return JavaUtils.isPresentAndThen(toExtract(inv), it -> extract(inv, it, getExtractionPower(it)));
    }

    public boolean canExtract(List<IFluidHandler> invs) {
        return toExtract(invs).isPresent();
    }

    public boolean tryExtract(List<IFluidHandler> invs) {
        return JavaUtils.isPresentAndThen(toExtract(invs), item -> extract(invs, item, getExtractionPower(item)));
    }

    public boolean shouldReceive(FluidStack fluid) {
        return liquidSlot.canStack(fluid) && liquidFilter.test(fluid);
    }

    public int getExtractableAmount(FluidStack liquid) {
        return Math.min(getExtractionPower(liquid), getReceivableAmount(liquid));
    }

    public int getExtractionPower(FluidStack liquid) {
        if (!shouldReceive(liquid))
            return 0;

        var extractionPower = stackMode ? liquidSlot.getCapacity() : baseSpeed;
        if (byWorldInteraction)
            extractionPower = Math.max(extractionPower, wi());

        return extractionPower;
    }

    public int getReceivableAmount(FluidStack liquid) {
        return Math.min(getFreeSpace(liquid), liquid.getAmount());
    }

    public int getFreeSpace(FluidStack liquid) {
        if (!shouldReceive(liquid))
            return 0;

        var capacity = liquidSlot.getCapacity();
        if (byWorldInteraction)
            capacity = Math.max(capacity, wi());

        return liquidSlot.getFreeSpace(capacity);
    }

    private Optional<FluidStack> toExtract(IFluidHandler inv) {
        return Optional.ofNullable(
                liquidSlot.hasLiquid()
                ? canExtract(inv, liquidSlot.getFluid())
                  ? liquidSlot.getFluid()
                  : null
                : ForgeUtils.findFirstLiquid(inv, this::shouldReceive));
    }

    private boolean canExtract(IFluidHandler inv, FluidStack fluid) {
        return getExtractableAmount(fluid) > 0 && ForgeUtils.contains(inv, fluid);
    }

    private int extract(IFluidHandler inv, FluidStack fluid, int extractionPower) {
        return JavaUtils.decrementRecursion(
                ForgeUtils.containingSlots(inv, fluid),
                extractionPower,
                (slot, i) -> Math.min(getExtractableAmount(inv.getFluidInTank(slot)), i),
                (slot, i) -> liquidSlot.fill(inv.drain(ForgeUtils.copyWithAmount(inv.getFluidInTank(slot), i), EXECUTE), EXECUTE));
    }

    private Optional<FluidStack> toExtract(List<IFluidHandler> invs) {
        return OptionalStream.of(invs, this::toExtract).findFirst();
    }

    private int extract(List<IFluidHandler> invs, FluidStack fluid, int extractionPower) {
        return JavaUtils.recursion(
                invs,
                extractionPower,
                (inv, i) -> extract(inv, fluid, i));
    }

    /**
     * 液体搬入
     */
    public boolean canInsert(IFluidHandler inv) {
        return toInsert(inv).isPresent();
    }

    public boolean tryInsert(IFluidHandler inv) {
        return JavaUtils.isPresentAndThen(toInsert(inv), fluid -> insert(inv, fluid));
    }

    public boolean tryInsert(List<IFluidHandler> invs) {
        invs = JavaUtils.filter(invs, this::canInsert);
        if (!invs.isEmpty()) {

            JavaUtils.forEach(invs, liquidSlot::isEmpty, this::tryInsert);
            return true;
        }
        return false;
    }

    private void insert(IFluidHandler inv, FluidStack fluid) {
        if (!fluid.isFluidEqual(liquidSlot.getFluid()) || fluid.getAmount() > liquidSlot.getAmount())
            return;

        var remainder = inv.drain(fluid, SIMULATE);
        liquidSlot.extract(ForgeUtils.copyWithSub(fluid, remainder));
    }

    private Optional<FluidStack> toInsert(IFluidHandler inv) {
        var self = liquidSlot.getFluid();
        if (self.isEmpty())
            return Optional.empty();

        //ration
        var ration = liquidRation - ForgeUtils.countLiquid(inv, self);
        if (ration <= 0)
            return Optional.empty();

        //insertable amount
        var toInsert = ForgeUtils.copyWithAmount(self, Math.min(ration, self.getAmount()));
        var remainder = inv.fill(toInsert, SIMULATE);
        toInsert = ForgeUtils.copyWithSub(toInsert, remainder);
        if (toInsert.isEmpty())
            return Optional.empty();

        return Optional.of(toInsert);
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
