package nin.transferpipe.block.pipe;

import net.minecraft.core.BlockPos;
import nin.transferpipe.block.node.TileBaseTransferNode;

import java.util.List;

public interface FunctionChanger {

    Object storeAndChange(BlockPos pos, TileBaseTransferNode node);

    void restore(Object cache, TileBaseTransferNode node);
}
