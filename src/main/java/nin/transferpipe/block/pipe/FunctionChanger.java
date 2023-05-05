package nin.transferpipe.block.pipe;

import net.minecraft.core.BlockPos;
import nin.transferpipe.block.node.BaseTileNode;

public interface FunctionChanger {

    Object storeAndChange(BlockPos pos, BaseTileNode node);

    void restore(Object cache, BaseTileNode node);
}
