package nin.transferpipe.gui;

import net.minecraft.world.item.ItemStack;

public interface PatternSlot {

    boolean shouldSet(ItemStack item);

    void setPattern(ItemStack item);

    void resetPattern();

    default void trySetPattern(ItemStack item) {
        if (shouldSet(item))
            setPattern(item);
    }

    boolean isSamePattern(ItemStack item);

    boolean hasPattern();
}
