package nin.transferpipe.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.node.TransferNodeEnergy;
import nin.transferpipe.util.forge.ReferenceEnergyStorage;
import nin.transferpipe.util.forge.RegistryEntityBlock;
import nin.transferpipe.util.minecraft.BaseTile;
import nin.transferpipe.util.minecraft.ITickingEntityBlock;
import nin.transferpipe.util.transferpipe.TPUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnergyReceiverPipe extends EnergyPipe implements ITickingEntityBlock<EnergyReceiverPipe.Tile> {

    @Override
    public RegistryEntityBlock<Tile> registry() {
        return TPBlocks.ENERGY_RECEIVER_PIPE;
    }

    public static class Tile extends BaseTile {

        @Nullable
        public TransferNodeEnergy.Tile node;
        public LazyOptional<ReferenceEnergyStorage> loReferencedEnergy = LazyOptional.empty();

        public Tile(BlockPos p_155229_, BlockState p_155230_) {
            super(TPBlocks.ENERGY_RECEIVER_PIPE.tile(), p_155229_, p_155230_);
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == ForgeCapabilities.ENERGY
                           && loReferencedEnergy.isPresent()
                           && (side == null || TPUtils.connection(getBlockState(), side) == Connection.MACHINE)
                   ? loReferencedEnergy.cast()
                   : super.getCapability(cap, side);
        }

        @Override
        public void onRemove() {
            this.disConnect();
        }

        public void connect(TransferNodeEnergy.Tile node) {
            disConnect();
            this.node = node;
            loReferencedEnergy = node.getCapability(ForgeCapabilities.ENERGY).lazyMap(ReferenceEnergyStorage::new);
            setChanged();
        }

        public void disConnect() {
            if (node != null)
                node.energyReceiverPipes.remove(worldPosition);
        }

        public void reset() {
            node = null;
            loReferencedEnergy.invalidate();
            setChanged();
        }
    }
}
