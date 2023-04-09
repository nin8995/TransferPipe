package nin.transferpipe.block.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.particle.ColorSquare;
import nin.transferpipe.util.HandlerUtils;
import nin.transferpipe.util.TPUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class TileTransferNodeEnergy extends TileTransferNode {

    private final EnergyStorage energyStorage;
    private final Map<BlockPos, Map<Direction, LazyOptional<IEnergyStorage>>> extractablesLOs = new HashMap<>();
    private final Map<BlockPos, Map<Direction, LazyOptional<IEnergyStorage>>> receivableLOs = new HashMap<>();
    private final Map<BlockPos, Map<Direction, LazyOptional<IEnergyStorage>>> bothLOs = new HashMap<>();

    public static final String ENERGY_STORAGE = "EnergyStorage";
    public static final String CONNECTIONS = "Connections";
    private final Map<BlockPos, Set<Direction>> loadCache = new HashMap<>();
    public ContainerData energyData = new ContainerData() {
        @Override
        public int get(int p_39284_) {
            return switch (p_39284_) {
                case 0 -> energyStorage.getEnergyStored();
                case 1 -> extractablesLOs.size();
                case 2 -> receivableLOs.size();
                case 3 -> bothLOs.size();
                default -> -1;
            };
        }

        @Override
        public void set(int p_39285_, int p_39286_) {
            if (p_39285_ == 0)
                energyStorage.receiveEnergy(p_39286_, false);
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public TileTransferNodeEnergy(BlockPos p_155229_, BlockState p_155230_) {
        super(TPBlocks.TRANSFER_NODE_ENERGY.entity(), p_155229_, p_155230_);
        this.energyStorage = new EnergyStorage(10000, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public void calcUpgrades() {
        super.calcUpgrades();
        pseudoRoundRobin = true;
        breadthFirst = true;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return ForgeCapabilities.ENERGY.orEmpty(cap, LazyOptional.of(() -> energyStorage));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(ENERGY_STORAGE, energyStorage.getEnergyStored());
        tag.put(CONNECTIONS, TPUtils.writePosDirsMapMap(extractablesLOs, receivableLOs, bothLOs));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(ENERGY_STORAGE))
            energyStorage.receiveEnergy(tag.getInt(ENERGY_STORAGE), false);
        if (tag.contains(CONNECTIONS))
            TPUtils.readPosDirs(tag.getCompound(CONNECTIONS), loadCache::put);
    }

    /**
     * 機能
     */

    @Override
    public boolean shouldSearch() {
        return true;
    }

    @Override
    public void facing(BlockPos pos, Direction dir) {
        tryEstablishConnection(pos, dir);
    }

    @Override
    public void terminal(BlockPos pos, Direction dir) {
        tryEstablishConnection(pos, dir);
    }

    public void tryEstablishConnections(BlockPos pos, Set<Direction> dirs) {
        dirs.forEach(dir -> tryEstablishConnection(pos, dir));
    }

    public void tryEstablishConnection(BlockPos pos, Direction dir) {
        var loEnergy = HandlerUtils.getEnergyStorageOptional(level, pos, dir);
        if (loEnergy != null)
            loEnergy.ifPresent(energy -> {
                if (energy.canExtract() && energy.canReceive())
                    TPUtils.addToMapMap(bothLOs, pos, dir, loEnergy);
                else if (energy.canExtract())
                    TPUtils.addToMapMap(extractablesLOs, pos, dir, loEnergy);
                else if (energy.canReceive())
                    TPUtils.addToMapMap(receivableLOs, pos, dir, loEnergy);
            });
    }

    @Override
    public boolean canWork(BlockPos pos, Direction d) {
        return HandlerUtils.hasEnergyStorage(level, pos, d);
    }

    @Override
    public boolean canWorkMultipleAtTime() {
        return true;
    }

    @Override
    public ColorSquare.Option getParticleOption() {
        return new ColorSquare.Option(1, 1, 0, 1);
    }

    @Override
    public void tick() {
        //load時にはlevelがnullなため、ここで実際にloをload
        if (!loadCache.isEmpty()) {
            loadCache.forEach(this::tryEstablishConnections);
            loadCache.clear();
        }

        super.tick();
        refreshConnections();

        var extractables = getEnergyStorages(extractablesLOs);
        var receivables = getEnergyStorages(receivableLOs);
        var both = getEnergyStorages(bothLOs);

        extractFrom(extractables);
        extractFrom(both);
        insertTo(receivables);
        insertTo(both);
    }

    public void refreshConnections() {
        refreshConnection(extractablesLOs, IEnergyStorage::canExtract);
        refreshConnection(receivableLOs, IEnergyStorage::canReceive);
        refreshConnection(bothLOs, e -> e.canReceive() && e.canExtract());
    }

    public void refreshConnection(Map<BlockPos, Map<Direction, LazyOptional<IEnergyStorage>>> map, Predicate<IEnergyStorage> shouldSustain) {
        var toRemove = new HashMap<BlockPos, Set<Direction>>();
        map.forEach((pos, value) -> value.forEach((dir, loEnergy) -> {//!isPresentとisEmptyはLazyOptionalにおいては違う
            if (loEnergy.filter(shouldSustain::test).isEmpty() || !loEnergy.isPresent())//保持するべきではない or そもそもなくなってる
                TPUtils.addToSetMap(toRemove, pos, dir);
            else //なんかRFToolsのマルチブロック蓄電器はこれだけじゃ消去されなかったから、実際にやってエラー吐かれることを以て無効化とみなす
                try {
                    loEnergy.resolve().get().getEnergyStored();
                } catch (Exception e) {
                    TPUtils.addToSetMap(toRemove, pos, dir);
                }
        }));
        toRemove.forEach((pos, value) -> value.forEach(dir -> TPUtils.removeFromMapMap(map, pos, dir)));
    }

    public List<IEnergyStorage> getEnergyStorages(Map<BlockPos, Map<Direction, LazyOptional<IEnergyStorage>>> map) {
        return map.values().stream().flatMap(valMap -> valMap.values().stream().map(lo -> lo.resolve().get())).toList();
    }

    public void extractFrom(List<IEnergyStorage> extractables) {
        extractables = extractables.stream().filter(e -> e.getEnergyStored() != 0).toList();

        if (extractables.size() != 0) {
            var energyPerExtractables = (energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored()) / extractables.size();
            if (energyPerExtractables != 0)
                extractables.forEach(e -> energyStorage.receiveEnergy(e.extractEnergy(energyPerExtractables, false), false));
            else { //sizeで割られるのがsizeより小さいと0になって、微妙に満杯までいかなかったり微妙に０までいかなかったりする
                var i = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
                for (IEnergyStorage e : extractables) {//のでその微妙な値を分配する特殊処理が必要
                    if (i == 0)
                        return;

                    if (e.extractEnergy(1, false) == 1) {
                        energyStorage.receiveEnergy(1, false);
                        i--;
                    }
                }
            }
        }
    }

    public void insertTo(List<IEnergyStorage> receivables) {
        receivables = receivables.stream().filter(e -> e.getEnergyStored() != e.getMaxEnergyStored()).toList();

        if (receivables.size() != 0) {
            var energyPerReceivables = energyStorage.getEnergyStored() / receivables.size();
            if (energyPerReceivables != 0)
                receivables.forEach(e -> energyStorage.extractEnergy(e.receiveEnergy(energyPerReceivables, false), false));
            else {
                var i = energyStorage.getEnergyStored();
                for (IEnergyStorage e : receivables) {
                    if (i == 0)
                        return;

                    if (e.receiveEnergy(1, false) == 1) {
                        energyStorage.extractEnergy(1, false);
                        i--;
                    }
                }
            }
        }
    }
}
