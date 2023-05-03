package nin.transferpipe.item;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import nin.transferpipe.gui.RegistryGUI;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static nin.transferpipe.TPMod.MODID;

public interface TPItems {

    DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    List<RegistryGUI> GUI = new ArrayList<>();

    //Normal Upgrade
    RegistryObject<Item> SPEED_UPGRADE = registerUpgrade("speed_upgrade");
    RegistryObject<Item> CAPACITY_UPGRADE = registerUpgrade("capacity_upgrade");
    RegistryObject<Item> WORLD_INTERACTION_UPGRADE = registerUpgrade("world_interaction_upgrade");
    RegistryObject<Item> OVERCLOCK_UPGRADE = registerUpgrade("overclock_upgrade");
    RegistryObject<Item> STACK_UPGRADE = registerUpgrade("stack_upgrade", p -> p.stacksTo(1));
    RegistryObject<Item> PSEUDO_ROUND_ROBIN_UPGRADE = registerUpgrade("pseudo_round_robin_upgrade", p -> p.stacksTo(1));
    RegistryObject<Item> DEPTH_FIRST_SEARCH_UPGRADE = registerUpgrade("depth_first_search_upgrade", p -> p.stacksTo(1));
    RegistryObject<Item> BREADTH_FIRST_SEARCH_UPGRADE = registerUpgrade("breadth_first_search_upgrade", p -> p.stacksTo(1));
    RegistryObject<Item> SEARCH_MEMORY_UPGRADE = registerUpgrade("search_memory_upgrade", p -> p.stacksTo(1));

    //Functional Upgrade
    RegistryObject<Item> RATIONING_UPGRADE = register("rationing_upgrade", p -> new RationingUpgrade(64, p.stacksTo(1)));
    RegistryObject<Item> HYPER_RATIONING_UPGRADE = register("hyper_rationing_upgrade", p -> new RationingUpgrade(1, p.stacksTo(1)));
    RegistryGUIItem REGULATABLE_RATIONING_UPGRADE = registerGUIItem("regulatable_rationing_upgrade",
            p -> new RegulatableRationingUpgrade(p.stacksTo(1)), RegulatableRationingUpgrade.Menu::new, RegulatableRationingUpgrade.Screen::new);
    RegistryObject<Item> ITEM_SORTING_UPGRADE = register("item_sorting_upgrade", p -> new SortingUpgrade(SortingUpgrade.ITEM_SORT, p));
    RegistryObject<Item> MOD_SORTING_UPGRADE = register("mod_sorting_upgrade", p -> new SortingUpgrade(SortingUpgrade.MOD_SORT, p));
    RegistryObject<Item> CREATIVE_TAB_SORTING_UPGRADE = register("creative_tab_sorting_upgrade", p -> new SortingUpgrade(SortingUpgrade.CREATIVE_TAB_SORT, p));
    RegistryObject<Item> TAG_SORTING_UPGRADE = register("tag_sorting_upgrade", p -> new SortingUpgrade(SortingUpgrade.TAG_SORT, p));
    RegistryObject<Item> COMMON_TAG_SORTING_UPGRADE = register("common_tag_sorting_upgrade", p -> new SortingUpgrade(SortingUpgrade.COMMON_TAG_SORT, p));
    RegistryObject<Item> CLASS_SORTING_UPGRADE = register("class_sorting_upgrade", p -> new SortingUpgrade(SortingUpgrade.CLASS_SORT, p));
    RegistryObject<Item> COMMON_CLASS_SORTING_UPGRADE = register("common_class_sorting_upgrade", p -> new SortingUpgrade(SortingUpgrade.COMMON_CLASS_SORT, p));

    RegistryGUIItem ITEM_FILTER = registerGUIItem("item_filter",
            FilterItem::new, FilterItem.Menu::new, FilterItem.Screen::new);

    static RegistryObject<Item> registerUpgrade(String name) {
        return registerUpgrade(name, p -> p);
    }

    static RegistryObject<Item> registerUpgrade(String name, Function<Item.Properties, Item.Properties> pModifier) {
        return register(name, p -> new UpgradeItem(pModifier.apply(p)));
    }

    static RegistryObject<Item> registerFunctionUpgrade(String name) {
        return registerFunctionUpgrade(name, p -> p);
    }

    static RegistryObject<Item> registerFunctionUpgrade(String name, Function<Item.Properties, Item.Properties> pModifier) {
        return register(name, p -> new FunctionUpgrade(pModifier.apply(p)));
    }

    static RegistryObject<Item> register(String name, Function<Item.Properties, Item> item) {
        return ITEMS.register(name, () -> item.apply(new Item.Properties()));
    }

    static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>>
    RegistryGUIItem registerGUIItem(String name, Function<Item.Properties, Item> item, IContainerFactory<M> menu, MenuScreens.ScreenConstructor<M, U> screen) {
        var roItem = register(name, item);
        var roMenu = MENUS.register(name, () -> IForgeMenuType.create(menu));
        var registry = new RegistryGUIItem(roItem, (RegistryObject<MenuType<?>>) (Object) roMenu, screen);
        GUI.add(registry.gui());
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

        public RegistryGUI gui() {
            return new RegistryGUI(roMenu, screen);
        }
    }
}
