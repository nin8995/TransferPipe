package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.pipe.EnergyReceiverPipe;
import nin.transferpipe.gui.BaseBlockMenu;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.forge.LazyOptionalMap;
import nin.transferpipe.util.minecraft.TileMap;
import nin.transferpipe.util.transferpipe.TPUtils;
import org.joml.Vector3f;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class TransferNodeEnergy extends BaseBlockNode.Energy<TransferNodeEnergy.Tile> {

    @Override
    public TPBlocks.RegistryGUIEntityBlock<TransferNodeEnergy.Tile> registryWithGUI() {
        return TPBlocks.TRANSFER_NODE_ENERGY;
    }

    @Override
    public BaseBlockMenu menu(TransferNodeEnergy.Tile tile, int id, Inventory inv) {
        return new Menu(tile.energyData, tile.upgrades, tile.searchData, id, inv);
    }

    public static class Menu extends BaseMenuNode.Energy {

        public Menu(int containerId, Inventory inv, FriendlyByteBuf buf) {
            this(new SimpleContainerData(5), new ItemStackHandler(6), new SimpleContainerData(4), containerId, inv);
        }

        public Menu(ContainerData energyNodeData, IItemHandler upgrades, ContainerData data, int containerId, Inventory inv) {
            super(TPBlocks.TRANSFER_NODE_ENERGY, energyNodeData, upgrades, data, containerId, inv);
        }
    }

    public static class Screen extends BaseScreenNode.Energy<Menu> {

        public Screen(Menu p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }
    }

    public static class Tile extends BaseTileNodeEnergy {

        public Tile(BlockPos p_155229_, BlockState p_155230_) {
            super(TPBlocks.TRANSFER_NODE_ENERGY.tile(), p_155229_, p_155230_);
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
        public boolean isMultiTask() {
            return true;
        }

        /**
         * 接続管理
         */
        public final Supplier<LazyOptionalMap<IEnergyStorage>> loMap = () -> new LazyOptionalMap<>(ForgeUtils::getEnergyStorage, (pos, dir, lo) -> setChanged());
        public final LazyOptionalMap<IEnergyStorage> extractLOs = loMap.get();
        public final LazyOptionalMap<IEnergyStorage> receiveLOs = loMap.get();
        public final LazyOptionalMap<IEnergyStorage> bothLOs = loMap.get();

        @Override
        public void facing(BlockPos pos, Direction dir) {
            if (canWork(pos, dir))
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
                    extractLOs.addMarked(pos, dir, loEnergy);
                else if (energy.canReceive())
                    receiveLOs.addMarked(pos, dir, loEnergy);
                else
                    unchanged = true;

                if (!unchanged)
                    setChanged();
            });
        }

        public final TileMap<EnergyReceiverPipe.Tile> energyReceiverPipes = new TileMap<>(EnergyReceiverPipe.Tile.class,
                (pos, receiver) -> {
                    receiver.reset();
                    setChanged();
                });

        @Override
        public void onSearchProceed(BlockPos pos) {
            super.onSearchProceed(pos);
            if (TPUtils.getTile(level, pos) instanceof EnergyReceiverPipe.Tile receiver) {
                if (receiver.node != this)
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
            extractLOs.reset();
            receiveLOs.reset();
            bothLOs.reset();
            energyReceiverPipes.reset();
        }

        /**
         * エネルギー管理
         */
        @Override
        public void afterTick() {
            refreshConnection(extractLOs, IEnergyStorage::canExtract);
            refreshConnection(receiveLOs, IEnergyStorage::canReceive);
            refreshConnection(bothLOs, ForgeUtils::canBoth);

            var extract = extractLOs.forceGetValues();
            var receive = receiveLOs.forceGetValues();
            var both = bothLOs.forceGetValues();

            extractFrom(extract);
            extractFrom(both);
            insertTo(receive);
            insertTo(both);
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

        /**
         * 読む必要ない
         */
        public static final String EXTRACTABLES = "Extractables";
        public static final String RECEIVABLES = "Receivables";
        public static final String BOTH = "Both";
        public static final String ENERGY_RECEIVER_PIPES = "EnergyReceiverPipes";

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.put(EXTRACTABLES, extractLOs.serializeNBT());
            tag.put(RECEIVABLES, receiveLOs.serializeNBT());
            tag.put(BOTH, bothLOs.serializeNBT());
            tag.put(ENERGY_RECEIVER_PIPES, energyReceiverPipes.serializeNBT());
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            if (tag.contains(EXTRACTABLES))
                extractLOs.deserializeNBT(tag.getCompound(EXTRACTABLES));
            if (tag.contains(RECEIVABLES))
                receiveLOs.deserializeNBT(tag.getCompound(RECEIVABLES));
            if (tag.contains(BOTH))
                bothLOs.deserializeNBT(tag.getCompound(BOTH));
            if (tag.contains(ENERGY_RECEIVER_PIPES))
                energyReceiverPipes.deserializeNBT(tag.getCompound(ENERGY_RECEIVER_PIPES));
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
            extractLOs.tryLoadCache(level);
            receiveLOs.tryLoadCache(level);
            bothLOs.tryLoadCache(level);
            energyReceiverPipes.tryLoadCache(level, (pos, tile) -> tile.connect(this));

            setChanged();//TODO addFuncにsetChanged
        }

        public ContainerData energyData = new ContainerData() {
            @Override
            public int get(int p_39284_) {
                return switch (p_39284_) {
                    case 0 -> energySlot.getEnergyStored();
                    case 1 -> extractLOs.valueCount();
                    case 2 -> receiveLOs.valueCount();
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
}
