package nin.transferpipe.util.forge;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.energy.EnergyStorage;

public class TileEnergySlot<T extends BlockEntity> extends EnergyStorage {

    public final T be;

    public TileEnergySlot(int capacity, int maxReceive, int maxExtract, T be) {
        super(capacity, maxReceive, maxExtract, 0);
        this.be = be;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        be.setChanged();
        return super.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        be.setChanged();
        return super.extractEnergy(maxExtract, simulate);
    }

    public int getFreeSpace() {
        return getMaxEnergyStored() - getEnergyStored();
    }

    public void receive(int energy) {
        this.energy += energy;
    }
}
