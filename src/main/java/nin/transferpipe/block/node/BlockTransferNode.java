package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nin.transferpipe.block.GUIEntityBlock;
import nin.transferpipe.block.LightingBlock;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.pipe.TransferPipe;
import nin.transferpipe.gui.BaseBlockMenu;
import nin.transferpipe.util.minecraft.MCUtils;
import nin.transferpipe.util.transferpipe.PipeInstance;
import nin.transferpipe.util.transferpipe.TPUtils;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * TileBaseTransferNodeを持つブロック
 */
public abstract class BlockTransferNode<T extends TileBaseTransferNode> extends LightingBlock implements GUIEntityBlock<T> {

    public BlockTransferNode() {
        super(BlockBehaviour.Properties.of(Material.STONE));
    }

    /**
     * ルート計算
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean p_60514_) {
        if (level.getBlockEntity(pos) instanceof TileBaseTransferNode node) {
            var prevState = node.pipeState;
            var currentState = PipeInstance.recalcState(level, pos);
            if (prevState != currentState)
                node.setPipeStateAndUpdate(currentState);
        }

        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, p_60514_);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult p_60508_) {
        if (level.getBlockEntity(pos) instanceof TileBaseTransferNode node) {
            if (TPUtils.usingWrench(player, hand) && node.shouldRenderPipe()) {
                if (!level.isClientSide)
                    node.setPipeStateAndUpdate(PipeInstance.cycleAndCalcState(level, pos/*, player.isShiftKeyDown() shift右クリックはブロックからは検知できない*/));
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            return openMenu(level, pos, player, (T) node);
        }

        return InteractionResult.PASS;
    }

    /**
     * 面を持つノードの初期化と当たり判定
     */
    public abstract static class FacingNode<T extends TileBaseTransferNode> extends BlockTransferNode<T> {

        public static DirectionProperty FACING = BlockStateProperties.FACING;

        public FacingNode() {
            super();
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
        }

        public static final Map<Direction, VoxelShape> ROTATED_NODES = MCUtils.getRotatedShapes(Stream.of(
                Block.box(1, 1, 0, 15, 15, 1),
                Block.box(3, 3, 1, 13, 13, 4),
                Block.box(5, 5, 4, 11, 11, 6)
        ).reduce(Shapes::or).get());

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext context) {
            var shape = new AtomicReference<>(ROTATED_NODES.get(state.getValue(FACING)));
            blockGetter.getBlockEntity(pos, getType()).ifPresent(be -> {
                if (be.shouldRenderPipe())
                    shape.set(Shapes.or(shape.get(), TransferPipe.getShape(be.pipeState)));
            });

            return shape.get();
        }

        public Direction facing(Level level, BlockPos pos) {
            return facing(level.getBlockState(pos));
        }

        public Direction facing(BlockState state) {
            return state.getValue(FACING);
        }
    }

    /**
     * 各ノード
     */
    public static class Item extends FacingNode<TileTransferNodeItem> {

        @Override
        public TPBlocks.RegistryGUIEntityBlock<TileTransferNodeItem> registryWithGUI() {
            return TPBlocks.TRANSFER_NODE_ITEM;
        }

        @Override
        public BaseBlockMenu menu(TileTransferNodeItem tile, int id, Inventory inv) {
            return new MenuTransferNode.Item(tile.itemSlot, tile.upgrades, tile.searchData, id, inv);
        }
    }

    public static class Liquid extends FacingNode<TileTransferNodeLiquid> {

        @Override
        public TPBlocks.RegistryGUIEntityBlock<TileTransferNodeLiquid> registryWithGUI() {
            return TPBlocks.TRANSFER_NODE_LIQUID;
        }

        @Override
        public BaseBlockMenu menu(TileTransferNodeLiquid tile, int id, Inventory inv) {
            return new MenuTransferNode.Liquid(tile.dummyLiquidItem, tile.upgrades, tile.searchData, id, inv);
        }
    }

    public static class Energy extends BlockTransferNode<TileTransferNodeEnergy> {

        @Override
        public TPBlocks.RegistryGUIEntityBlock<TileTransferNodeEnergy> registryWithGUI() {
            return TPBlocks.TRANSFER_NODE_ENERGY;
        }

        @Override
        public BaseBlockMenu menu(TileTransferNodeEnergy tile, int id, Inventory inv) {
            return new MenuTransferNode.Energy(tile.energyData, tile.upgrades, tile.searchData, id, inv);
        }

        public static final VoxelShape ENERGY_NODE = Stream.of(
                Block.box(5, 3, 5, 11, 13, 11),
                Block.box(5, 5, 3, 11, 11, 13),
                Block.box(3, 5, 5, 13, 11, 11),
                Block.box(4, 4, 4, 12, 12, 12)
        ).reduce(Shapes::or).get();

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext context) {
            var shape = new AtomicReference<>(ENERGY_NODE);
            blockGetter.getBlockEntity(pos, getType()).ifPresent(be -> {
                if (be.shouldRenderPipe())
                    shape.set(Shapes.or(shape.get(), TransferPipe.getShape(be.pipeState)));
            });

            return shape.get();
        }
    }
}
