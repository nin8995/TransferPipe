package nin.transferpipe.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import nin.transferpipe.item.TPItems;

public abstract class BaseItemMenu extends BaseMenu {

    public BaseItemMenu(TPItems.RegistryGUIItem registry, int p_38852_, Inventory inv, String bg) {
        super(registry.gui(), p_38852_, inv, bg);
    }

    @Override
    public boolean stillValid(Player p_38874_) {
        return true;
    }
}
