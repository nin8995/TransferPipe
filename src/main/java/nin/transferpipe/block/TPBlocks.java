package nin.transferpipe.block;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.flag.FeatureFlags;
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
import nin.transferpipe.block.node.*;
import nin.transferpipe.block.pipe.EnergyReceiverPipe;
import nin.transferpipe.block.pipe.TransferPipe;
import nin.transferpipe.block.state.Flow;
import nin.transferpipe.item.Upgrade;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import static nin.transferpipe.TPMod.MODID;
import static nin.transferpipe.block.pipe.TransferPipe.CONNECTIONS;
import static nin.transferpipe.block.pipe.TransferPipe.FLOW;
import static nin.transferpipe.block.state.Connection.MACHINE;
import static nin.transferpipe.block.state.Connection.PIPE;
import static nin.transferpipe.block.state.Flow.IGNORE;

//public static finalの省略＆staticインポートの明示として実装
public interface TPBlocks {

    Set<RegistryObject<Block>> PIPES = new HashSet<>();
    Set<RegistryGUIEntityBlock<? extends TileBaseTransferNode>> NODES = new HashSet<>();

    DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    //Pipes
    RegistryObject<Block> TRANSFER_PIPE = registerPipe("transfer_pipe", TransferPipe::new);
    RegistryEntityBlock<EnergyReceiverPipe.Tile> ENERGY_RECEIVER_PIPE = registerPipe("energy_receiver_pipe",
            EnergyReceiverPipe::new, EnergyReceiverPipe.Tile::new);

    //Nodes
    RegistryGUIEntityBlock<TileTransferNodeItem> TRANSFER_NODE_ITEM = registerNode("transfer_node_item",
            BlockTransferNode.Item::new, TileTransferNodeItem::new, MenuTransferNode.Item::new, ScreenTransferNode.Item::new);
    RegistryGUIEntityBlock<TileTransferNodeLiquid> TRANSFER_NODE_LIQUID = registerNode("transfer_node_liquid",
            BlockTransferNode.Liquid::new, TileTransferNodeLiquid::new, MenuTransferNode.Liquid::new, ScreenTransferNode.Liquid::new);
    RegistryGUIEntityBlock<TileTransferNodeEnergy> TRANSFER_NODE_ENERGY = registerNode("transfer_node_energy",
            BlockTransferNode.Energy::new, TileTransferNodeEnergy::new, MenuTransferNode.Energy::new, ScreenTransferNode.Energy::new);

    static RegistryObject<Block> registerPipe(String id, Supplier<Block> block) {
        var ro = BLOCKS.register(id, block);
        ITEMS.register(id, () -> new Upgrade.BlockItem(ro.get(), new Item.Properties()));
        PIPES.add(ro);
        return ro;
    }

    static <T extends BlockEntity> RegistryEntityBlock<T> registerPipe(String id, Supplier<Block> block, BlockEntityType.BlockEntitySupplier<T> tile) {
        var roBlock = BLOCKS.register(id, block);
        var roEntity = TILES.register(id, () -> BlockEntityType.Builder.of(tile, roBlock.get()).build(null));
        var registry = new RegistryEntityBlock<>(roBlock, roEntity, tile);
        ITEMS.register(id, () -> new Upgrade.BlockItem(registry.block(), new Item.Properties()));
        PIPES.add(roBlock);
        return registry;
    }

    static <T extends TileBaseTransferNode, M extends MenuTransferNode, U extends Screen & MenuAccess<M>>
    RegistryGUIEntityBlock<T> registerNode(String id,
                                           Supplier<Block> block,
                                           BlockEntityType.BlockEntitySupplier<T> tile,
                                           MenuType.MenuSupplier<M> menu,
                                           MenuScreens.ScreenConstructor<M, U> screen) {
        var roBlock = BLOCKS.register(id, block);
        var roEntity = TILES.register(id, () -> BlockEntityType.Builder.of(tile, roBlock.get()).build(null));
        var roMenu = MENUS.register(id, () -> new MenuType<>(menu, FeatureFlags.DEFAULT_FLAGS));
        var registry = new RegistryGUIEntityBlock<>(roBlock, roEntity, tile, (RegistryObject<MenuType<?>>) (Object) roMenu, screen);
        ITEMS.register(id, () -> new BlockItem(registry.block(), new Item.Properties()));
        NODES.add(registry);
        return registry;
    }

