package nin.transferpipe.util.forge;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;

public class TileItemHandler<T extends BlockEntity> extends ItemStackHandler {

    public final T be;

    public TileItemHandler(int size, T be) {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
        this.be = be;
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        be.setChanged();
    }
}
