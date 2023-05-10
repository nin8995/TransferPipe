package nin.transferpipe.util.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import nin.transferpipe.util.java.ExceptionPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

    static IItemHandler getItemHandler(CapabilityProvider<?> cap) {
        return cap instanceof ItemEntity dropItem ? new DropItemHandler(dropItem) : cap.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().get();
    }

    static boolean hasItemHandler(CapabilityProvider<?> cap) {
        return cap.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
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
        return container != null
               ? container instanceof WorldlyContainer wc
                 ? containerCache.computeIfAbsent(container, it -> SidedInvWrapper.create(wc, dir)[0].cast())
                 : containerCache.computeIfAbsent(container, it -> LazyOptional.of(() -> new InvWrapper(container)))
               : LazyOptional.empty();
    }

    @Nullable
    static Container getContainer(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Container c
               ? c
               : level.getBlockState(pos).getBlock() instanceof WorldlyContainerHolder holder
                 ? holder.getContainer(level.getBlockState(pos), level, pos)
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
        return stream(inv)
                .map(ItemStack::getItem)
                .toList();
    }

    static Stream<ItemStack> stream(IItemHandler inv) {
        return IntStream.range(0, inv.getSlots())
                .mapToObj(inv::getStackInSlot);
    }

    static int countItem(IItemHandler inv, ItemStack toCount) {
        return stream(inv)
                .filter(item -> ItemHandlerHelper.canItemStacksStack(item, toCount))
                .map(ItemStack::getCount)
                .reduce(Integer::sum).orElse(0);
    }

    static List<ItemStack> filter(IItemHandler inv, Predicate<ItemStack> filter) {
        return stream(inv)
                .filter(filter)
                .toList();
    }

    static List<IItemHandler> filter(List<IItemHandler> invs, Predicate<ItemStack> filter) {
        return invs.stream()
                .filter(inv -> !filter(inv, filter).isEmpty())
                .toList();
    }

    @Nullable
    static ItemStack findFirst(IItemHandler inv, Predicate<ItemStack> filter) {
        return stream(inv)
                .filter(item -> !item.isEmpty() && filter.test(item))
                .findFirst().orElse(null);
    }

    @Nullable
    static ItemStack findFirst(List<IItemHandler> invs, Predicate<ItemStack> filter) {
        return invs.stream()
                .filter(inv -> findFirst(inv, filter) != null)
                .findFirst()
                .map(inv -> findFirst(inv, filter))
                .orElse(null);
    }

    static boolean isEmpty(IItemHandler inv) {
        return stream(inv)
                .allMatch(ItemStack::isEmpty);
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

    static IFluidHandler getFluidHandler(CapabilityProvider<?> cap) {
        return cap instanceof ItemStack
               ? cap.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().get()
               : cap.getCapability(ForgeCapabilities.FLUID_HANDLER).resolve().get();
    }

    static boolean hasFluidHandler(CapabilityProvider<?> cap) {
        return ExceptionPredicate.succeeded(() -> getFluidHandler(cap));
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

    static IEnergyStorage getEnergyHandler(CapabilityProvider<?> cap) {
        return cap.getCapability(ForgeCapabilities.ENERGY).resolve().get();
    }

    static boolean hasEnergyStorage(CapabilityProvider<?> cap) {
        return cap.getCapability(ForgeCapabilities.ENERGY).isPresent();
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

    static ItemStack getFluidItem(FluidStack fluid) {
        if (fluid.isEmpty())
            return ItemStack.EMPTY;

        var fluidItem = new FluidHandlerItemStack(Items.ENDER_DRAGON_SPAWN_EGG.getDefaultInstance(), Integer.MAX_VALUE);
        fluidItem.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
        return fluidItem.getContainer();
    }

    static ItemStack getFluidItem(ItemStack item) {
        return getFluidItem(getFluid(item));
    }

    static FluidStack getFluidFromCapability(ItemStack item) {
        return getFluidHandler(item).getFluidInTank(0);
    }

    static FluidStack getNBTFluid(ItemStack fluidItem) {
        return new FluidHandlerItemStack(fluidItem, Integer.MAX_VALUE).getFluid();
    }

    static boolean hasFluid(ItemStack item) {
        return !getFluid(item).isEmpty();
    }

    static FluidStack getFluid(ItemStack item) {
        return hasFluidHandler(item) ? getFluidFromCapability(item) : getNBTFluid(item);
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
