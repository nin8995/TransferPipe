package nin.transferpipe.block.pipe;

import nin.transferpipe.block.node.TileBaseTransferNode;
import nin.transferpipe.block.node.TileTransferNodeEnergy;

public class EnergyPipe extends TransferPipe {

    @Override
    public boolean isValidSearcher(TileBaseTransferNode node) {
        return node instanceof TileTransferNodeEnergy;
    }
}
