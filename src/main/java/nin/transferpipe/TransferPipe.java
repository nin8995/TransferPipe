package nin.transferpipe;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegistryObject;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.TransferNodeBlockEntity;
import nin.transferpipe.item.TPItems;
import org.slf4j.Logger;

//初期化＆イベント処理
@Mod(TransferPipe.MODID)
@Mod.EventBusSubscriber(modid = TransferPipe.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TransferPipe {

    public static final String MODID = "transferpipe";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TransferPipe() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        TPBlocks.init(bus);
        TPItems.init(bus);

        //クリエタブ登録
        bus.addListener((CreativeModeTabEvent.Register e) -> e.registerCreativeModeTab(new ResourceLocation(MODID, MODID), b -> b
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
    }

    @SubscribeEvent
    public static void clientInit(FMLClientSetupEvent e) {
        e.enqueueWork(() ->
                TPBlocks.NODES.forEach(node -> MenuScreens.register(node.menu(), node.screen())));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        TPBlocks.NODE_BES.getEntries().forEach(type ->
                e.registerBlockEntityRenderer((BlockEntityType<? extends TransferNodeBlockEntity>) type.get(), TransferNodeBlockEntity.Renderer::new));
    }
}
