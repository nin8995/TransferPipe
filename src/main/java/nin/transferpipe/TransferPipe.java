package nin.transferpipe;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
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
import nin.transferpipe.block.TransferNodeBlock;
import nin.transferpipe.block.TransferNodeBlockEntity;
import nin.transferpipe.block.TransferNodeRenderer;
import nin.transferpipe.block.TransferPipeBlock;
import org.slf4j.Logger;

import java.util.function.Supplier;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TransferPipe.MOD_ID)
public class TransferPipe {

    public static final String MOD_ID = "transferpipe";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Block> PIPES = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Block> NODES = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<Block> TRANSFER_PIPE = registerPipe("transfer_pipe", TransferPipeBlock::new);
    public static final RegistryObject<Block> TRANSFER_NODE_ITEM = registerNode("transfer_node_item", TransferNodeBlock::new);

    public static final RegistryObject<BlockEntityType<TransferNodeBlockEntity>> TRANSFER_NODE_ITEM_BE = BLOCK_ENTITY_TYPES.register("transfer_node_item",
            () -> BlockEntityType.Builder.of(TransferNodeBlockEntity::new, NODES.getEntries().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));


    public TransferPipe() {
        IEventBus eb = FMLJavaModLoadingContext.get().getModEventBus();
        eb.addListener((CreativeModeTabEvent.Register e) -> e.registerCreativeModeTab(new ResourceLocation(MOD_ID, MOD_ID), b -> b
                .icon(() -> new ItemStack(TRANSFER_PIPE.get()))
                .title(Component.translatable(MOD_ID))
                .displayItems((f, o, ho) -> {
                    NODES.getEntries().stream().map(RegistryObject::get).forEach(o::accept);
                    PIPES.getEntries().stream().map(RegistryObject::get).forEach(o::accept);
                })
        ));
        // Register the Deferred Register to the mod event bus so blocks get registered
        PIPES.register(eb);
        NODES.register(eb);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(eb);
        BLOCK_ENTITY_TYPES.register(eb);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static RegistryObject<Block> registerPipe(String id, Supplier<Block> block) {
        var ro = PIPES.register(id, block);
        ITEMS.register(id, () -> new BlockItem(ro.get(), new Item.Properties()));
        return ro;
    }

    public static RegistryObject<Block> registerNode(String id, Supplier<Block> block) {
        var ro = NODES.register(id, block);
        ITEMS.register(id, () -> new BlockItem(ro.get(), new Item.Properties()));
        return ro;
    }


    /*public static RegistryObject<BlockEntityType<TransferNodeBlockEntity>> registerBlockEntity(String transfer_node_item, Object aNew) {
    }*/

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerBlockEntityRenderer(TRANSFER_NODE_ITEM_BE.get(), TransferNodeRenderer::new);
        }
    }
}
