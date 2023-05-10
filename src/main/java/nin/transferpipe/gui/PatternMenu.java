package nin.transferpipe.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import nin.transferpipe.item.TPItems;
import nin.transferpipe.util.transferpipe.TPUtils;

import java.util.ArrayList;
import java.util.List;

public class PatternMenu extends BaseItemMenu {
    public List<List<? extends PatternSlot>> patternsList = new ArrayList<>();

    public PatternMenu(TPItems.RegistryGUIItem registry, int slot, int p_38852_, Inventory inv, String bg, int bgHeight) {
        super(registry, slot, p_38852_, inv, bg, bgHeight);
    }

    public void addPatterns(List<? extends PatternSlot> patterns) {
        patternsList.add(patterns);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var slot = this.slots.get(index);
        var item = slot.getItem();
        if (!item.isEmpty()) {
            if (hotbarStart <= index && index <= inventoryEnd) {
                if (patternsList.stream().noneMatch(patterns -> TPUtils.trySetPattern(patterns, item)))
                    moveAmongInventory(index);
            } else if (slot instanceof PatternSlot pattern)
                pattern.resetPattern();
        }
        return ItemStack.EMPTY;
    }
}
