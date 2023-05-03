package nin.transferpipe.util.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public interface ForgeUtils {

    /**
     * ItemHandler
     */
    static void forItemHandler(Level level, BlockPos pos, Direction dir, NonNullConsumer<? super IItemHandler> func) {
        getItemHandler(level, pos, dir).ifPresent(func);
    }

    static boolean hasItemHandler(Level level, BlockPos pos, Direction dir) {
        return getItemHandler(level, pos, dir).isPresent();
    }

    Map<Container, LazyOptional<IItemHandler>> containerCache = new HashMap<>();

    static LazyOptional<IItemHandler> getItemHandler(Level level, BlockPos pos, Direction dir) {
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
    static Container getContainer(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Container c ? c
                                                                : level.getBlockState(pos).getBlock() instanceof WorldlyContainerHolder holder ? holder.getContainer(level.getBlockState(pos), level, pos)
                                                                                                                                               : null;
    }

    static void forFirstItemSlot(Level level, BlockPos pos, Direction dir, BiConsumer<IItemHandler, Integer> func) {
        forItemHandler(level, pos, dir, inv -> IntStream
                .range(0, inv.getSlots())
                .filter(i -> !inv.extractItem(i, 1, true).isEmpty())
                .findFirst().ifPresent(i -> func.accept(inv, i))
        );
    }

    static List<Item> toItemList(IItemHandler inv) {
        return IntStream.range(0, inv.getSlots())
                .mapToObj(inv::getStackInSlot)
                .map(ItemStack::getItem).toList();
    }

    static int countItem(IItemHandler inv, ItemStack item) {
        return IntStream.range(0, inv.getSlots())
                .filter(slot -> ItemHandlerHelper.canItemStacksStack(inv.getStackInSlot(slot), item))
                .map(slot -> inv.getStackInSlot(slot).getCount())
                .reduce(Integer::sum).orElse(0);
    }

    static List<ItemStack> filter(IItemHandler inv, Predicate<ItemStack> filter) {
        return IntStream.range(0, inv.getSlots())
                .filter(i -> filter.test(inv.getStackInSlot(i)))
                .mapToObj(inv::getStackInSlot).toList();
    }

    @Nullable
    static ItemStack findFirst(IItemHandler inv, Predicate<ItemStack> filter) {
        var oi = IntStream.range(0, inv.getSlots())
                .filter(i -> !inv.extractItem(i, 1, true).isEmpty() && filter.test(inv.getStackInSlot(i)))
                .findFirst();
        return oi.isPresent() ? inv.getStackInSlot(oi.getAsInt()) : null;
    }

    @Nullable
    static ItemStack findLast(IItemHandler inv, Predicate<ItemStack> filter) {
        var oi = IntStream.range(0, inv.getSlots())
                .map(i -> inv.getSlots() - 1 - i)
                .filter(i -> !inv.extractItem(i, 1, true).isEmpty() && filter.test(inv.getStackInSlot(i)))
                .findFirst();
        return oi.isPresent() ? inv.getStackInSlot(oi.getAsInt()) : null;
    }

    /**
     * FluidHandler
     */
    static void forFluidHandler(Level level, BlockPos pos, Direction dir, NonNullConsumer<? super IFluidHandler> func) {
        getFluidHandler(level, pos, dir).ifPresent(func);
    }

    static boolean hasFluidHandler(Level level, BlockPos pos, Direction dir) {
        return getFluidHandler(level, pos, dir).isPresent();
    }

    static LazyOptional<IFluidHandler> getFluidHandler(Level level, BlockPos pos, Direction dir) {
        var be = level.getBlockEntity(pos);
        return be != null ? be.getCapability(ForgeCapabilities.FLUID_HANDLER, dir) : LazyOptional.empty();
    }

    static int countFluid(IFluidHandler tanks, FluidStack fluid) {
        return IntStream.range(0, tanks.getTanks())
                .filter(tank -> tanks.getFluidInTank(tank).isFluidEqual(fluid))
                .map(tank -> tanks.getFluidInTank(tank).getAmount())
                .reduce(Integer::sum).orElse(0);
    }

    /**
     * EnergyStorage
     */
    static void forEnergyStorage(Level level, BlockPos pos, Direction dir, NonNullConsumer<? super IEnergyStorage> func) {
        getEnergyStorage(level, pos, dir).ifPresent(func);
    }

    static boolean hasEnergyStorage(Level level, BlockPos pos, Direction dir) {
        return getEnergyStorage(level, pos, dir).isPresent();
    }

    static LazyOptional<IEnergyStorage> getEnergyStorage(Level level, BlockPos pos, Direction dir) {
        var be = level.getBlockEntity(pos);
        return be != null ? be.getCapability(ForgeCapabilities.ENERGY, dir) : LazyOptional.empty();
    }

    static boolean canBoth(IEnergyStorage energy) {
        return energy.canExtract() && energy.canReceive();
    }

    /**
     * FluidStack
     */
    static FluidStack copyWithAddition(FluidStack fluid, int addition) {
        return copyWithAmount(fluid, fluid.getAmount() + addition);
    }

    static FluidStack copyWithAmount(FluidStack fluid, int amount) {
        var copy = fluid.copy();
        copy.setAmount(amount);
        return copy;
    }

    /**
     * Format
     */
    static String toMilliBucket(int amount) {
        return String.format("%,d", amount) + "mb";
    }

    static String toFE(int energy) {
        return String.format("%,d", energy) + "FE";
    }
}
