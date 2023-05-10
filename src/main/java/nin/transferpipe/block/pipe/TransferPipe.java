package nin.transferpipe.block.pipe;

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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nin.transferpipe.util.forge.ForgeUtils;
import nin.transferpipe.util.minecraft.LightingBlock;
import nin.transferpipe.util.minecraft.MCUtils;
import nin.transferpipe.util.transferpipe.PipeInstance;
import nin.transferpipe.util.transferpipe.Searcher;
import nin.transferpipe.util.transferpipe.TPUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * BlockStateでノードの検索ルートを決めるブロック
 */
public class TransferPipe extends LightingBlock {

    /**
     * 初期化
     */
    public static final EnumProperty<Flow> FLOW = EnumProperty.create("flow", Flow.class);
    public static final Map<Direction, EnumProperty<Connection>> CONNECTIONS = Direction.stream().collect(Collectors.toMap(
            UnaryOperator.identity(),
            d -> EnumProperty.create(d.getName(), Connection.class)));

    public TransferPipe() {
        super(BlockBehaviour.Properties.of(Material.STONE));

        var defaultState = stateDefinition.any();
        defaultState = defaultState.setValue(FLOW, Flow.ALL);
        defaultState = Connection.map(defaultState, d -> Connection.NONE);
        registerDefaultState(defaultState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FLOW);
        CONNECTIONS.values().forEach(builder::add);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext bpc) {
        return PipeInstance.precalcState(bpc.getLevel(), bpc.getClickedPos(), defaultBlockState());
    }

    /**
     * 当たり判定
     */
    public static final VoxelShape CENTER = Block.box(6, 6, 6, 10, 10, 10);
    public static final Map<Direction, VoxelShape> LIMBS = MCUtils.getRotatedShapes(Block.box(6, 6, 0, 10, 10, 6));
    public static final Map<Direction, VoxelShape> JOINTS = MCUtils.getRotatedShapes(Block.box(5, 5, -0.001, 11, 11, 2.999));
    public static Map<List<Connection>, VoxelShape> shapeCache = calcPossibleConnectionStates().stream().collect(Collectors.toMap(
            UnaryOperator.identity(), TransferPipe::calcShape));

    @Override
    public VoxelShape getShape(BlockState bs, BlockGetter p_60556_, BlockPos p_60557_, CollisionContext p_60558_) {
        return getShape(bs);
    }

    public static VoxelShape getShape(BlockState state) {
        return shapeCache.get(Direction.stream().map(d -> state.getValue(CONNECTIONS.get(d))).toList());
    }

    public static List<List<Connection>> calcPossibleConnectionStates() {
        var connectionStates = new ArrayList<List<Connection>>();
        Connection.forEach(c0 -> Connection.forEach(c1 -> Connection.forEach(c2 -> Connection.forEach(c3 -> Connection.forEach(c4 -> Connection.forEach(c5 ->
                connectionStates.add(List.of(c0, c1, c2, c3, c4, c5))))))));
        return connectionStates;
    }

    public static VoxelShape calcShape(List<Connection> connections) {
        var shape = CENTER;
        for (Direction d : Direction.values()) {
            var limb = LIMBS.get(d);
            var joint = JOINTS.get(d);
            switch (connections.get(d.ordinal())) {
                case PIPE -> shape = Shapes.or(shape, limb);
                case MACHINE -> shape = Shapes.or(shape, limb, joint);
            }
        }
        return shape;
    }

    /**
     * ルート計算
     */
    @Override
    public void neighborChanged(BlockState bs, Level l, BlockPos pos, Block p_60512_, BlockPos p_60513_, boolean p_60514_) {
        var currentState = l.getBlockState(pos);
        var newState = PipeInstance.recalcState(l, pos);
        if (newState != currentState)
            l.setBlockAndUpdate(pos, newState);

        super.neighborChanged(bs, l, pos, p_60512_, p_60513_, p_60514_);
    }

    @Override
    public InteractionResult use(BlockState p_60503_, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult p_60508_) {
        if (TPUtils.usingWrench(player, hand)) {
            if (!level.isClientSide)
                level.setBlockAndUpdate(pos, PipeInstance.cycleAndCalcState(level, pos));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    /**
     * 計算方法変更の余地
     */
    public boolean isWorkPlace(Level level, BlockPos pos, @Nullable Direction dir) {
        return ForgeUtils.hasItemHandler(level, pos, dir)
                || ForgeUtils.hasFluidHandler(level, pos, dir)
                || ForgeUtils.hasEnergyStorage(level, pos, dir);
    }

    public boolean isValidPipe(TransferPipe pipe) {
        return true;
    }

    /**
     * 検索方法変更の余地
     */
    public boolean isValidSearcher(Searcher searcher) {
        return true;
    }
}
