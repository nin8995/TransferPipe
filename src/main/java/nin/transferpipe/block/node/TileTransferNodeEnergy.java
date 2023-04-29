package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.pipe.EnergyReceiverPipe;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.forge.LazyOptionalMap;
import nin.transferpipe.util.forge.TileEnergySlot;
import nin.transferpipe.util.minecraft.TileMap;
import nin.transferpipe.util.transferpipe.PipeUtils;
import nin.transferpipe.util.transferpipe.TPUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class TileTransferNodeEnergy extends TileBaseTransferNode {

    /**
     * 初期化
     */
    public final TileEnergySlot<TileTransferNodeEnergy> energySlot;

    public TileTransferNodeEnergy(BlockPos p_155229_, BlockState p_155230_) {
        super(TPBlocks.TRANSFER_NODE_ENERGY.tile(), p_155229_, p_155230_);
        this.energySlot = new TileEnergySlot<>(10000, Integer.MAX_VALUE, Integer.MAX_VALUE, this);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return orSuper(ForgeCapabilities.ENERGY, energySlot, cap, side);
    }

    @Override
    public boolean shouldSearch() {
        return true;
    }

    @Override
    public Vector3f getColor() {
        return new Vector3f(1, 1, 0);
    }

    @Override
    public void calcUpgrades() {
        super.calcUpgrades();
        pseudoRoundRobin = true;
        breadthFirst = true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        tryLoadCaches();
    }

    @Override
    public void beforeTick() {
        super.beforeTick();
        if (level.getGameTime() % 20 == 0)
            tryLoadCaches();
    }

    public void tryLoadCaches() {
        extractablesLOs.tryLoadCache(level);
        receivableLOs.tryLoadCache(level);
        bothLOs.tryLoadCache(level);
        energyReceiverPipes.tryLoadCache(level);

        setChanged();
    }

    @Override
    public boolean isMultiTask() {
        return true;
    }

    /**
     * 接続管理
     */
    public final LazyOptionalMap<IEnergyStorage> extractablesLOs = new LazyOptionalMap<>(ForgeUtils::getEnergyStorage, (pos, dir, lo) -> setChanged());
    public final LazyOptionalMap<IEnergyStorage> receivableLOs = new LazyOptionalMap<>(ForgeUtils::getEnergyStorage, (pos, dir, lo) -> setChanged());
    public final LazyOptionalMap<IEnergyStorage> bothLOs = new LazyOptionalMap<>(ForgeUtils::getEnergyStorage, (pos, dir, lo) -> setChanged());

    @Override
    public void facing(BlockPos pos, Direction dir) {
        if (PipeUtils.isWorkPlace(level, pos, dir))
            tryEstablishConnection(pos, dir);
    }

    @Override
    public boolean canWork(BlockPos pos, Direction d) {
        return ForgeUtils.hasEnergyStorage(level, pos, d);
    }

    @Override
    public void work(BlockPos pos, Direction dir) {
        tryEstablishConnection(pos, dir);
    }

    public void tryEstablishConnection(BlockPos pos, Direction dir) {
        var loEnergy = ForgeUtils.getEnergyStorage(level, pos, dir);
        loEnergy.ifPresent(energy -> {
            var unchanged = false;
            if (energy.canExtract() && energy.canReceive())
                bothLOs.addMarked(pos, dir, loEnergy);
            else if (energy.canExtract())
                extractablesLOs.addMarked(pos, dir, loEnergy);
            else if (energy.canReceive())
                receivableLOs.addMarked(pos, dir, loEnergy);
            else
                unchanged = true;
            if (!unchanged)
                setChanged();
        });
    }

    public final TileMap<EnergyReceiverPipe.Tile> energyReceiverPipes = new TileMap<>(EnergyReceiverPipe.Tile.class,
            (pos, receiver) -> receiver.nodeReference = null);

    @Override
    public void onSearchProceed(BlockPos pos) {
        super.onSearchProceed(pos);
        if (TPUtils.getTile(level, pos) instanceof EnergyReceiverPipe.Tile receiver) {
            receiver.connect(this);
            energyReceiverPipes.putMarked(pos, receiver);
            setChanged();
        }
    }

    @Override
    public void onRemove() {
        super.onRemove();
        energyReceiverPipes.invalidate();
    }

    @Override
    public void onSearchEnd() {
        super.onSearchEnd();
        extractablesLOs.reset();
        receivableLOs.reset();
        bothLOs.reset();

        energyReceiverPipes.reset();
        energyReceiverPipes.removeIf((pos, v) -> v.isRemoved());
    }

    /**
     * エネルギー管理
     */
    @Override
    public void afterTick() {
        refreshConnection(extractablesLOs, IEnergyStorage::canExtract);
        refreshConnection(receivableLOs, IEnergyStorage::canReceive);
        refreshConnection(bothLOs, ForgeUtils::canBoth);

        var extractables = extractablesLOs.getValues();
        var receivables = receivableLOs.getValues();
        var both = bothLOs.getValues();

        var prevEnergy = energySlot.getEnergyStored();
        extractFrom(extractables);
        extractFrom(both);
        if (prevEnergy != energySlot.getEnergyStored())
            setChanged();

        prevEnergy = energySlot.getEnergyStored();
        insertTo(receivables);
        insertTo(both);
        if (prevEnergy != energySlot.getEnergyStored())
            setChanged();
    }

    public void refreshConnection(LazyOptionalMap<IEnergyStorage> loEnergyMap, Predicate<IEnergyStorage> shouldSustain) {
        loEnergyMap.removeValueIf((pos, dir, loEnergy) -> {//!isPresentとisEmptyはLazyOptionalにおいては違う
            if (loEnergy.filter(shouldSustain::test).isEmpty() || !loEnergy.isPresent()) //保持するべきではない or そもそもなくなってる
                return true;
            else //なんかRFToolsのマルチブロック蓄電器はこれだけじゃ消去されなかったから、実際にやってエラー吐かれることを以て無効化とみなす
                try {
                    loEnergy.resolve().get().getEnergyStored();
                    return false;
                } catch (Exception e) {
                    return true;
                }
        });
    }

    public void extractFrom(List<IEnergyStorage> machines) {
        machines = machines.stream().filter(e -> e.getEnergyStored() != 0).toList();

        if (machines.size() != 0) {
            var energy = energySlot.getFreeSpace() / machines.size();
            if (energy != 0)
                machines.forEach(e -> energySlot.receiveEnergy(e.extractEnergy(energy, false), false));
            if (energySlot.getFreeSpace() / machines.size() == 0) {
                var i = new AtomicInteger(energySlot.getFreeSpace());
                machines.stream()
                        .takeWhile(a -> i.get() > 0)
                        .forEach(e -> {
                            if (e.extractEnergy(1, false) == 1) {
                                energySlot.receiveEnergy(1, false);
                                i.getAndDecrement();
                            }
                        });
            }
        }
    }

    public void insertTo(List<IEnergyStorage> machines) {
        machines = machines.stream().filter(e -> e.getEnergyStored() != e.getMaxEnergyStored()).toList();

        if (machines.size() != 0) {
            var energy = energySlot.getEnergyStored() / machines.size();
            if (energy != 0)
                machines.forEach(e -> energySlot.extractEnergy(e.receiveEnergy(energy, false), false));
            if (energySlot.getEnergyStored() / machines.size() == 0) {//1とか3とか端数が残るの嫌だから出し切る
                var i = new AtomicInteger(energySlot.getEnergyStored());
                machines.stream()
                        .takeWhile(a -> i.get() > 0)
                        .forEach(e -> {
                            if (e.receiveEnergy(1, false) == 1) {
                                energySlot.extractEnergy(1, false);
                                i.getAndDecrement();
                            }
                        });
            }
        }
    }

    /**
     * ゴミ処理場
     */
    public static final String ENERGY = "Energy";
    public static final String EXTRACTABLES = "Extractables";
    public static final String RECEIVABLES = "Receivables";
    public static final String BOTH = "Both";
    public static final String ENERGY_RECEIVER_PIPES = "EnergyReceiverPipes";

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(ENERGY, energySlot.getEnergyStored());
        tag.put(EXTRACTABLES, extractablesLOs.serializeNBT());
        tag.put(RECEIVABLES, receivableLOs.serializeNBT());
        tag.put(BOTH, bothLOs.serializeNBT());
        tag.put(ENERGY_RECEIVER_PIPES, energyReceiverPipes.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(ENERGY))
            energySlot.receive(tag.getInt(ENERGY));
        if (tag.contains(EXTRACTABLES))
            extractablesLOs.deserializeNBT(tag.getCompound(EXTRACTABLES));
        if (tag.contains(RECEIVABLES))
            receivableLOs.deserializeNBT(tag.getCompound(RECEIVABLES));
        if (tag.contains(BOTH))
            bothLOs.deserializeNBT(tag.getCompound(BOTH));
        if (tag.contains(ENERGY_RECEIVER_PIPES))
            energyReceiverPipes.deserializeNBT(tag.getCompound(ENERGY_RECEIVER_PIPES));
    }

    public ContainerData energyData = new ContainerData() {
        @Override
        public int get(int p_39284_) {
            return switch (p_39284_) {
                case 0 -> energySlot.getEnergyStored();
                case 1 -> extractablesLOs.valueCount();
                case 2 -> receivableLOs.valueCount();
                case 3 -> bothLOs.valueCount();
                case 4 -> energyReceiverPipes.size();
                default -> -1;
            };
        }

        @Override
        public void set(int p_39285_, int p_39286_) {
        }

        @Override
        public int getCount() {
            return 5;
        }
    };
}
