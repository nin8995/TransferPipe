package nin.transferpipe;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.RegistryObject;

public record RegistryGUI(RegistryObject<MenuType<?>> roMenu, MenuScreens.ScreenConstructor<?, ?> screen) {

    public MenuType<?> menu() {
        return roMenu.get();
    }
}
