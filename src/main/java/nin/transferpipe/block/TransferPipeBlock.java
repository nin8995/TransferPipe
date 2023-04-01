package nin.transferpipe.block;

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
import nin.transferpipe.block.state.Connection;
import nin.transferpipe.block.state.Flow;
import nin.transferpipe.util.PipeUtils;
import nin.transferpipe.util.TPUtils;

import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class TransferPipeBlock extends LightingBlock {

    /**
     * 基本情報
     */

    public static final EnumProperty<Flow> FLOW = EnumProperty.create("flow", Flow.class);
    public static final Map<Direction, EnumProperty<Connection>> CONNECTIONS = Direction.stream().collect(Collectors.toMap(
            UnaryOperator.identity(),
            d -> EnumProperty.create(d.getName(), Connection.class)));

    public TransferPipeBlock() {
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
        return PipeUtils.calcInitialState(bpc.getLevel(), bpc.getClickedPos());
    }

    /**
     * 当たり判定
     */

    public static final VoxelShape CENTER = Block.box(6, 6, 6, 10, 10, 10);
    public static final Map<Direction, VoxelShape> LIMBS = TPUtils.getRotatedShapes(Block.box(6, 6, 0, 10, 10, 6));
    public static final Map<Direction, VoxelShape> JOINTS = TPUtils.getRotatedShapes(Block.box(5, 5, -0.001, 11, 11, 2.999));
    public static Map<BlockState, VoxelShape> shapeCache = null;

    @Override
    public VoxelShape getShape(BlockState bs, BlockGetter p_60556_, BlockPos p_60557_, CollisionContext p_60558_) {
        return getShape(bs);
    }

    public static VoxelShape getShape(BlockState bs) {
        if (shapeCache == null)//RegistryObject.getを使うため、staticのとこには置けない。でもここならいいね
            shapeCache = TPBlocks.TRANSFER_PIPE.get().getStateDefinition().getPossibleStates().stream().collect(Collectors.toMap(
                    UnaryOperator.identity(),
                    TransferPipeBlock::calculateShape));

        return shapeCache.get(bs);
    }

    public static VoxelShape calculateShape(BlockState state) {
        var shape = CENTER;
        for (Direction d : Direction.values()) {
            var limb = LIMBS.get(d);
            var joint = JOINTS.get(d);
            switch (state.getValue(CONNECTIONS.get(d))) {
                case PIPE -> shape = Shapes.or(shape, limb);
                case MACHINE -> shape = Shapes.or(shape, limb, joint);
            }
        }
        return shape;
    }

    /**
     * 機能
     */

    @Override
    public void neighborChanged(BlockState bs, Level l, BlockPos bp, Block p_60512_, BlockPos p_60513_, boolean p_60514_) {
        var prevState = l.getBlockState(bp);
        var currentState = PipeUtils.recalcConnections(l, bp);
        if (currentState != prevState)
            l.setBlockAndUpdate(bp, currentState);

        super.neighborChanged(bs, l, bp, p_60512_, p_60513_, p_60514_);
    }

    @Override
    public InteractionResult use(BlockState p_60503_, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult p_60508_) {
        if (PipeUtils.usingWrench(player, hand)) {
            level.setBlockAndUpdate(pos, PipeUtils.cycleFlowAndRecalc(level, pos));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