    static void init(IEventBus bus) {
        BLOCKS.register(bus);
        TILES.register(bus);
        MENUS.register(bus);
        ITEMS.register(bus);
    }

    //EntityBlockにまつわるRegistryObjectをコード上で取得しやすい用
    record RegistryGUIEntityBlock<T extends BlockEntity>
            (RegistryObject<Block> roBlock,
             RegistryObject<BlockEntityType<T>> roTile, BlockEntityType.BlockEntitySupplier<T> tileSupplier,
             RegistryObject<MenuType<?>> roMenu, MenuScreens.ScreenConstructor<?, ?> screenConstructor) {

        public Block block() {
            return roBlock.get();
        }

        public BlockEntityType<T> tile() {
            return roTile.get();
        }

        public MenuType<?> menu() {
            return roMenu.get();
        }
    }

    record RegistryEntityBlock<T extends BlockEntity>
            (RegistryObject<Block> roBlock,
             RegistryObject<BlockEntityType<T>> roTile, BlockEntityType.BlockEntitySupplier<T> tileSupplier) {

        public Block block() {
            return roBlock.get();
        }

        public BlockEntityType<T> tile() {
            return roTile.get();
        }
    }

    //ここで登録したもののDataGen
    class DataGen extends BlockStateProvider {

        private final ExistingFileHelper ex;

        public DataGen(PackOutput output, String modid, ExistingFileHelper exFileHelper) {
            super(output, modid, exFileHelper);
            this.ex = exFileHelper;
        }

        @Override
        protected void registerStatesAndModels() {
            PIPES.forEach(this::pipe);
            NODES.stream().map(RegistryGUIEntityBlock::roBlock).forEach(this::node);
        }

        private void pipe(RegistryObject<Block> ro) {
            var block = ro.get();
            var center = genModel("block/base_pipe_center", "_center", ro);
            var limb = genModel("block/base_pipe_limb", "_limb", ro);
            var joint = genModel("block/base_pipe_joint", "_joint", ro);
            var overlayIgnoreCenter = new ModelFile.ExistingModelFile(modLoc("block/overlay_ignore_center"), ex);
            var overlayIgnoreLimb = new ModelFile.ExistingModelFile(modLoc("block/overlay_ignore_limb"), ex);
            var overlayOneway = new ModelFile.ExistingModelFile(modLoc("block/overlay_oneway"), ex);
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

            var inv = genModel("block/base_pipe_inv", "_inv", ro);
            simpleBlockItem(block, inv);
        }

        private void node(RegistryObject<Block> ro) {
            var block = ro.get();

            if (block instanceof BlockTransferNode.FacingNode) {
                var model = genModel("block/transfer_node", "", ro);
                var mb = getMultipartBuilder(block);
                Direction.stream().forEach(dir ->
                        rotate(mb.part().modelFile(model), dir).addModel()
                                .condition(BlockTransferNode.FacingNode.FACING, dir).end());

                var inv = genModel("block/transfer_node_inv", "_inv", ro);
                simpleBlockItem(block, inv);
            } else {
                simpleBlockWithItem(block, new ModelFile.ExistingModelFile(ro.getId().withPath("block/" + ro.getId().getPath()), ex));
            }
        }

        public ModelFile genModel(String parent, String suffix, RegistryObject<Block> child) {
            var loc = child.getId();
            var texture = loc.withPath("block/" + loc.getPath());
            return models().getBuilder(loc.getPath() + suffix)
                    .parent(new ModelFile.ExistingModelFile(modLoc(parent), ex))
                    .texture("0", texture)
                    .texture("particle", texture);
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
