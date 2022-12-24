package nin.transferpipe.handler;

import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import nin.transferpipe.TransferPipe;
import nin.transferpipe.data.TPBlockStateProvider;

@Mod.EventBusSubscriber(modid = TransferPipe.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenHandler {
    @SubscribeEvent
    public static void onDataGen(GatherDataEvent event) {
        var data = event.getGenerator();
        data.addProvider(event.includeClient(), new TPBlockStateProvider(data, TransferPipe.MOD_ID, event.getExistingFileHelper()));
    }
}
