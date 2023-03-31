package nin.transferpipe;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.TransferNodeBlockEntity;
import org.slf4j.Logger;

//初期化＆イベント処理
@Mod(TransferPipe.MODID)
@Mod.EventBusSubscriber(modid = TransferPipe.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TransferPipe {

    public static final String MODID = "transferpipe";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TransferPipe() {
        var eb = FMLJavaModLoadingContext.get().getModEventBus();
        TPBlocks.init(eb);
    }

    @SubscribeEvent
    public static void onDataGen(GatherDataEvent event) {
        var data = event.getGenerator();
        var output = data.getPackOutput();
        data.addProvider(event.includeClient(), new TPBlocks.DataGen(output, MODID, event.getExistingFileHelper()));
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers e) {
        TPBlocks.NODE_BES.getEntries().forEach(type ->
                e.registerBlockEntityRenderer((BlockEntityType<? extends TransferNodeBlockEntity>) type.get(), TransferNodeBlockEntity.Renderer::new));
    }
}
