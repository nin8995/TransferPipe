package nin.transferpipe.block;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import nin.transferpipe.block.state.Flow;
import nin.transferpipe.block.tile.TileTransferNode;
import nin.transferpipe.block.tile.TileTransferNodeEnergy;
import nin.transferpipe.block.tile.TileTransferNodeItem;
import nin.transferpipe.block.tile.TileTransferNodeLiquid;
import nin.transferpipe.block.tile.gui.TransferNodeMenu;
import nin.transferpipe.block.tile.gui.TransferNodeScreen;
import nin.transferpipe.item.Upgrade;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

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
    RegistryGUIEntityBlock<TileTransferNodeItem, TransferNodeMenu.Item, TransferNodeScreen.Item> TRANSFER_NODE_ITEM = registerNode("transfer_node_item",
            TransferNodeBlock.Item::new, TileTransferNodeItem::new, TransferNodeMenu.Item::new, TransferNodeScreen.Item::new);
    RegistryGUIEntityBlock<TileTransferNodeLiquid, TransferNodeMenu.Liquid, TransferNodeScreen.Liquid> TRANSFER_NODE_LIQUID = registerNode("transfer_node_liquid",
            TransferNodeBlock.Liquid::new, TileTransferNodeLiquid::new, TransferNodeMenu.Liquid::new, TransferNodeScreen.Liquid::new);
    RegistryGUIEntityBlock<TileTransferNodeEnergy, TransferNodeMenu.Energy, TransferNodeScreen.Energy> TRANSFER_NODE_ENERGY = registerNode("transfer_node_energy",
            TransferNodeBlock.Energy::new, TileTransferNodeEnergy::new, TransferNodeMenu.Energy::new, TransferNodeScreen.Energy::new);

    static RegistryObject<Block> registerPipe(String id, Supplier<Block> block) {
        var ro = PIPES.register(id, block);
        ITEMS.register(id, () -> new Upgrade.BlockItem(ro.get(), new Item.Properties()));
        return ro;
    }

    static <T extends TileTransferNode, M extends TransferNodeMenu, U extends Screen & MenuAccess<M>>
    RegistryGUIEntityBlock<T, M, U> registerNode(String id,
                                                 Supplier<Block> block,
                                                 BlockEntityType.BlockEntitySupplier<T> type,
                                                 MenuType.MenuSupplier<M> menu,
                                                 MenuScreens.ScreenConstructor<M, U> screen) {
        var roBlock = NODE_BLOCKS.register(id, block);
        var roEntity = NODE_BES.register(id, () -> BlockEntityType.Builder.of(type, roBlock.get()).build(null));
        var roMenu = NODE_MENUS.register(id, () -> new MenuType<>(menu, FeatureFlags.DEFAULT_FLAGS));
        var registry = new RegistryGUIEntityBlock<>(roBlock, roEntity, roMenu, screen);
        ITEMS.register(id, () -> new BlockItem(registry.block(), new Item.Properties()));
        NODES.add(registry);
        return registry;
    }

    static void init(IEventBus bus) {
        PIPES.register(bus);
        NODE_BLOCKS.register(bus);
        NODE_BES.register(bus);
        NODE_MENUS.register(bus);
        ITEMS.register(bus);
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
            var name = ro.getId().getPath();
            var center = new ModelFile.UncheckedModelFile(modLoc("block/" + name + "_center"));
            var limb = new ModelFile.UncheckedModelFile(modLoc("block/" + name + "_limb"));
            var joint = new ModelFile.UncheckedModelFile(modLoc("block/" + name + "_joint"));
            var overlayIgnoreCenter = new ModelFile.UncheckedModelFile(modLoc("block/overlay_ignore_center"));
            var overlayIgnoreLimb = new ModelFile.UncheckedModelFile(modLoc("block/overlay_ignore_limb"));
            var overlayOneway = new ModelFile.UncheckedModelFile(modLoc("block/overlay_oneway"));
            var mb = getMultipartBuilder(block);

            mb.part().modelFile(center).addModel().end();//中心
            mb.part().modelFile(overlayIgnoreCenter).addModel()//無視時中心オーバーレイ
                    .condition(FLOW, IGNORE).end();

            Direction.stream().forEach(dir -> {
                rotate(mb.part().modelFile(limb), dir).addModel()//管
                        .condition(CONNECTIONS.get(dir), PIPE, MACHINE).end();//パイプと機械に向けて

                rotate(mb.part().modelFile(joint), dir).addModel()//接合部
                        .condition(CONNECTIONS.get(dir), MACHINE).end();//機械に向けて

                rotate(mb.part().modelFile(overlayIgnoreLimb), dir).addModel()//無視時管オーバーレイ
                        .condition(FLOW, IGNORE)//無視したいとき
                        .condition(CONNECTIONS.get(dir), PIPE).end();//パイプに向けて

                var oneWayStates = Flow.stream().filter(f -> (f.toDir() != null && f.toDir() != dir) || f == Flow.BLOCK);
                rotate(mb.part().modelFile(overlayOneway), dir).addModel()//一方通行オーバーレイ
                        .condition(FLOW, oneWayStates.toArray(Flow[]::new))//Flowが今やってる方向以外または塞ぎ込んでるとき
                        .condition(CONNECTIONS.get(dir), PIPE).end();//パイプに向けて
            });

            var inv = new ModelFile.UncheckedModelFile(modLoc("block/" + name + "_inv"));
            simpleBlockItem(block, inv);
        }

        private void node(RegistryObject<Block> ro) {
            var block = ro.get();
            var name = ro.getId().getPath();
            var model = new ModelFile.UncheckedModelFile(modLoc("block/" + name));

            if (block instanceof TransferNodeBlock.FacingNode) {
                var mb = getMultipartBuilder(block);
                Direction.stream().forEach(dir ->
                        rotate(mb.part().modelFile(model), dir).addModel()
                                .condition(TransferNodeBlock.FacingNode.FACING, dir).end());
            } else
                simpleBlock(block, model);

            var inv = new ModelFile.UncheckedModelFile(modLoc("block/" + name + "_inv"));
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
