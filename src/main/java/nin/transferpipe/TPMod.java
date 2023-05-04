package nin.transferpipe;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegistryObject;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.node.TileBaseTransferNode;
import nin.transferpipe.item.TPItems;
import nin.transferpipe.network.TPPackets;
import nin.transferpipe.particle.TPParticles;
import org.slf4j.Logger;

/**
 * 初期化＆イベント処理
 */
@Mod(TPMod.MODID)
@Mod.EventBusSubscriber(modid = TPMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TPMod {

    public static final String MODID = "transferpipe";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation loc(String id) {
        return new ResourceLocation(MODID, id);
    }

    public TPMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        TPBlocks.init(bus);
        TPItems.init(bus);
        TPParticles.init(bus);
        TPPackets.init();

        //クリエタブ登録
        bus.addListener((CreativeModeTabEvent.Register e) -> e.registerCreativeModeTab(loc(MODID), b -> b
                .icon(() -> new ItemStack(TPBlocks.TRANSFER_PIPE.get()))
                .title(Component.translatable(MODID))
                .displayItems((params, output) -> {
                    TPBlocks.ITEMS.getEntries().stream().map(RegistryObject::get).forEach(output::accept);
                    TPItems.ITEMS.getEntries().stream().map(RegistryObject::get).forEach(output::accept);
                })
        ));
    }

    @SubscribeEvent
    public static void onDataGen(GatherDataEvent event) {
        var data = event.getGenerator();
        var output = data.getPackOutput();
        data.addProvider(event.includeClient(), new TPBlocks.DataGen(output, MODID, event.getExistingFileHelper()));
        data.addProvider(event.includeClient(), new TPItems.DataGen(output, MODID, event.getExistingFileHelper()));
    }

    @SubscribeEvent
    public static void clientInit(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            TPBlocks.GUI.forEach(gui -> bindMenuAndScreen(gui.menu(), gui.screen()));
            TPItems.GUI.forEach(gui -> bindMenuAndScreen(gui.menu(), gui.screen()));
        });
    }

    public static void bindMenuAndScreen(MenuType menu, MenuScreens.ScreenConstructor screen) {
        MenuScreens.register(menu, screen);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        TPBlocks.NODES.forEach(node -> e.registerBlockEntityRenderer(node.tile(), TileBaseTransferNode.Renderer::new));
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent e) {
        TPParticles.clientInit(e);
    }
}
