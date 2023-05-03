package nin.transferpipe.gui;


import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import nin.transferpipe.util.forge.ObscuredInventory;

import java.util.Optional;

public class PatternSlot extends SlotItemHandler implements SwapRestricted {

    public PatternSlot(ObscuredInventory itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public ItemStack safeInsert(ItemStack p_150660_, int p_150658_) {
        set(p_150660_.copyWithCount(1));
        return p_150660_;
    }
/*
            @Override
            public ItemStack safeTake(int p_150648_, int p_150649_, Player p_150650_) {
                set(ItemStack.EMPTY);
                return ItemStack.EMPTY;
            }*/

    @Override
    public Optional<ItemStack> tryRemove(int p_150642_, int p_150643_, Player p_150644_) {
        set(ItemStack.EMPTY);
        return Optional.empty();
    }

    public boolean mayPlace(ItemStack itemStack) {
        return false;
    }

    public boolean mayPickup(Player player) {
        return true;
    }
}
