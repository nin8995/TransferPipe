package nin.transferpipe.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
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
import nin.transferpipe.block.property.ConnectionStates;
import nin.transferpipe.block.property.FlowStates;
import nin.transferpipe.block.property.TPProperties;
import nin.transferpipe.util.ShapeUtil;
import nin.transferpipe.util.TPUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class TransferPipeBlock extends LightingBlock {

    public static final EnumProperty<FlowStates> FLOW = TPProperties.FLOW;
    public static final Map<Direction, EnumProperty<ConnectionStates>> CONNECTIONS = TPProperties.CONNECTIONS;

    public TransferPipeBlock() {
        super(BlockBehaviour.Properties.of(Material.STONE));
        var s = stateDefinition.any();
        for (Direction d : Direction.stream().toList())
            s = s.setValue(CONNECTIONS.get(d), ConnectionStates.NONE);
        s = s.setValue(FLOW, FlowStates.ALL);
        registerDefaultState(s);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTIONS.values().forEach(builder::add);
        builder.add(FLOW);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext bpc) {
        return TPUtil.getPipeState(bpc.getLevel(), bpc.getClickedPos());
    }

    @Override
    public void neighborChanged(BlockState bs, Level l, BlockPos bp, Block p_60512_, BlockPos p_60513_, boolean p_60514_) {
        var os = l.getBlockState(bp);
        var ns = TPUtil.getPipeState(l, bp);
        if (ns != os)
            l.setBlockAndUpdate(bp, ns);
        super.neighborChanged(bs, l, bp, p_60512_, p_60513_, p_60514_);
    }

    @Override
    public InteractionResult use(BlockState bs, Level l, BlockPos bp, Player p, InteractionHand h, BlockHitResult p_60508_) {
        if (p.getItemInHand(h).getItem() == Items.STICK) {
            l.setBlockAndUpdate(bp, TPUtil.cycleFlow(l, bp, bs));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public static final VoxelShape CENTER = Block.box(6, 6, 6, 10, 10, 10);
    public static final Map<Direction, VoxelShape> LIMBS = ShapeUtil.getRotatedShapes(Block.box(6, 6, 0, 10, 10, 6));
    public static final Map<Direction, VoxelShape> JOINTS = ShapeUtil.getRotatedShapes(Block.box(5, 5, -0.001, 11, 11, 2.999));
    public static final Map<ConnectionCacheKey, VoxelShape> shapeCache = getPossibleConnectionStates().stream().collect(Collectors.toMap(UnaryOperator.identity(), TransferPipeBlock::calculateShape));

    public static List<ConnectionCacheKey> getPossibleConnectionStates() {
        var states = new ArrayList<ConnectionCacheKey>();
        for (ConnectionStates down : ConnectionStates.values())
            for (ConnectionStates up : ConnectionStates.values())
                for (ConnectionStates north : ConnectionStates.values())
                    for (ConnectionStates east : ConnectionStates.values())
                        for (ConnectionStates south : ConnectionStates.values())
                            for (ConnectionStates west : ConnectionStates.values())
                                states.add(new ConnectionCacheKey(down, up, north, south, east, west));
        return states;
    }

    private static VoxelShape calculateShape(ConnectionCacheKey cacheKey) {
        var shape = CENTER;
        for (Direction d : Direction.values()) {
            var limb = LIMBS.get(d);
            var joint = JOINTS.get(d);
            switch (cacheKey.getState(d)) {
                case PIPE -> shape = Shapes.or(shape, limb);
                case MACHINE -> shape = Shapes.or(shape, limb, joint);
            }
        }
        return shape;
    }

    @Override
    public VoxelShape getShape(BlockState bs, BlockGetter p_60556_, BlockPos p_60557_, CollisionContext p_60558_) {
        return getShape(bs);
    }

    public static VoxelShape getShape(BlockState bs) {
        var down = bs.getValue(CONNECTIONS.get(Direction.DOWN));
        var up = bs.getValue(CONNECTIONS.get(Direction.UP));
        var north = bs.getValue(CONNECTIONS.get(Direction.NORTH));
        var south = bs.getValue(CONNECTIONS.get(Direction.SOUTH));
        var east = bs.getValue(CONNECTIONS.get(Direction.EAST));
        var west = bs.getValue(CONNECTIONS.get(Direction.WEST));

        return shapeCache.get(new ConnectionCacheKey(down, up, north, south, east, west));
    }


    private record ConnectionCacheKey(ConnectionStates down, ConnectionStates up, ConnectionStates north,
                                      ConnectionStates south, ConnectionStates east,
                                      ConnectionStates west) {
        private ConnectionStates getState(Direction direction) {
            return switch (direction) {
                case UP -> up;
                case DOWN -> down;
                case NORTH -> north;
                case SOUTH -> south;
                case EAST -> east;
                case WEST -> west;
            };
        }
    }
}
