package nin.transferpipe.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import nin.transferpipe.item.TPItems;
import org.jetbrains.annotations.NotNull;

public abstract class BaseItemMenu extends BaseMenu {

    public int slot;

    public BaseItemMenu(TPItems.RegistryGUIItem registry, int slot, int p_38852_, Inventory inv, String bg, int bgHeight) {
        super(registry.gui(), p_38852_, inv, bg, bgHeight);
        this.slot = slot;
    }

    @Override
    public boolean stillValid(@NotNull Player p_38874_) {
        return true;
    }


    @Override
    public boolean shouldLock(Slot info, int index) {
        return shouldLock() && info.container instanceof Inventory && slot == index;
    }

    public boolean shouldLock() {
        return true;
    }
}
