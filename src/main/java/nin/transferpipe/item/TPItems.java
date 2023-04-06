package nin.transferpipe.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;

import static nin.transferpipe.TransferPipe.MODID;

public interface TPItems {
    DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    RegistryObject<Item> SPEED_UPGRADE = registerUpgrade("speed_upgrade");
    RegistryObject<Item> STACK_UPGRADE = registerUpgrade("stack_upgrade", p -> p.stacksTo(1));
    RegistryObject<Item> PSEUDO_ROUND_ROBIN_UPGRADE = registerUpgrade("pseudo_round_robin_upgrade", p -> p.stacksTo(1));
    RegistryObject<Item> DEPTH_FIRST_SEARCH_UPGRADE = registerUpgrade("depth_first_search_upgrade", p -> p.stacksTo(1));
    RegistryObject<Item> BREADTH_FIRST_SEARCH_UPGRADE = registerUpgrade("breadth_first_search_upgrade", p -> p.stacksTo(1));

    static RegistryObject<Item> registerUpgrade(String name) {
        return registerUpgrade(name, p -> p);
    }

    static RegistryObject<Item> registerUpgrade(String name, Function<Item.Properties, Item.Properties> p) {
        return ITEMS.register(name, () -> new Upgrade.Item(p.apply(new Item.Properties())));
    }

    static void init(IEventBus bus) {
        ITEMS.register(bus);
    }
}
