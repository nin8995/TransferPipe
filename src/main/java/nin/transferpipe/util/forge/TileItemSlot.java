package nin.transferpipe.util.forge;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import nin.transferpipe.util.java.JavaUtils;
import nin.transferpipe.util.minecraft.MCUtils;

public class TileItemSlot<T extends BlockEntity> extends TileItemHandler<T> {

    public TileItemSlot(T be) {
        super(1, be);
    }

    public ItemStack getItem() {
        return getStackInSlot(0);
    }

    public void setItem(ItemStack item) {
        setStackInSlot(0, item);
    }

    public void insert(ItemStack item) {
        setItem(getItem().isEmpty() ? item : MCUtils.copyWithAdd(getItem(), item));
    }

    public void extract(ItemStack item) {
        setItem(MCUtils.copyWithSub(getItem(), item));
    }

    public int getCount() {
        return getItem().getCount();
    }

    public int getMaxStackSize() {
        return (int) (getItem().getMaxStackSize() * capacityRate);
    }

    public int getFreeSpace() {
        return getFreeSpace(getMaxStackSize());
    }

    public int getFreeSpace(int maxStackSize) {
        return maxStackSize - getCount();
    }

    public boolean isEmpty() {
        return getItem().isEmpty();
    }

    public boolean hasFreeSpace() {
        return getFreeSpace() > 0;
    }

    public boolean isFull() {
        return getFreeSpace() == 0;
    }

    public boolean hasItem() {
        return !isEmpty();
    }

    public boolean canStack(ItemStack item) {
        return JavaUtils.fork(getItem().isEmpty(),
                !item.isEmpty(), MCUtils.same(item, getItem()));
    }
}
