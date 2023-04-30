package nin.transferpipe.util.forge;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemHandlerHelper;

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

    public void receive(ItemStack item) {
        setItem(getItem().isEmpty() ? item : item.copyWithCount(item.getCount() + getItem().getCount()));
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

    public boolean canStack(ItemStack item) {
        return (getItem().isEmpty() && !item.isEmpty()) || ItemHandlerHelper.canItemStacksStack(item, getItem());
    }
}
