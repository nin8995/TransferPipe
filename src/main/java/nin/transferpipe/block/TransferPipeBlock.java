package nin.transferpipe.block;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nin.transferpipe.TransferPipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TransferPipeBlock extends Block {

    public TransferPipeBlock() {
        super(BlockBehaviour.Properties.of(Material.STONE));
        var s = stateDefinition.any();
        for (Direction d : Direction.stream().toList())
            s = s.setValue(CONNECTIONS.get(d), ConnectionStates.NONE);
        s = s.setValue(FLOW, FlowStates.ALL);
        registerDefaultState(s);
    }

    @Override
    public InteractionResult use(BlockState bs, Level l, BlockPos pos, Player p, InteractionHand h, BlockHitResult p_60508_) {
        if (p.getItemInHand(h).getItem() == Items.STICK)
            l.setBlockAndUpdate(pos, getState(l, pos, bs.setValue(FLOW, FlowStates.getNext(l, pos, bs))));

        return InteractionResult.SUCCESS;
    }

    @Override
    public float getShadeBrightness(BlockState p_48731_, BlockGetter p_48732_, BlockPos p_48733_) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState p_48740_, BlockGetter p_48741_, BlockPos p_48742_) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext bpc) {
        return getState(bpc.getLevel(), bpc.getClickedPos());
    }

    @Override
    public void neighborChanged(BlockState bs, Level l, BlockPos bp, Block p_60512_, BlockPos p_60513_, boolean p_60514_) {
        super.neighborChanged(bs, l, bp, p_60512_, p_60513_, p_60514_);
        var os = l.getBlockState(bp);
        var ns = getState(l, bp);
        if (ns != os)
            l.setBlockAndUpdate(bp, ns);
    }

    public BlockState getState(Level l, BlockPos p) {
        var s = l.getBlockState(p);
        return getState(l, p, s.hasProperty(FLOW) ? s : defaultBlockState());
    }

    public BlockState getState(Level l, BlockPos p, BlockState s) {
        for (Direction d : Direction.stream().toList())
            s = s.setValue(CONNECTIONS.get(d), isPipe(l, p, d) && canGo(l, p, d) ? ConnectionStates.PIPE : canConnect(l, p, d) && s.getValue(FLOW) != FlowStates.IGNORE ? ConnectionStates.MACHINE : ConnectionStates.NONE);

        return s;
    }

    public static boolean isPipe(LevelAccessor l, BlockPos p, Direction d) {
        return l.getBlockState(p.relative(d)).getBlock() == TransferPipe.TRANSFER_PIPE.get();
    }

    public static boolean canGo(LevelAccessor l, BlockPos p, Direction d) {
        var me = l.getBlockState(p);
        var you = l.getBlockState(p.relative(d));
        return !(isOneWay(me, d) && isOneWay(you, d.getOpposite()));
    }

    public static boolean isOneWay(BlockState bs, Direction d) {
        if (!bs.hasProperty(FLOW))//ここには何故か初見さんきちゃう
            return false;

        var s = bs.getValue(FLOW);
        return !(s == FlowStates.fromDirection(d) || s == FlowStates.ALL || s == FlowStates.IGNORE);
    }

    public boolean canConnect(LevelAccessor l, BlockPos p, Direction d) {
        return l.getBlockEntity(p.relative(d)) instanceof Container;
    }

    public static final Map<Direction, EnumProperty<ConnectionStates>> CONNECTIONS = Direction.stream().collect(Collectors
            .toMap(UnaryOperator.identity(), d -> EnumProperty.create(d.getName(), ConnectionStates.class)));
    public static final EnumProperty<FlowStates> FLOW = EnumProperty.create("flow", FlowStates.class);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTIONS.values().forEach(builder::add);
        builder.add(FLOW);
    }

    private static final VoxelShape CENTER = Block.box(6, 6, 6, 10, 10, 10);
    private static final Map<Direction, VoxelShape> LIMBS = getRotatedShapes(new Vec3(6, 6, 0), new Vec3(10, 10, 6));
    private static final Map<Direction, VoxelShape> JOINTS = getRotatedShapes(new Vec3(5, 5, -0.001), new Vec3(11, 11, 2.999));
    // private final Map<BlockState, VoxelShape> shapes = this.getShapeForEachState(TransferPipeBlock::calculateShape);
    private final Map<ConnectionCacheKey, VoxelShape> shapeCache = getPossibleConnectionStates().stream().collect(Collectors.toMap(UnaryOperator.identity(), TransferPipeBlock::calculateShape));

    /*public void putAllStates(ArrayList<ConnectionCacheKey> bss, ArrayList<ConnectionStates> css, ArrayList<EnumProperty<ConnectionStates>> eps) {
        for (ConnectionStates cs : ConnectionStates.values()) {
            if(css == null)
                css = new ArrayList<>();
            css.add(cs);
            if (eps.size() == 1) {
                bss.add(new ConnectionCacheKey(css.get(0), css.get(1), css.get(2), css.get(3), css.get(4), css.get(5)));
            } else {
                var neps =(ArrayList<EnumProperty<ConnectionStates>>) eps.clone();
                neps.remove(neps.size()-1);
                putAllStates(bss, css, neps);
            }
        }
    }*/

    public List<ConnectionCacheKey> getPossibleConnectionStates() {
        /*var bss = new ArrayList<ConnectionCacheKey>();
        putAllStates(bss, null,new ArrayList<>(CONNECTIONS.values())) ;
        return bss;*/

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

    @Override
    public VoxelShape getShape(BlockState bs, BlockGetter p_60556_, BlockPos p_60557_, CollisionContext p_60558_) {
        var down = bs.getValue(CONNECTIONS.get(Direction.DOWN));
        var up = bs.getValue(CONNECTIONS.get(Direction.UP));
        var north = bs.getValue(CONNECTIONS.get(Direction.NORTH));
        var south = bs.getValue(CONNECTIONS.get(Direction.SOUTH));
        var east = bs.getValue(CONNECTIONS.get(Direction.EAST));
        var west = bs.getValue(CONNECTIONS.get(Direction.WEST));

        return shapeCache.get(new ConnectionCacheKey(down, up, north, south, east, west));
    }

    private static VoxelShape calculateShape(ConnectionCacheKey cacheKey) {
        var shape = CENTER;
        for (Direction d : CONNECTIONS.keySet()) {
            var limb = LIMBS.get(d);
            var joint = JOINTS.get(d);
            switch (cacheKey.getState(d)) {
                case PIPE -> shape = Shapes.or(shape, limb);
                case MACHINE -> shape = Shapes.or(shape, limb, joint);
            }
        }
        return shape;
    }
  /*  private static VoxelShape calculateShape(BlockState state) {
        var shape = CENTER;
        for (Direction d : CONNECTIONS.keySet()) {
            var limb = LIMBS.get(d);
            var joint = JOINTS.get(d);
            switch (state.getValue(CONNECTIONS.get(d))) {
                case PIPE -> shape = Shapes.or(shape, limb);
                case MACHINE -> shape = Shapes.or(shape, limb, joint);
            }
        }
        return shape;
    }*/

    public static Vec3 rotate(Vec3 v, Direction d) {
        var rightAngle = (float) (Math.PI / 2);
        var toRotate = v.add(-8, -8, -8);
        var rotated = switch (d) {
            case DOWN -> toRotate.xRot(rightAngle);
            case UP -> toRotate.xRot(-rightAngle);
            case NORTH -> toRotate;
            case SOUTH -> toRotate.yRot(rightAngle * 2);
            case WEST -> toRotate.yRot(rightAngle);
            case EAST -> toRotate.yRot(-rightAngle);
        };
        return rotated.add(8, 8, 8);
    }

    public static Map<Direction, VoxelShape> getRotatedShapes(Vec3 start, Vec3 end) {
        return Direction.stream().collect(Collectors.toMap(UnaryOperator.identity(), d -> {
            var sr = rotate(start, d);
            var er = rotate(end, d);
            return Block.box(Math.min(sr.x, er.x), Math.min(sr.y, er.y), Math.min(sr.z, er.z), Math.max(sr.x, er.x), Math.max(sr.y, er.y), Math.max(sr.z, er.z));
        }));
    }


    private record ConnectionCacheKey(ConnectionStates down, ConnectionStates up, ConnectionStates north,
                                      ConnectionStates south, ConnectionStates east,
                                      ConnectionStates west) {
        private ConnectionStates getState(Direction direction) {
            switch (direction) {
                case UP -> {
                    return up;
                }
                case DOWN -> {
                    return down;
                }
                case NORTH -> {
                    return north;
                }
                case SOUTH -> {
                    return south;
                }
                case EAST -> {
                    return east;
                }
                case WEST -> {
                    return west;
                }
            }
            return north;
        }
    }

    public enum ConnectionStates implements StringRepresentable {
        NONE,
        PIPE,
        MACHINE;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase();
        }
    }

    public enum FlowStates implements StringRepresentable {
        ALL,
        UP,
        DOWN,
        NORTH,
        EAST,
        SOUTH,
        WEST,
        NONE,
        IGNORE;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase();
        }

        public static FlowStates fromDirection(Direction d) {
            return Arrays.stream(FlowStates.values()).filter(f -> f.name().equals(d.name())).findFirst().get();
        }

        public static Stream<FlowStates> directions() {
            return Direction.stream().map(FlowStates::fromDirection);
        }

        public static FlowStates[] statesWithout(FlowStates s) {
            return Arrays.stream(FlowStates.values()).filter(f -> f != s).toArray(FlowStates[]::new);
        }

        public FlowStates next() {
            return Arrays.stream(FlowStates.values()).filter(fs -> fs.ordinal() == this.ordinal() + 1).findFirst().orElse(ALL);
        }

        public static FlowStates getNext(Level l, BlockPos p, BlockState bs) {
            var validStates = new java.util.ArrayList<>(Arrays.stream(FlowStates.values()).toList());
            var nonValidStates = new java.util.ArrayList<>(Direction.stream()
                    .filter(d -> !isPipe(l, p, d)).map(FlowStates::fromDirection).toList());
            if (nonValidStates.size() == 6) {
                nonValidStates.add(NONE);
            } else if (nonValidStates.size() == 5) {
                nonValidStates.add(ALL);
            }
            validStates.removeAll(nonValidStates);
            var searching = bs.getValue(FLOW).next();
            while (true) {
                if (validStates.contains(searching))
                    return searching;
                searching = searching.next();
            }
        }
    }
}
