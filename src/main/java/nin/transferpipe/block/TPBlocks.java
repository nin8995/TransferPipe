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
import nin.transferpipe.block.pipe.EnergyPipe;
import nin.transferpipe.block.pipe.EnergyReceiverPipe;
import nin.transferpipe.block.pipe.Flow;
import nin.transferpipe.block.pipe.TransferPipe;
import nin.transferpipe.gui.BaseBlockMenu;
import nin.transferpipe.gui.RegistryGUI;
import nin.transferpipe.item.UpgradeBlockItem;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static nin.transferpipe.TPMod.MODID;
import static nin.transferpipe.block.pipe.Connection.MACHINE;
import static nin.transferpipe.block.pipe.Connection.PIPE;
import static nin.transferpipe.block.pipe.Flow.IGNORE;
import static nin.transferpipe.block.pipe.TransferPipe.CONNECTIONS;
import static nin.transferpipe.block.pipe.TransferPipe.FLOW;

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
    RegistryObject<Block> ENERGY_PIPE = registerPipe("energy_pipe", EnergyPipe::new);
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
        ITEMS.register(id, () -> new UpgradeBlockItem(ro.get(), new Item.Properties()));
        PIPES.add(ro);
        return ro;
    }

    static <T extends BlockEntity> RegistryEntityBlock<T> registerPipe(String id, Supplier<Block> block, BlockEntityType.BlockEntitySupplier<T> tile) {
        var roBlock = BLOCKS.register(id, block);
        var roEntity = TILES.register(id, () -> BlockEntityType.Builder.of(tile, roBlock.get()).build(null));
        var registry = new RegistryEntityBlock<>(roBlock, roEntity, tile);
        ITEMS.register(id, () -> new UpgradeBlockItem(registry.block(), new Item.Properties()));
        PIPES.add(roBlock);
        return registry;
    }

    static <T extends BlockEntity, M extends BaseBlockMenu, U extends Screen & MenuAccess<M>>
    RegistryGUIEntityBlock<T> registerPipe(String id, Supplier<Block> block, BlockEntityType.BlockEntitySupplier<T> tile,
                                           MenuType.MenuSupplier<M> menu, MenuScreens.ScreenConstructor<M, U> screen) {
        var registry = registerGUIEntityBlock(id, block, tile, menu, screen);
        PIPES.add(registry.roBlock);
        return registry;
    }

    static <T extends TileBaseTransferNode, M extends BaseBlockMenu, U extends Screen & MenuAccess<M>>
    RegistryGUIEntityBlock<T> registerNode(String id, Supplier<Block> block, BlockEntityType.BlockEntitySupplier<T> tile,
                                           MenuType.MenuSupplier<M> menu, MenuScreens.ScreenConstructor<M, U> screen) {
        var registry = registerGUIEntityBlock(id, block, tile, menu, screen);
        NODES.add(registry);
        return registry;
    }

    static <T extends BlockEntity, M extends BaseBlockMenu, U extends Screen & MenuAccess<M>>
    RegistryGUIEntityBlock<T> registerGUIEntityBlock(String id, Supplier<Block> block, BlockEntityType.BlockEntitySupplier<T> tile,
                                                     MenuType.MenuSupplier<M> menu, MenuScreens.ScreenConstructor<M, U> screen) {
        var roBlock = BLOCKS.register(id, block);
        var roEntity = TILES.register(id, () -> BlockEntityType.Builder.of(tile, roBlock.get()).build(null));
        var roMenu = MENUS.register(id, () -> new MenuType<>(menu, FeatureFlags.DEFAULT_FLAGS));
        var registry = new RegistryGUIEntityBlock<>(roBlock, roEntity, tile, (RegistryObject<MenuType<?>>) (Object) roMenu, screen);
        ITEMS.register(id, () -> new BlockItem(registry.block(), new Item.Properties()));
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
             RegistryObject<MenuType<?>> roMenu, MenuScreens.ScreenConstructor<?, ?> screen) {

        public Block block() {
            return roBlock.get();
        }

        public BlockEntityType<T> tile() {
            return roTile.get();
        }

        public MenuType<?> menu() {
            return roMenu.get();
        }

        public RegistryGUI gui() {
            return new RegistryGUI(roMenu, screen);
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
            var mb = getMultipartBuilder(block);

            //中心モデル
            var center = genPipeModel("block/pipe_center", ro);
            var overlayIgnoreCenter = genPipeModel("block/pipe_overlay_ignore_center", ro);
            forModel(mb, center, p -> p);//中心を無条件で
            forModel(mb, overlayIgnoreCenter, p -> p.condition(FLOW, IGNORE));//無視用を無視時に

            //管モデル
            var limb = genPipeModel("block/pipe_limb", ro);
            var machine = genPipeModel("block/pipe_machine", ro);
            var overlayIgnoreLimb = genPipeModel("block/pipe_overlay_ignore_limb", ro);
            var overlayOneway = genPipeModel("block/pipe_overlay_oneway", ro);
            Direction.stream().forEach(dir -> {
                forRotatedModel(mb, dir, limb, p -> p.condition(CONNECTIONS.get(dir), PIPE));//管をパイプに向けて

                forRotatedModel(mb, dir, machine, p -> p.condition(CONNECTIONS.get(dir), MACHINE));//機械用管を機械に向けて

                forRotatedModel(mb, dir, overlayIgnoreLimb, p -> p//無視用を
                        .condition(FLOW, IGNORE)//無視の時
                        .condition(CONNECTIONS.get(dir), PIPE));//パイプに

                forRotatedModel(mb, dir, overlayOneway, p -> p//一方通行を
                        .condition(FLOW, Flow.stream().filter(f -> !f.openTo(dir)).toArray(Flow[]::new))//一方通行の時
                        .condition(CONNECTIONS.get(dir), PIPE));//パイプに
            });

            //インベントリモデル
            var inv = genPipeModel("block/pipe_inv", ro);
            simpleBlockItem(block, inv);
        }

        private void node(RegistryObject<Block> ro) {
            var block = ro.get();

            if (block instanceof BlockTransferNode.FacingNode) {
                var mb = getMultipartBuilder(block);

                var node = genNodeModel("block/transfer_node", ro);
                Direction.stream().forEach(dir ->
                        forRotatedModel(mb, dir, node, p -> p.condition(BlockTransferNode.FacingNode.FACING, dir)));

                var inv = genNodeModel("block/transfer_node_inv", ro);
                simpleBlockItem(block, inv);
            } else
                simpleBlockWithItem(block, new ModelFile.ExistingModelFile(ro.getId().withPath("block/" + ro.getId().getPath()), ex));
        }

        public ModelFile genPipeModel(String parent, RegistryObject<Block> child) {
            return genModel(parent, parent.replace("block/pipe", ""), child);
        }

        public ModelFile genNodeModel(String parent, RegistryObject<Block> child) {
            return genModel(parent, parent.replace("block/transfer_node", ""), child);
        }

        public ModelFile genModel(String parent, String suffix, RegistryObject<Block> child) {
            var loc = child.getId();
            var texture = loc.withPath("block/" + loc.getPath());
            return models().getBuilder(loc.getPath() + suffix)
                    .parent(new ModelFile.ExistingModelFile(modLoc(parent), ex))
                    .texture("texture", texture)
                    .texture("particle", texture);
        }

        public static void forModel(MultiPartBlockStateBuilder mb, ModelFile model, Function<MultiPartBlockStateBuilder.PartBuilder, MultiPartBlockStateBuilder.PartBuilder> func) {
            func.apply(mb.part().modelFile(model).addModel()).end();
        }

        public static void forRotatedModel(MultiPartBlockStateBuilder mb, Direction dir, ModelFile model, Function<MultiPartBlockStateBuilder.PartBuilder, MultiPartBlockStateBuilder.PartBuilder> func) {
            func.apply(rotate(mb.part().modelFile(model), dir).addModel()).end();
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
