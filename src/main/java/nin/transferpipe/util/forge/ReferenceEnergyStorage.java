package nin.transferpipe.util.forge;

import net.minecraftforge.energy.IEnergyStorage;

public class ReferenceEnergyStorage implements IEnergyStorage {

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
