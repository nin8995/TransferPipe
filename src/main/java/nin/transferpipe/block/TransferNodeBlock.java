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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import nin.transferpipe.TransferPipe;
import nin.transferpipe.util.ShapeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TransferNodeBlock extends Block implements EntityBlock {

    public static DirectionProperty FACING = BlockStateProperties.FACING;

    public TransferNodeBlock() {
        super(BlockBehaviour.Properties.of(Material.STONE));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
        return defaultBlockState().setValue(FACING, bpc.getClickedFace().getOpposite());
    }

    @Override
    public void neighborChanged(BlockState bs, Level l, BlockPos bp, Block p_60512_, BlockPos p_60513_, boolean p_60514_) {
        if (l.getBlockEntity(bp) instanceof TransferNodeBlockEntity be)
            be.setPipeState(getPipeState(l, bp));
        super.neighborChanged(bs, l, bp, p_60512_, p_60513_, p_60514_);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState p_60457_) {
        return true;
    }

    @Override
    public InteractionResult use(BlockState bs, Level l, BlockPos bp, Player p, InteractionHand h, BlockHitResult p_60508_) {
        if (p.getItemInHand(h).getItem() == Items.STICK && l.getBlockEntity(bp) instanceof TransferNodeBlockEntity be) {
            be.setPipeState(setConnections(l, bp, be.getPipeState().setValue(TransferPipeBlock.FLOW, TransferPipeBlock.FlowStates.getNextWithout(bs.getValue(FACING), l, bp, be.getPipeState()))));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public static BlockState getPipeState(Level l, BlockPos bp) {
        var bs = l.getBlockEntity(bp) instanceof TransferNodeBlockEntity be ? be.getPipeState()
                : TransferPipe.TRANSFER_PIPE.get().defaultBlockState();
        return setConnections(l, bp, bs);
    }

    public static BlockState setConnections(Level l, BlockPos bp, BlockState bs) {
        return TransferPipeBlock.setConnections(l, bp, bs)
                .setValue(TransferPipeBlock.CONNECTIONS.get(l.getBlockState(bp).getValue(FACING)), TransferPipeBlock.ConnectionStates.NONE);
    }

    public static final Map<Direction, VoxelShape> TRANSFER_NODE_ITEM = ShapeUtil.getRotatedShapes(Stream.of(
            Block.box(1, 1, 0, 15, 15, 1),
            Block.box(3, 3, 1, 13, 13, 4),
            Block.box(5, 5, 4, 11, 11, 6)
    ).reduce(Shapes::or).get());
    public final Map<BlockState, VoxelShape> shapeCache = this.stateDefinition.getPossibleStates().stream().collect(Collectors
            .toMap(UnaryOperator.identity(), bs -> TRANSFER_NODE_ITEM.get(bs.getValue(FACING))));

    @Override
    public VoxelShape getShape(BlockState bs, BlockGetter bg, BlockPos bp, CollisionContext context) {
        var be = bg.getBlockEntity(bp, TransferPipe.TRANSFER_NODE_ITEM_BE.get());
        var pipeState = be.isPresent() ? be.get().getPipeState() : TransferPipe.TRANSFER_PIPE.get().defaultBlockState();
        var pipeShape = pipeState != TransferPipe.TRANSFER_PIPE.get().defaultBlockState() ? TransferPipeBlock.getShape(pipeState) : Shapes.empty();

        return Shapes.or(shapeCache.get(bs), pipeShape);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos bp, BlockState bs) {
        return new TransferNodeBlockEntity(bp, bs);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == TransferPipe.TRANSFER_NODE_ITEM_BE.get() ? TransferNodeBlockEntity::tick : null;
    }

    /*public static final Map<Direction, EnumProperty<NodeStates>> NODES = Direction.stream().collect(Collectors
            .toMap(UnaryOperator.identity(), d -> EnumProperty.create(d.getName(), NodeStates.class)));

    public TransferNodeBlock(){
        super(BlockBehaviour.Properties.of(Material.STONE));
        var s = stateDefinition.any();
        for (Direction d : Direction.values()) {
            s = s.setValue(NODES.get(d), NodeStates.NONE);
        }
        registerDefaultState(s);
    }

    public enum NodeStates implements StringRepresentable {
        NONE,
        TRANSFER_NODE_ITEM,
        TRANSFER_NODE_FLUID,
        RETRIEVAL_NODE_ITEM,
        RETRIEVAL_NODE_FLUID;


        @Override
        public String getSerializedName() {
            return this.name().toLowerCase();
        }
    }*/
}
