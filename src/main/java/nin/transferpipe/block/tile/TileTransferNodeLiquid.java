package nin.transferpipe.block.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.util.HandlerUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;

public class TileTransferNodeLiquid extends TileTransferNode {

    /**
     * 基本情報
     */

    public final FluidTank liquidSlot;
    public final ItemStackHandler liquidItemSlot;

    public static final String LIQUID_SLOT = "LiquidSlot";
    private final int baseSpeed = 200;
    public ContainerData liquidData = new ContainerData() {
        @Override
        public int get(int p_39284_) {
            return switch (p_39284_) {
                case 0 -> Block.getId(liquidSlot.getFluid().getFluid().defaultFluidState().createLegacyBlock());
                case 1 -> liquidSlot.getFluidAmount();
                default -> -1;
            };
        }

        @Override
        public void set(int p_39285_, int p_39286_) {
            switch (p_39285_) {
                case 0 -> liquidSlot.setFluid(new FluidStack(Block.stateById(p_39286_).getFluidState().getType(), 1));
                case 1 -> {
                    var f = liquidSlot.getFluid();
                    f.setAmount(p_39286_);
                    liquidSlot.setFluid(f);
                }
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public TileTransferNodeLiquid(BlockPos p_155229_, BlockState p_155230_) {
        super(TPBlocks.TRANSFER_NODE_LIQUID.entity(), p_155229_, p_155230_);
        liquidItemSlot = new ItemStackHandler();
        liquidSlot = new HandlerUtils.TileLiquid<>(8000, this);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return ForgeCapabilities.FLUID_HANDLER.orEmpty(cap, LazyOptional.of(() -> liquidSlot));
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
        var pullableAmount = stackMode ? fluid.getAmount() : 1;
        pullableAmount *= baseSpeed;
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
}
