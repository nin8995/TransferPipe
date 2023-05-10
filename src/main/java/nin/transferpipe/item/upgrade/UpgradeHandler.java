package nin.transferpipe.item.upgrade;

import nin.transferpipe.block.node.BaseTileNode;
import nin.transferpipe.util.forge.TileItemHandler;

public class UpgradeHandler extends TileItemHandler<BaseTileNode> {

    public UpgradeHandler(int size, BaseTileNode be) {
        super(size, be);
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        be.calcUpgrades();
    }
}
