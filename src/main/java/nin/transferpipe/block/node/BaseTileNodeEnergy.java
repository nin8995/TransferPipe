package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import nin.transferpipe.util.forge.TileEnergySlot;
import nin.transferpipe.util.java.JavaUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class BaseTileNodeEnergy extends BaseTileNode {

    /**
     * 初期化
     */
    public final TileEnergySlot<BaseTileNodeEnergy> energySlot;
    public final int baseCapacity = 10000;

    public BaseTileNodeEnergy(BlockEntityType<? extends BaseTileNode> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
        this.energySlot = new TileEnergySlot<>(baseCapacity, Integer.MAX_VALUE, Integer.MAX_VALUE, this);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return orSuper(ForgeCapabilities.ENERGY, energySlot, cap, side);
    }

    @Override
    public void calcUpgrades() {
        super.calcUpgrades();
        pseudoRoundRobin = true;
        breadthFirst = true;
        searchMemory = true;
        energySlot.setCapacity((int) (baseCapacity * capacityRate));
    }

    /**
     * エネルギーを流す
     */
    public void extractFrom(List<IEnergyStorage> machines) {
        interactWith(machines, e -> e.getEnergyStored() != 0, TileEnergySlot::getFreeSpace,
                (e, energy) -> e.extractEnergy(energy, false), energy -> energySlot.receiveEnergy(energy, false));
    }

    public void insertTo(List<IEnergyStorage> machines) {
        interactWith(machines, e -> e.getEnergyStored() != e.getMaxEnergyStored(), IEnergyStorage::getEnergyStored,
                (e, energy) -> e.receiveEnergy(energy, false), energy -> energySlot.extractEnergy(energy, false));
    }

    public void interactWith(List<IEnergyStorage> machines, Predicate<IEnergyStorage> filter, Function<TileEnergySlot<?>, Integer> energyGetter,
                             BiFunction<IEnergyStorage, Integer, Integer> targetFunc, Consumer<Integer> selfFunc) {
        machines = JavaUtils.filter(machines, filter);

        if (machines.size() != 0) {
            var energy = energyGetter.apply(energySlot) / machines.size();
            if (energy != 0)
                machines.forEach(e ->
                        selfFunc.accept(targetFunc.apply(e, energy)));
            if (energyGetter.apply(energySlot) / machines.size() == 0) {//1とか3とか端数が残るの嫌だから出し切る
                var i = new AtomicInteger(energyGetter.apply(energySlot));
                machines.stream()
                        .takeWhile(a -> i.get() > 0)
                        .forEach(e -> {
                            if (targetFunc.apply(e, 1) == 1) {
                                selfFunc.accept(1);
                                i.getAndDecrement();
                            }
                        });
            }
        }
    }

    /**
     * NBT
     */
    public static final String ENERGY = "Energy";

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(ENERGY, energySlot.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(ENERGY))
            energySlot.receive(tag.getInt(ENERGY));
    }
}
