package nin.transferpipe.block;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import nin.transferpipe.block.state.Flow;
import nin.transferpipe.item.Upgrade;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static nin.transferpipe.TransferPipe.MODID;
import static nin.transferpipe.block.TransferPipeBlock.CONNECTIONS;
import static nin.transferpipe.block.TransferPipeBlock.FLOW;
import static nin.transferpipe.block.state.Connection.MACHINE;
import static nin.transferpipe.block.state.Connection.PIPE;
import static nin.transferpipe.block.state.Flow.IGNORE;

//public static finalの省略＆staticインポートの明示として実装
public interface TPBlocks {

    DeferredRegister<Block> PIPES = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    DeferredRegister<Block> NODE_BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    DeferredRegister<BlockEntityType<?>> NODE_BES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    DeferredRegister<MenuType<?>> NODE_MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    Set<RegistryGUIEntityBlock> NODES = new HashSet<>();

    RegistryObject<Block> TRANSFER_PIPE = registerPipe("transfer_pipe", TransferPipeBlock::new);
    RegistryGUIEntityBlock<TransferNodeBlockEntity, TransferNodeMenu.Item, TransferNodeScreen.Item> TRANSFER_NODE_ITEM = registerNode("transfer_node_item",
            TransferNodeBlock.Item::new, TransferNodeBlockEntity.Item::new, TransferNodeMenu.Item::new, TransferNodeScreen.Item::new);

    static RegistryObject<Block> registerPipe(String id, Supplier<Block> block) {
        var ro = PIPES.register(id, block);
        ITEMS.register(id, () -> new Upgrade.BlockItem(ro.get(), new Item.Properties()));
        return ro;
    }

    static <M extends TransferNodeMenu, U extends Screen & MenuAccess<M>>
    RegistryGUIEntityBlock<TransferNodeBlockEntity, M, U> registerNode(String id,
                                                                       Supplier<Block> block,
                                                                       BlockEntityType.BlockEntitySupplier<TransferNodeBlockEntity> type,
                                                                       MenuType.MenuSupplier<M> menu,
                                                                       MenuScreens.ScreenConstructor<M, U> screen) {
        var roBlock = NODE_BLOCKS.register(id, block);
        var roEntity = NODE_BES.register(id, () -> BlockEntityType.Builder.of(type, roBlock.get()).build(null));
        var roMenu = NODE_MENUS.register(id, () -> new MenuType<>(menu, FeatureFlags.DEFAULT_FLAGS));
        var ro = new RegistryGUIEntityBlock<>(roBlock, roEntity, roMenu, screen);
        ITEMS.register(id, () -> new BlockItem(ro.block(), new Item.Properties()));
        NODES.add(ro);
        return ro;
    }

    static void init(IEventBus eb) {
        PIPES.register(eb);
        NODE_BLOCKS.register(eb);
        NODE_BES.register(eb);
        NODE_MENUS.register(eb);
        ITEMS.register(eb);

        //クリエタブ登録
        eb.addListener((CreativeModeTabEvent.Register e) -> e.registerCreativeModeTab(new ResourceLocation(MODID, MODID), b -> b
                .icon(() -> new ItemStack(TRANSFER_PIPE.get()))
                .title(Component.translatable(MODID))
                .displayItems((params, output) -> ITEMS.getEntries().stream().map(RegistryObject::get).forEach(output::accept))
        ));
    }

    //EntityBlockにまつわるRegistryObjectをコード上で取得しやすい用
    record RegistryGUIEntityBlock<T extends BlockEntity, M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>>
            (RegistryObject<Block> roBlock,
             RegistryObject<BlockEntityType<T>> roEntity,
             RegistryObject<MenuType<M>> roMenu,
             MenuScreens.ScreenConstructor<M, U> screen) {

        public Block block() {
            return roBlock.get();
        }

        public BlockEntityType<T> entity() {
            return roEntity.get();
        }

        public MenuType<M> menu() {
            return roMenu.get();
        }
    }

    //ここで登録したもののDataGen
    class DataGen extends BlockStateProvider {

        public DataGen(PackOutput output, String modid, ExistingFileHelper exFileHelper) {
            super(output, modid, exFileHelper);
        }

        @Override
        protected void registerStatesAndModels() {
            PIPES.getEntries().forEach(this::pipe);
            NODE_BLOCKS.getEntries().forEach(this::node);
        }

        private void pipe(RegistryObject<Block> ro) {
            var block = ro.get();
            var id = ro.getId().getPath();
            var center = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_center"));
            var limb = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_limb"));
            var joint = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_joint"));
            var overlayIgnoreCenter = new ModelFile.UncheckedModelFile(modLoc("block/overlay_ignore_center"));
            var overlayIgnoreLimb = new ModelFile.UncheckedModelFile(modLoc("block/overlay_ignore_limb"));
            var overlayOneway = new ModelFile.UncheckedModelFile(modLoc("block/overlay_oneway"));
            var mb = getMultipartBuilder(block);

            mb.part().modelFile(center).addModel().end();//中心
            mb.part().modelFile(overlayIgnoreCenter).addModel()//無視時中心オーバーレイ
                    .condition(FLOW, IGNORE).end();

            Direction.stream().forEach(d -> {
                rotate(mb.part().modelFile(limb), d).addModel()//管
                        .condition(CONNECTIONS.get(d), PIPE, MACHINE).end();//パイプと機械に向けて

                rotate(mb.part().modelFile(joint), d).addModel()//接合部
                        .condition(CONNECTIONS.get(d), MACHINE).end();//機械に向けて

                rotate(mb.part().modelFile(overlayIgnoreLimb), d).addModel()//無視時管オーバーレイ
                        .condition(FLOW, IGNORE)//無視したいとき
                        .condition(CONNECTIONS.get(d), PIPE).end();//パイプに向けて

                var oneWayStates = Direction.stream().filter(f -> f != d).map(Flow::fromDirection).collect(Collectors.toSet());
                oneWayStates.add(Flow.BLOCK);
                rotate(mb.part().modelFile(overlayOneway), d).addModel()//一方通行オーバーレイ
                        .condition(FLOW, oneWayStates.toArray(new Flow[]{}))//Flowが今やってる方向以外または塞ぎ込んでるとき
                        .condition(CONNECTIONS.get(d), PIPE).end();//パイプに向けて
            });

            var inv = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_inv"));
            simpleBlockItem(block, inv);
        }

        private void node(RegistryObject<Block> ro) {
            var block = ro.get();
            var id = ro.getId().getPath();
            var model = new ModelFile.UncheckedModelFile(modLoc("block/" + id));


            var mb = getMultipartBuilder(block);
            Direction.stream().forEach(d ->
                    rotate(mb.part().modelFile(model), d).addModel()
                            .condition(TransferNodeBlock.FACING, d).end());

            var inv = new ModelFile.UncheckedModelFile(modLoc("block/" + id + "_inv"));
            simpleBlockItem(block, inv);
        }

        public static ConfiguredModel.Builder<MultiPartBlockStateBuilder.PartBuilder> rotate(ConfiguredModel.Builder<MultiPartBlockStateBuilder.PartBuilder> m, Direction d) {
            return switch (d) {
                case DOWN -> m.rotationX(90);
                case UP -> m.rotationX(270);
                case NORTH -> m.rotationY(0);
                case SOUTH -> m.rotationY(180);
                case WEST -> m.rotationY(270);
                case EAST -> m.rotationY(90);
            };
        }
    }
}
