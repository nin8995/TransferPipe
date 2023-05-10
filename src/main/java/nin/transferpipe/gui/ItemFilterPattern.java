package nin.transferpipe.gui;


import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.Optional;

public class ItemFilterPattern extends SlotItemHandler implements SwapRestricted, PatternSlot {

    public ItemFilterPattern(IItemHandler inv, int index, int xPosition, int yPosition) {
        super(inv, index, xPosition, yPosition);
    }

    @Override
    public boolean shouldSet(ItemStack item) {
        return true;
    }

    @Override
    public void setPattern(ItemStack item) {
        set(item.copyWithCount(1));
    }

    @Override
    public void resetPattern() {
        set(ItemStack.EMPTY);
    }

    @Override
    public boolean isSamePattern(ItemStack item) {
        return ItemStack.isSameItemSameTags(item, getItem());
    }

    @Override
    public boolean hasPattern() {
        return hasItem();
    }

    @Override
    public ItemStack safeInsert(ItemStack item, int p_150658_) {
        trySetPattern(item);
        return item;
    }

    @Override
    public Optional<ItemStack> tryRemove(int p_150642_, int p_150643_, Player p_150644_) {
        resetPattern();
        return Optional.empty();
    }

    /**
     * doClick制御１
     */
    public boolean mayPlace(ItemStack itemStack) {
        return false;
    }

    /**
     * doClick制御２
     */
    public boolean mayPickup(Player player) {
        return true;
    }
}
