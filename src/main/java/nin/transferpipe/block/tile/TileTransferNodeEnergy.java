package nin.transferpipe.block.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.particle.ColorSquare;
import nin.transferpipe.particle.TPParticles;

public class TileTransferNodeEnergy extends TileTransferNode {

    private int energy;

    public ContainerData energyData = new ContainerData() {
        @Override
        public int get(int p_39284_) {
            return p_39284_ == 0 ? energy : -1;
        }

        @Override
        public void set(int p_39285_, int p_39286_) {
            if (p_39285_ == 0)
                setEnergy(p_39286_);
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public TileTransferNodeEnergy(BlockPos p_155229_, BlockState p_155230_) {
        super(TPBlocks.TRANSFER_NODE_ENERGY.entity(), p_155229_, p_155230_);
    }

    @Override
    public void calcUpgrades() {
        super.calcUpgrades();
        pseudoRoundRobin = true;
        depthFirst = false;
        breadthFirst = true;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
        setChanged();
    }

    @Override
    public boolean shouldSearch() {
        return true;
    }

    @Override
    public void facing(BlockPos pos, Direction dir) {

    }

    @Override
    public void terminal(BlockPos pos, Direction dir) {

    }

    @Override
    public boolean canWork(BlockPos pos, Direction d) {
        return true;
    }

    @Override
    public ColorSquare.Option getParticleOption() {
        var rand = level.random.nextFloat();
        return new ColorSquare.Option(0.5F + 0.5F * rand, 0.5F + 0.5F * rand, 0, 1);
    }
}
