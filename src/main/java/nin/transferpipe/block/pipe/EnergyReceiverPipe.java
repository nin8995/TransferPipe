package nin.transferpipe.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import nin.transferpipe.block.NonStaticTickingEntity;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.TickingEntityBlock;
import nin.transferpipe.block.node.TileTransferNodeEnergy;
import nin.transferpipe.block.state.Connection;
import nin.transferpipe.util.PipeUtils;
import nin.transferpipe.util.TPUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnergyReceiverPipe extends EnergyPipe implements TickingEntityBlock<EnergyReceiverPipe.Tile> {

    @Override
    public TPBlocks.RegistryEntityBlock<Tile> registry() {
        return TPBlocks.ENERGY_RECEIVER_PIPE;
    }

    public static class Tile extends NonStaticTickingEntity {

        @Nullable
        public TileTransferNodeEnergy nodeReference = null;
        @Nullable
        public LazyOptional<ReferenceEnergyStorage> lo = null;

        public static String NODE_POS = "NodePos";
        private BlockPos initPos = null;

        public Tile(BlockPos p_155229_, BlockState p_155230_) {
            super(TPBlocks.ENERGY_RECEIVER_PIPE.tile(), p_155229_, p_155230_);
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);

            TPUtils.forNullable(nodeReference, t -> tag.put(NODE_POS, NbtUtils.writeBlockPos(t.POS)));
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            if (tag.contains(NODE_POS))
                initPos = NbtUtils.readBlockPos(tag.getCompound(NODE_POS));
        }

        @Override
        public void onRemove() {
            this.disConnect();
        }

        public void connect(TileTransferNodeEnergy node) {
            disConnect();
            nodeReference = node;
            setChanged();
        }

        public void disConnect() {
            if (nodeReference != null) {
                nodeReference.removeEnergyReceiverPipe(worldPosition);
                nodeReference = null;
                setChanged();
                TPUtils.forNullable(lo, LazyOptional::invalidate);
            }
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == ForgeCapabilities.ENERGY && nodeReference != null
                    && side != null && PipeUtils.currentConnection(getBlockState(), side) == Connection.MACHINE) {
                var nodeCap = nodeReference.getCapability(ForgeCapabilities.ENERGY, side);
                if (nodeCap.isPresent()) {
                    var wrappedCap = new ReferenceEnergyStorage((nodeCap.resolve().get()));
                    return LazyOptional.of(() -> wrappedCap).cast();
                }
            }
            return LazyOptional.empty();
        }

        @Override
        public void tick() {
            if (initPos != null) {
                nodeReference = level.getBlockEntity(initPos) instanceof TileTransferNodeEnergy tile ? tile : null;
                initPos = null;
            }

            if (nodeReference != null && !nodeReference.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
                nodeReference = null;
                setChanged();
                TPUtils.forNullable(lo, LazyOptional::invalidate);
            }
        }

        public static class ReferenceEnergyStorage implements IEnergyStorage {

            private final IEnergyStorage es;

            public ReferenceEnergyStorage(IEnergyStorage es) {
                this.es = es;
            }

            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                return es.receiveEnergy(maxReceive, simulate);
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return es.extractEnergy(maxExtract, simulate);
            }

            @Override
            public int getEnergyStored() {
                return es.getEnergyStored();
            }

            @Override
            public int getMaxEnergyStored() {
                return es.getMaxEnergyStored();
            }

            @Override
            public boolean canExtract() {
                return false;
            }

            @Override
            public boolean canReceive() {
                return es.canReceive();
            }
        }
    }
}
