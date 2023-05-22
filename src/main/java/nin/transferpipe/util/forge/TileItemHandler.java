package nin.transferpipe.util.forge;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import nin.transferpipe.util.minecraft.BaseTile;

import java.util.function.Consumer;

public class TileItemHandler<T extends BaseTile> extends ItemStackHandler {

    public final T tile;

    public TileItemHandler(int size, T tile) {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
        this.tile = tile;
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        tile.setChanged();
    }

    public float capacityRate = 1;

    @Override
    public int getSlotLimit(int slot) {
        return (int) (super.getSlotLimit(slot) * capacityRate);
    }

    public void forEachItem(Consumer<ItemStack> func) {
        stacks.forEach(func);
    }
}
