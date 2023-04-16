package nin.transferpipe.item;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static nin.transferpipe.TPMod.MODID;

public interface TPItems {

    DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    List<RegistryGUIItem> MENU_SCREENS = new ArrayList<>();

    RegistryObject<Item> SPEED_UPGRADE = registerUpgrade("speed_upgrade");
    RegistryObject<Item> AMPLIFIED_SPEED_UPGRADE = registerUpgrade("amplified_speed_upgrade");
    RegistryObject<Item> STACK_UPGRADE = registerUpgrade("stack_upgrade", p -> p.stacksTo(1));
    RegistryObject<Item> PSEUDO_ROUND_ROBIN_UPGRADE = registerUpgrade("pseudo_round_robin_upgrade", p -> p.stacksTo(1));
    RegistryObject<Item> DEPTH_FIRST_SEARCH_UPGRADE = registerUpgrade("depth_first_search_upgrade", p -> p.stacksTo(1));
    RegistryObject<Item> BREADTH_FIRST_SEARCH_UPGRADE = registerUpgrade("breadth_first_search_upgrade", p -> p.stacksTo(1));
    RegistryObject<Item> RATIONING_UPGRADE = register("rationing_upgrade", p -> new RationingUpgradeItem(64, p.stacksTo(1)));
    RegistryObject<Item> MINIMAL_RATIONING_UPGRADE = register("minimal_rationing_upgrade", p -> new RationingUpgradeItem(1, p.stacksTo(1)));
    RegistryGUIItem REGULATABLE_RATIONING_UPGRADE = registerGUIItem("regulatable_rationing_upgrade",
            p -> new RationingUpgradeItem.Regulatable(p.stacksTo(1)), RationingUpgradeItem.Regulatable.Menu::new, RationingUpgradeItem.Regulatable.Screen::new);

    static RegistryObject<Item> registerUpgrade(String name) {
        return registerUpgrade(name, p -> p);
    }

    static RegistryObject<Item> registerUpgrade(String name, Function<Item.Properties, Item.Properties> pModifier) {
        return register(name, p -> new Upgrade.Item(pModifier.apply(p)));
    }

    static RegistryObject<Item> registerFunctionUpgrade(String name) {
        return registerFunctionUpgrade(name, p -> p);
    }

    static RegistryObject<Item> registerFunctionUpgrade(String name, Function<Item.Properties, Item.Properties> pModifier) {
        return register(name, p -> new Upgrade.Function(pModifier.apply(p)));
    }

    static RegistryObject<Item> register(String name, Function<Item.Properties, Item> item) {
        return ITEMS.register(name, () -> item.apply(new Item.Properties()));
    }

    static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>>
    RegistryGUIItem registerGUIItem(String name, Function<Item.Properties, Item> item, MenuType.MenuSupplier<M> menu, MenuScreens.ScreenConstructor<M, U> screen) {
        var roItem = register(name, item);
        var roMenu = MENUS.register(name, () -> new MenuType<>(menu, FeatureFlags.DEFAULT_FLAGS));
        var registry = new RegistryGUIItem(roItem, (RegistryObject<MenuType<?>>) (Object) roMenu, screen);
        MENU_SCREENS.add(registry);
        return registry;
    }

    static void init(IEventBus bus) {
        ITEMS.register(bus);
        MENUS.register(bus);
    }

    record RegistryGUIItem(RegistryObject<Item> roItem, RegistryObject<MenuType<?>> roMenu, MenuScreens.ScreenConstructor<?, ?> screen) {

        public MenuType<?> menu() {
            return roMenu.get();
        }
    }
}
