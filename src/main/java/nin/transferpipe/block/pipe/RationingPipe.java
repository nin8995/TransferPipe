package nin.transferpipe.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class RationingPipe extends TransferPipe {

    protected final int ration;

    public RationingPipe(int ration) {
        this.ration = ration;
    }

    public int getItemRation(Level level, BlockPos pos) {
        return ration;
    }

    public int getLiquidRation(Level level, BlockPos pos) {
        return ration * 250;
    }
}
