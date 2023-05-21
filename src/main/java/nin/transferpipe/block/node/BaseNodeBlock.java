package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import nin.transferpipe.block.pipe.Pipe;
import nin.transferpipe.util.minecraft.GUIEntityBlock;
import nin.transferpipe.util.minecraft.LightingBlock;
import nin.transferpipe.util.minecraft.MCUtils;
import nin.transferpipe.util.transferpipe.PipeInstance;
import nin.transferpipe.util.transferpipe.TPUtils;

import java.util.Map;
import java.util.stream.Stream;

/**
 * ノードのブロック部分。中のパイプの更新。
 */
public abstract class BaseNodeBlock<T extends BaseTileNode<?>> extends LightingBlock implements GUIEntityBlock<T> {

    public BaseNodeBlock() {
        super(BlockBehaviour.Properties.of(Material.STONE).dynamicShape());
    }

    /**
     * ルート計算
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean p_60514_) {
        if (level.getBlockEntity(pos) instanceof BaseTileNode<?> node) {
            var prevState = node.pipeState;
            var currentState = PipeInstance.recalcState(level, pos);
            if (prevState != currentState)
                node.setPipeStateAndUpdate(currentState);
        }

        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, p_60514_);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var node = getOuterTile(level, pos);

        //レンチ
        if (TPUtils.usingWrench(player, hand)) {
            if (!level.isClientSide)
                node.setPipeStateAndUpdate(PipeInstance.cycleAndCalcState(level, pos/*, player.isShiftKeyDown() アイテム持ちながらのshift右クリックはmixinしないと検知できない*/));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        //パイプGUIを開く
        if (!MCUtils.contains(nodeShape(state), MCUtils.relativeLocation(hit, pos))
                && node.hasHoldingTileMenu())
            return node.openHoldingTileMenu(player);

        //ノードGUIを開く
        return node.openMenu(player);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext context) {
        return Shapes.or(nodeShape(state), blockGetter.getBlockEntity(pos, getType())
                .filter(BaseTileNode::shouldRenderPipe)
                .map(node -> Pipe.getShape(node.pipeState))
                .orElse(Shapes.empty()));
    }

    public abstract VoxelShape nodeShape(BlockState state);

    /**
     * 面を持つノードの初期化と当たり判定
     */
    public abstract static class Facing<T extends BaseTileNode<?>> extends BaseNodeBlock<T> {

        public static DirectionProperty FACING = BlockStateProperties.FACING;

        public Facing() {
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
        public VoxelShape nodeShape(BlockState state) {
            return ROTATED_NODES.get(state.getValue(FACING));
        }

        public Direction facing(Level level, BlockPos pos) {
            return facing(level.getBlockState(pos));
        }

        public Direction facing(BlockState state) {
            return state.getValue(FACING);
        }
    }

    /**
     * エネルギーノード型の当たり判定
     */
    public abstract static class Energy<T extends BaseTileNode<?>> extends BaseNodeBlock<T> {

        public static final VoxelShape ENERGY_NODE = Stream.of(
                Block.box(5, 3, 5, 11, 13, 11),
                Block.box(5, 5, 3, 11, 11, 13),
                Block.box(3, 5, 5, 13, 11, 11),
                Block.box(4, 4, 4, 12, 12, 12)
        ).reduce(Shapes::or).get();

        @Override
        public VoxelShape nodeShape(BlockState state) {
            return ENERGY_NODE;
        }
    }
}
