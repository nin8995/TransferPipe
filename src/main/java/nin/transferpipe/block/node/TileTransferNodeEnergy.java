package nin.transferpipe.block.node;

import com.mojang.datafixers.util.Pair;
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
import nin.transferpipe.block.pipe.EnergyReceiverPipe;
import nin.transferpipe.util.HandlerUtils;
import nin.transferpipe.util.PipeUtils;
import nin.transferpipe.util.TPUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class TileTransferNodeEnergy extends TileBaseTransferNode {

    private final EnergyStorage energyStorage;
    private final Map<BlockPos, Map<Direction, Pair<LazyOptional<IEnergyStorage>, Boolean>>> extractablesLOs = new HashMap<>();
    private final Map<BlockPos, Map<Direction, Pair<LazyOptional<IEnergyStorage>, Boolean>>> receivableLOs = new HashMap<>();
    private final Map<BlockPos, Map<Direction, Pair<LazyOptional<IEnergyStorage>, Boolean>>> bothLOs = new HashMap<>();
    private final Map<BlockPos, Boolean> energyReceiverPipes = new HashMap<>();

    public static final String ENERGY = "Energy";
    public static final String CONNECTIONS = "Connections";
    public static final String SEARCHED = "Searched";
    public static final String ENERGY_RECEIVER_PIPES = "EnergyReceiverPipes";
    private final Map<BlockPos, Map<Direction, Boolean>> loadCache = new HashMap<>();
    public ContainerData energyData = new ContainerData() {
        @Override
        public int get(int p_39284_) {
            return switch (p_39284_) {
                case 0 -> energyStorage.getEnergyStored();
                case 1 -> extractablesLOs.size();
                case 2 -> receivableLOs.size();
                case 3 -> bothLOs.size();
                case 4 -> energyReceiverPipes.size();
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
            return 5;
        }
    };

    public TileTransferNodeEnergy(BlockPos p_155229_, BlockState p_155230_) {
        super(TPBlocks.TRANSFER_NODE_ENERGY.tile(), p_155229_, p_155230_);
        this.energyStorage = new HandlerUtils.TileEnergy<>(10000, Integer.MAX_VALUE, Integer.MAX_VALUE, this);
    }

    @Override
    public void calcUpgrades() {
        super.calcUpgrades();
        pseudoRoundRobin = true;
        breadthFirst = true;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return orSuper(ForgeCapabilities.ENERGY, energyStorage, cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(ENERGY, energyStorage.getEnergyStored());
        tag.put(CONNECTIONS, TPUtils.writePosDirsMapMap((t, pair) -> t.putBoolean(SEARCHED, pair.getSecond()), extractablesLOs, receivableLOs, bothLOs));
        tag.put(ENERGY_RECEIVER_PIPES, TPUtils.writePosMap(energyReceiverPipes, (t, b) -> t.putBoolean(SEARCHED, b)));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(ENERGY))
            energyStorage.receiveEnergy(tag.getInt(ENERGY), false);
        if (tag.contains(CONNECTIONS))
            TPUtils.readPosDirsMap(tag.getCompound(CONNECTIONS), t -> t.getBoolean(SEARCHED), loadCache::put);
        if (tag.contains(ENERGY_RECEIVER_PIPES))
            TPUtils.readPosMap(tag.getCompound(ENERGY_RECEIVER_PIPES), t -> t.getBoolean(SEARCHED), energyReceiverPipes::put);
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
        if (PipeUtils.isWorkPlace(level, pos, dir))
            tryEstablishConnection(pos, dir, true);
    }

    @Override
    public void work(BlockPos pos, Direction dir) {
        tryEstablishConnection(pos, dir, true);
    }

    @Override
    public boolean canWork(BlockPos pos, Direction d) {
        return HandlerUtils.hasEnergyStorage(level, pos, d);
    }

    @Override
    public boolean isMultiTask() {
        return true;
    }

    @Override
    public Vector3f getColor() {
        return new Vector3f(1, 1, 0);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!loadCache.isEmpty()) {
            loadCache.forEach((pos, dirsSearched) -> dirsSearched.forEach((dir, searched) -> tryEstablishConnection(pos, dir, searched)));
            loadCache.clear();
        }
    }

    @Override
    public void afterTick() {
        refreshConnections();

        var extractables = getEnergyStorages(extractablesLOs);
        var receivables = getEnergyStorages(receivableLOs);
        var both = getEnergyStorages(bothLOs);

        extractFrom(extractables);
        extractFrom(both);
        insertTo(receivables);
        insertTo(both);
    }

    public void tryEstablishConnection(BlockPos pos, Direction dir, boolean wasSearched) {
        var loEnergy = HandlerUtils.getEnergyStorageOptional(level, pos, dir);
        if (loEnergy != null)
            loEnergy.ifPresent(energy -> {
                if (energy.canExtract() && energy.canReceive())
                    TPUtils.addToMapMap(bothLOs, pos, dir, Pair.of(loEnergy, wasSearched));
                else if (energy.canExtract())
                    TPUtils.addToMapMap(extractablesLOs, pos, dir, Pair.of(loEnergy, wasSearched));
                else if (energy.canReceive())
                    TPUtils.addToMapMap(receivableLOs, pos, dir, Pair.of(loEnergy, wasSearched));
            });
    }

    public void refreshConnections() {
        refreshConnection(extractablesLOs, IEnergyStorage::canExtract);
        refreshConnection(receivableLOs, IEnergyStorage::canReceive);
        refreshConnection(bothLOs, e -> e.canReceive() && e.canExtract());
    }

    public void refreshConnection(Map<BlockPos, Map<Direction, Pair<LazyOptional<IEnergyStorage>, Boolean>>> map, Predicate<IEnergyStorage> shouldSustain) {
        TPUtils.removeFromMapMap(map, (pos, dir, pair) -> {
            var loEnergy = pair.getFirst();//!isPresentとisEmptyはLazyOptionalにおいては違う
            if (loEnergy.filter(shouldSustain::test).isEmpty() || !loEnergy.isPresent())//保持するべきではない or そもそもなくなってる
                return true;
            else //なんかRFToolsのマルチブロック蓄電器はこれだけじゃ消去されなかったから、実際にやってエラー吐かれることを以て無効化とみなす
                try {
                    loEnergy.resolve().get().getEnergyStored();
                } catch (Exception e) {
                    return true;
                }
            return false;
        });
    }

    public List<IEnergyStorage> getEnergyStorages(Map<BlockPos, Map<Direction, Pair<LazyOptional<IEnergyStorage>, Boolean>>> map) {
        return map.values().stream().flatMap(valMap -> valMap.values().stream().map(pair -> pair.getFirst().resolve().get())).toList();
    }

    public void extractFrom(List<IEnergyStorage> extractables) {
        extractables = extractables.stream().filter(e -> e.getEnergyStored() != 0).toList();

        if (extractables.size() != 0) {
            var energyPerExtractables = (energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored()) / extractables.size();
            //sizeで割られるのがsizeより小さいと0になって、微妙に満杯までいかなかったり微妙に０までいかなかったりする
            if (energyPerExtractables != 0)
                extractables.forEach(e -> energyStorage.receiveEnergy(e.extractEnergy(energyPerExtractables, false), false));
            else {//そういうやつらのために端数を取る特別処理
                var i = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
                for (IEnergyStorage e : extractables) {
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

    @Override
    public void onSearchEnd() {
        super.onSearchEnd();
        disconnectUnlessWasSearched(extractablesLOs);
        disconnectUnlessWasSearched(receivableLOs);
        disconnectUnlessWasSearched(bothLOs);
        TPUtils.removeFromMap(energyReceiverPipes, (pos, b) -> !b, pos -> {
            if (TPUtils.getTile(level, pos) instanceof EnergyReceiverPipe.Tile tile)
                tile.nodeReference = null;
        });

        resetSearchedFlags(extractablesLOs);
        resetSearchedFlags(receivableLOs);
        resetSearchedFlags(bothLOs);
        energyReceiverPipes.forEach((pos, b) -> energyReceiverPipes.put(pos, false));
        setChanged();
    }

    public void disconnectUnlessWasSearched(Map<BlockPos, Map<Direction, Pair<LazyOptional<IEnergyStorage>, Boolean>>> map) {
        TPUtils.removeFromMapMap(map, (pos, dir, pair) -> !pair.getSecond());
    }

    public void resetSearchedFlags(Map<BlockPos, Map<Direction, Pair<LazyOptional<IEnergyStorage>, Boolean>>> map) {
        map.forEach((pos, dirsMap) -> dirsMap.forEach((dir, pair) -> dirsMap.put(dir, pair.mapSecond(b -> false))));
    }

    @Override
    public void onSearchProceed(BlockPos pos) {
        super.onSearchProceed(pos);
        if (TPUtils.getTile(level, pos) instanceof EnergyReceiverPipe.Tile receiver) {
            receiver.connect(this);
            energyReceiverPipes.put(pos, true);
            setChanged();
        }
    }

    public void removeEnergyReceiverPipe(BlockPos pos) {
        energyReceiverPipes.remove(pos);
        setChanged();
    }
}
