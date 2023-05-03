package nin.transferpipe.block.pipe;

import net.minecraft.core.BlockPos;
import nin.transferpipe.block.node.TileBaseTransferNode;

import java.util.List;

public interface FunctionChanger {

    List<?> storeAndChange(BlockPos pos, TileBaseTransferNode node);

    void restore(List<?> cache, TileBaseTransferNode node);
}
