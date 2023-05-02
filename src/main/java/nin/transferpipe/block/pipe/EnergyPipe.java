package nin.transferpipe.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import nin.transferpipe.block.node.TileTransferNodeEnergy;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.transferpipe.Searcher;
import org.jetbrains.annotations.Nullable;

public class EnergyPipe extends TransferPipe {

    @Override
    public boolean isWorkPlace(Level level, BlockPos pos, @Nullable Direction dir) {
        return ForgeUtils.hasEnergyStorage(level, pos, dir);
    }

    @Override
    public boolean isValidSearcher(Searcher searcher) {
        return searcher instanceof TileTransferNodeEnergy;
    }
}
