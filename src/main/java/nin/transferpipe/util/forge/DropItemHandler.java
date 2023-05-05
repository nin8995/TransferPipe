package nin.transferpipe.util.forge;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import nin.transferpipe.util.minecraft.MCUtils;
import org.jetbrains.annotations.NotNull;

public record DropItemHandler(ItemEntity dropItem) implements IItemHandler {

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return dropItem.getItem();
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!ItemHandlerHelper.canItemStacksStack(stack, dropItem.getItem()))
            return stack;
        var sum = dropItem.getItem().getCount() + stack.getCount();
        var toInsert = Math.min(dropItem.getItem().getMaxStackSize(), sum) - dropItem.getItem().getCount();

        if (!simulate)
            dropItem.setItem(MCUtils.copyWithAdd(dropItem.getItem(), toInsert));
        return stack.copyWithCount(stack.getCount() - toInsert);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        var toExtract = Math.min(dropItem.getItem().getCount(), amount);
        var extracted = dropItem.getItem().copyWithCount(toExtract);

        if (!simulate) {
            dropItem.setItem(MCUtils.copyWithSub(dropItem.getItem(), toExtract));
            if (dropItem.getItem().isEmpty())
                dropItem.discard();
        }
        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        return getStackInSlot(slot).getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return true;
    }
}
