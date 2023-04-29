package nin.transferpipe.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.TickingEntityBlock;
import nin.transferpipe.block.node.TileTransferNodeEnergy;
import nin.transferpipe.util.forge.ReferenceEnergyStorage;
import nin.transferpipe.util.transferpipe.PipeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnergyReceiverPipe extends EnergyPipe implements TickingEntityBlock<EnergyReceiverPipe.Tile> {

    @Override
    public TPBlocks.RegistryEntityBlock<Tile> registry() {
        return TPBlocks.ENERGY_RECEIVER_PIPE;
    }

    public static class Tile extends nin.transferpipe.block.Tile {

        @Nullable
        public TileTransferNodeEnergy nodeReference = null;
        public LazyOptional<ReferenceEnergyStorage> loReferencedEnergy = LazyOptional.empty();

        public Tile(BlockPos p_155229_, BlockState p_155230_) {
            super(TPBlocks.ENERGY_RECEIVER_PIPE.tile(), p_155229_, p_155230_);
        }

        @Override
        public void onRemove() {
            this.disConnect();
        }

        public void connect(TileTransferNodeEnergy node) {
            disConnect();
            nodeReference = node;
            setChanged();
            loReferencedEnergy = node.getCapability(ForgeCapabilities.ENERGY)
                    .lazyMap(ReferenceEnergyStorage::new);
        }

        public void disConnect() {
            if (nodeReference != null) {
                nodeReference.energyReceiverPipes.remove(worldPosition);
                nodeReference = null;
                setChanged();
                loReferencedEnergy.invalidate();
            }
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return nodeReference != null && cap == ForgeCapabilities.ENERGY
                           && (side == null || PipeUtils.currentConnection(getBlockState(), side) == Connection.MACHINE)
                   ? loReferencedEnergy.cast()
                   : super.getCapability(cap, side);
        }

        /**
         * NBT
         */
        public static String NODE_POS = "NodePos";

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);

            if (nodeReference != null)
                tag.put(NODE_POS, NbtUtils.writeBlockPos(nodeReference.POS));
        }

        private BlockPos initPos = null;

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            if (tag.contains(NODE_POS))
                initPos = NbtUtils.readBlockPos(tag.getCompound(NODE_POS));
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (initPos != null)
                nodeReference = level.getBlockEntity(initPos) instanceof TileTransferNodeEnergy tile ? tile : null;
        }
    }
}
