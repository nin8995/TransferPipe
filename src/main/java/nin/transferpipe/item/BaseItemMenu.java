package nin.transferpipe.item;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import nin.transferpipe.BaseMenu;

public abstract class BaseItemMenu extends BaseMenu {

    protected BaseItemMenu(TPItems.RegistryGUIItem registry, int p_38852_, Inventory inv, String bg, boolean noItemSlots) {
        super(registry.gui(), p_38852_, inv, bg, noItemSlots);
    }

    @Override
    public boolean stillValid(Player p_38874_) {
        return true;
    }
}
