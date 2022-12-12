package nin.transferpipe.block;

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
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import nin.transferpipe.TransferPipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TransferPipeBlock extends PipeBlock {

    public TransferPipeBlock() {
        super(1 / 8F, BlockBehaviour.Properties.of(Material.STONE));
        var s = stateDefinition.any();
        for (Direction d : Direction.stream().toList())
            s = s.setValue(CONNECTIONS.get(d), ConnectionStates.NONE);
        s = s.setValue(FLOW, FlowStates.ALL);
        registerDefaultState(s);
    }

    @Override
    public InteractionResult use(BlockState bs, Level l, BlockPos pos, Player p, InteractionHand h, BlockHitResult p_60508_) {
        if (p.getItemInHand(h).getItem() == Items.STICK)
            l.setBlockAndUpdate(pos, bs.setValue(FLOW, FlowStates.getNext(l, pos, bs)));

        return super.use(bs, l, pos, p, h, p_60508_);
    }

    public float getShadeBrightness(BlockState p_48731_, BlockGetter p_48732_, BlockPos p_48733_) {
        return 1.0F;
    }

    public boolean propagatesSkylightDown(BlockState p_48740_, BlockGetter p_48741_, BlockPos p_48742_) {
        return true;
    }

    @Nullable
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
        var s = defaultBlockState();
        for (Direction d : Direction.stream().toList())
            s = s.setValue(CONNECTIONS.get(d), isPipe(l, p, d) && canGo(l,p,d) ? ConnectionStates.PIPE : canConnect(l, p, d) ? ConnectionStates.MACHINE : ConnectionStates.NONE);
        if (l.getBlockState(p).hasProperty(FLOW))
            s = s.setValue(FLOW, l.getBlockState(p).getValue(FLOW));//ここには初見さんも来る、BlockStateはあるがPropertyが定まってないヤツ
        return s;
    }

    public static boolean isPipe(LevelAccessor l, BlockPos p, Direction d) {
        return l.getBlockState(p.relative(d)).getBlock() == TransferPipe.TRANSFER_PIPE.get();
    }

    public static boolean canGo(LevelAccessor l, BlockPos p, Direction d){
        var me = l.getBlockState(p);
        var you = l.getBlockState(p.relative(d));
        return !(isOneWay(me, d) && isOneWay(you, d.getOpposite()));
    }

    public static boolean isOneWay(BlockState bs, Direction d){
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

    @Override
    protected int getAABBIndex(BlockState bs) {
        int i = 0;
        var ds = Direction.values();
        for (int j = 0; j < ds.length; ++j)
            if (bs.getValue(CONNECTIONS.get(ds[j])) != ConnectionStates.NONE
                    && !(bs.getValue(CONNECTIONS.get(ds[j])) == ConnectionStates.MACHINE && bs.getValue(FLOW) == FlowStates.IGNORE))
                i |= 1 << j;

        return i;
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

        public static Stream<FlowStates> directions(){
            return Direction.stream().map(FlowStates::fromDirection);
        }

        public static FlowStates[] statesWithout(FlowStates s){
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
            } else if(nonValidStates.size() == 5){
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
