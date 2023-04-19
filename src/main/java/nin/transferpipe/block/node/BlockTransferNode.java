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
import nin.transferpipe.block.BaseBlockMenu;
import nin.transferpipe.block.LightingBlock;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.TickingGUIBlock;
import nin.transferpipe.block.pipe.TransferPipe;
import nin.transferpipe.util.PipeUtils;
import nin.transferpipe.util.TPUtils;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//搬送する種類に依らない、「ノード」のブロックとしての機能
public abstract class BlockTransferNode<T extends TileBaseTransferNode> extends LightingBlock implements TickingGUIBlock<T> {

    /**
     * 基本情報
     */

    public BlockTransferNode() {
        super(BlockBehaviour.Properties.of(Material.STONE));
    }

    //energy nodeかそうじゃないか
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

        public static final Map<Direction, VoxelShape> ROTATED_NODES = TPUtils.getRotatedShapes(Stream.of(
                Block.box(1, 1, 0, 15, 15, 1),
                Block.box(3, 3, 1, 13, 13, 4),
                Block.box(5, 5, 4, 11, 11, 6)
        ).reduce(Shapes::or).get());
        public final Map<BlockState, VoxelShape> shapeCache = this.stateDefinition.getPossibleStates().stream().collect(Collectors.toMap(
                UnaryOperator.identity(),
                bs -> ROTATED_NODES.get(bs.getValue(FACING))));

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext context) {
            var shape = new AtomicReference<>(shapeCache.get(state));
            blockGetter.getBlockEntity(pos, getType()).ifPresent(be -> {
                if (be.shouldRenderPipe())
                    shape.set(Shapes.or(shape.get(), TransferPipe.getShape(be.getPipeState())));
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
     * 一般の機能
     */

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean p_60514_) {
        if (level.getBlockEntity(pos) instanceof TileBaseTransferNode be) {
            var prevState = be.getPipeState();
            var currentState = PipeUtils.recalcConnections(level, pos);
            if (prevState != currentState)
                be.setPipeStateAndUpdate(currentState);
        }

        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, p_60514_);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult p_60508_) {
        var be = (T) level.getBlockEntity(pos);
        if (PipeUtils.usingWrench(player, hand) && be.shouldRenderPipe()) {
            if (!level.isClientSide)
                be.setPipeStateAndUpdate(PipeUtils.cycleFlowAndRecalc(level, pos/*, player.isShiftKeyDown() shift右クリックはブロックからは検知できない*/));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return openMenu(level, pos, player);
    }

    /**
     * 搬送種毎の情報
     */

    public static class Item extends FacingNode<TileTransferNodeItem> {

        @Override
        public TPBlocks.RegistryGUIEntityBlock<TileTransferNodeItem> registryWithGUI() {
            return TPBlocks.TRANSFER_NODE_ITEM;
        }

        @Override
        public BaseBlockMenu menu(TileTransferNodeItem be, int id, Inventory inv) {
            return new MenuTransferNode.Item(be.getItemSlotHandler(), be.getUpgrades(), be.searchData, id, inv);
        }
    }

    public static class Liquid extends FacingNode<TileTransferNodeLiquid> {

        @Override
        public TPBlocks.RegistryGUIEntityBlock<TileTransferNodeLiquid> registryWithGUI() {
            return TPBlocks.TRANSFER_NODE_LIQUID;
        }

        @Override
        public BaseBlockMenu menu(TileTransferNodeLiquid be, int id, Inventory inv) {
            return new MenuTransferNode.Liquid(be.dummyLiquidItem, be.getUpgrades(), be.searchData, id, inv);
        }
    }

    public static class Energy extends BlockTransferNode<TileTransferNodeEnergy> {

        @Override
        public TPBlocks.RegistryGUIEntityBlock<TileTransferNodeEnergy> registryWithGUI() {
            return TPBlocks.TRANSFER_NODE_ENERGY;
        }

        @Override
        public BaseBlockMenu menu(TileTransferNodeEnergy be, int id, Inventory inv) {
            return new MenuTransferNode.Energy(be.energyData, be.getUpgrades(), be.searchData, id, inv);
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
                    shape.set(Shapes.or(shape.get(), TransferPipe.getShape(be.getPipeState())));
            });

            return shape.get();
        }
    }
}
