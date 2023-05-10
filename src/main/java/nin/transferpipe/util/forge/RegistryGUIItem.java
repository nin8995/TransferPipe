package nin.transferpipe.util.forge;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public record RegistryGUIItem(RegistryObject<Item> roItem, RegistryObject<MenuType<?>> roMenu, MenuScreens.ScreenConstructor<?, ?> screen) {

    public Item item() {
        return roItem.get();
    }

    public MenuType<?> menu() {
        return roMenu.get();
    }

    public RegistryGUI gui() {
        return new RegistryGUI(roMenu, screen);
    }
}
