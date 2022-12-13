package nin.transferpipe;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.renderable.ITextureRenderTypeLookup;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import nin.transferpipe.block.TransferPipeBlock;
import org.slf4j.Logger;

import java.util.function.Supplier;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TransferPipe.MOD_ID)
public class TransferPipe {

    public static final String MOD_ID = "transferpipe";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Block> PIPES = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<Block> TRANSFER_PIPE = register("transfer_pipe", TransferPipeBlock::new);

    public TransferPipe() {
        IEventBus eb = FMLJavaModLoadingContext.get().getModEventBus();

        eb.addListener((CreativeModeTabEvent.Register e) -> e.registerCreativeModeTab(new ResourceLocation(MOD_ID, MOD_ID), b -> b
                .icon(() -> new ItemStack(TRANSFER_PIPE.get()))
                .title(Component.translatable(MOD_ID))
                .displayItems((f, o, ho) ->{
                    o.accept(TRANSFER_PIPE.get());
                })
        ));
        // Register the Deferred Register to the mod event bus so blocks get registered
        PIPES.register(eb);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(eb);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static RegistryObject<Block> register(String id, Supplier<Block> block){
        var ro = PIPES.register("transfer_pipe", block);
        ITEMS.register("transfer_pipe", () -> new BlockItem(ro.get(), new Item.Properties()));
        return ro;
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

        }
    }
}
