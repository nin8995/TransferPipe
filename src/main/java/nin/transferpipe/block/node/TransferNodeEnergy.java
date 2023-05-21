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
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.forge.LazyOptionalMap;
import nin.transferpipe.util.forge.RegistryGUIEntityBlock;
import nin.transferpipe.util.java.ExceptionPredicate;
import nin.transferpipe.util.java.JavaUtils;
import nin.transferpipe.util.minecraft.BaseBlockMenu;
import nin.transferpipe.util.minecraft.TileMap;
import nin.transferpipe.util.transferpipe.TPUtils;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class TransferNodeEnergy extends BaseNodeBlock.Energy<TransferNodeEnergy.Tile> {

    @Override
    public RegistryGUIEntityBlock<Tile> registryWithGUI() {
        return TPBlocks.TRANSFER_NODE_ENERGY;
    }

    public static class Menu extends BaseNodeMenu.Energy {

        public Menu(int containerId, Inventory inv, FriendlyByteBuf buf) {
            this(new SimpleContainerData(5), new ItemStackHandler(), new ItemStackHandler(6), new SimpleContainerData(4), containerId, inv);
        }

        public Menu(ContainerData energyNodeData, IItemHandler charge, IItemHandler upgrades, ContainerData searchData, int containerId, Inventory inv) {
            super(TPBlocks.TRANSFER_NODE_ENERGY, energyNodeData, charge, upgrades, searchData, containerId, inv);
        }
    }

    public static class Screen extends BaseNodeScreen.Energy<Menu> {

        public Screen(Menu p_97741_, Inventory p_97742_, Component p_97743_) {
            super(p_97741_, p_97742_, p_97743_);
        }
    }

    public static class Tile extends BaseTileNodeEnergy {

        public Tile(BlockPos p_155229_, BlockState p_155230_) {
            super(TPBlocks.TRANSFER_NODE_ENERGY.tile(), p_155229_, p_155230_);
        }

        @Override
        public BaseBlockMenu menu(int id, Inventory inv) {
            return new Menu(energyData, chargeSlot, upgrades, searchData, id, inv);
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
        public final LazyOptionalMap<IEnergyStorage> insertLOs = loMap.get();
        public final LazyOptionalMap<IEnergyStorage> bothLOs = loMap.get();

        @Override
        public boolean canFacingWork() {
            return true;
        }

        @Override
        public void facingWork(BlockPos pos, Direction dir, IEnergyStorage inv) {
            tryEstablishConnection(pos, dir);
        }

        @Override
        public boolean canWork(IEnergyStorage inv) {
            return true;
        }

        @Override
        public void work(BlockPos pos, Direction dir, IEnergyStorage inv) {
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
                    insertLOs.addMarked(pos, dir, loEnergy);
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
            if (TPUtils.getInnerTile(level, pos) instanceof EnergyReceiverPipe.Tile receiver) {
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
            insertLOs.reset();
            bothLOs.reset();
            energyReceiverPipes.reset();
        }

        @Override
        public void tryWorldInteraction(BlockPos pos, Direction dir) {
        }

        /**
         * エネルギー管理
         */
        @Override
        public void afterTick() {
            //refresh
            refreshConnection(extractLOs, IEnergyStorage::canExtract);
            refreshConnection(insertLOs, IEnergyStorage::canReceive);
            refreshConnection(bothLOs, ForgeUtils::canBoth);

            //box to energy storages
            var extract = extractLOs.forceGetValues();
            var receive = insertLOs.forceGetValues();
            var both = bothLOs.forceGetValues();
            var entities = new ArrayList<IEnergyStorage>();

            //extract
            extractFrom(extract);
            if (worldInteraction > 0) {
                tryEntityInteraction(pos, entities::addAll);
                var cap = energySlot.getCapacity();
                energySlot.setCapacity(Math.max(cap, wi()));
                extractFrom(JavaUtils.filter(entities, IEnergyStorage::canExtract));
                extractFrom(JavaUtils.filter(entities, ForgeUtils::canBoth));
                energySlot.setCapacity(cap);
            }
            extractFrom(both);

            //insert
            ForgeUtils.forEnergyStorage(chargeSlot.getItem(), this::insertTo);
            insertTo(receive);
            insertTo(JavaUtils.filter(entities, IEnergyStorage::canReceive));
            insertTo(both);
            insertTo(JavaUtils.filter(entities, ForgeUtils::canBoth));
        }

        public void refreshConnection(LazyOptionalMap<IEnergyStorage> loEnergyMap, Predicate<IEnergyStorage> shouldSustain) {
            loEnergyMap.removeValueIf((pos, dir, loEnergy) ->
                    !loEnergy.isPresent()  //!isPresentとisEmptyはLazyOptionalにおいては違う
                            || loEnergy.filter(shouldSustain::test).isEmpty()
                            //なんかRFToolsのマルチブロック蓄電器はこれだけじゃ消去されなかったから、実際にやってエラー吐かれることを以て無効化とみなす
                            || ExceptionPredicate.failed(() -> loEnergy.resolve().get().getEnergyStored()));
        }

        /**
         * 読む必要ない
         */
        public static final String EXTRACT = "Extract";
        public static final String INSERT = "Insert";
        public static final String BOTH = "Both";
        public static final String ENERGY_RECEIVER_PIPES = "EnergyReceiverPipes";

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.put(EXTRACT, extractLOs.serializeNBT());
            tag.put(INSERT, insertLOs.serializeNBT());
            tag.put(BOTH, bothLOs.serializeNBT());
            tag.put(ENERGY_RECEIVER_PIPES, energyReceiverPipes.serializeNBT());
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            if (tag.contains(EXTRACT))
                extractLOs.deserializeNBT(tag.getCompound(EXTRACT));
            if (tag.contains(INSERT))
                insertLOs.deserializeNBT(tag.getCompound(INSERT));
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
            insertLOs.tryLoadCache(level);
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
                    case 2 -> insertLOs.valueCount();
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
