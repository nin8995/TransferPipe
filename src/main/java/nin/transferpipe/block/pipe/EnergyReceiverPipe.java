package nin.transferpipe.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.TickingEntityBlock;
import nin.transferpipe.block.TileNonStaticTicker;
import nin.transferpipe.block.node.TileTransferNodeEnergy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnergyReceiverPipe extends TransferPipe implements TickingEntityBlock<EnergyReceiverPipe.Tile> {

    @Override
    public TPBlocks.RegistryEntityBlock<Tile> registry() {
        return TPBlocks.ENERGY_RECEIVER_PIPE;
    }

    @Override
    public void onRemove(BlockState p_60515_, Level p_60516_, BlockPos p_60517_, BlockState p_60518_, boolean p_60519_) {
        if (p_60516_.getBlockEntity(p_60517_) instanceof Tile tile)
            tile.disConnect();
        super.onRemove(p_60515_, p_60516_, p_60517_, p_60518_, p_60519_);
    }

    public static class Tile extends TileNonStaticTicker {

        @Nullable
        public TileTransferNodeEnergy nodeReference = null;

        public static String NODE_POS = "NodePos";
        private BlockPos initPos = null;

        public Tile(BlockPos p_155229_, BlockState p_155230_) {
            super(TPBlocks.ENERGY_RECEIVER_PIPE.tile(), p_155229_, p_155230_);
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (nodeReference != null)
                tag.put(NODE_POS, NbtUtils.writeBlockPos(nodeReference.POS));
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            if (tag.contains(NODE_POS))
                initPos = NbtUtils.readBlockPos(tag.getCompound(NODE_POS));
        }

        public void connect(TileTransferNodeEnergy node) {
            disConnect();
            nodeReference = node;
        }

        public void disConnect() {
            if (nodeReference != null) {
                nodeReference.removeEnergyReceiverPipe(worldPosition);
                nodeReference = null;
            }
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return nodeReference != null ? nodeReference.getCapability(cap, side) : LazyOptional.empty();
        }

        @Override
        public void tick() {
            if (initPos != null) {
                nodeReference = level.getBlockEntity(initPos) instanceof TileTransferNodeEnergy tile ? tile : null;
                initPos = null;
            }

            if (nodeReference != null && !nodeReference.getCapability(ForgeCapabilities.ENERGY).isPresent())
                nodeReference = null;
        }
    }
}
