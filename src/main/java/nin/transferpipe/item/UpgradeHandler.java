package nin.transferpipe.item;

import nin.transferpipe.block.node.TileBaseTransferNode;
import nin.transferpipe.util.forge.TileItemHandler;

public class UpgradeHandler extends TileItemHandler<TileBaseTransferNode> {

    public UpgradeHandler(int size, TileBaseTransferNode be) {
        super(size, be);
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        be.calcUpgrades();
    }
}
