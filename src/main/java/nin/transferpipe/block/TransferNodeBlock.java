package nin.transferpipe.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
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
import net.minecraftforge.network.NetworkHooks;
import nin.transferpipe.util.PipeUtils;
import nin.transferpipe.util.TPUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//搬送する種類に依らない、「ノード」のブロックとしての機能
public abstract class TransferNodeBlock extends LightingBlock implements EntityBlock {

    /**
     * 基本情報
     */

    public static DirectionProperty FACING = BlockStateProperties.FACING;

    private static final Component CONTAINER_TITLE = Component.translatable("block.node.menu");

    public TransferNodeBlock() {
        super(BlockBehaviour.Properties.of(Material.STONE));
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

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == getType() ? (Level l, BlockPos p, BlockState bs, T t) -> {
            if (t instanceof TransferNodeBlockEntity be)
                be.tick();
        } : null;
    }

    /**
     * 当たり判定
     */

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
                shape.set(Shapes.or(shape.get(), TransferPipeBlock.getShape(be.getPipeState())));
        });

        return shape.get();
    }

    /**
     * 一般の機能
     */

    @Override
    public void neighborChanged(BlockState p_60509_, Level level, BlockPos pos, Block p_60512_, BlockPos p_60513_, boolean p_60514_) {
        if (level.getBlockEntity(pos) instanceof TransferNodeBlockEntity be) {
            var prevState = be.getPipeState();
            var currentState = PipeUtils.recalcConnections(level, pos);
            if (prevState != currentState)
                be.setPipeStateAndUpdate(currentState);
        }

        super.neighborChanged(p_60509_, level, pos, p_60512_, p_60513_, p_60514_);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult p_60508_) {
        if (level.getBlockEntity(pos) instanceof TransferNodeBlockEntity be) {
            if (PipeUtils.usingWrench(player, hand)) {
                be.setPipeStateAndUpdate(PipeUtils.cycleFlowAndRecalc(level, pos));
                return InteractionResult.SUCCESS;
            }

            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, this.getMenuProvider(state, level, pos));
                be.setChanged();//ほんとはslotが変わったときにMenu側からsetChanged呼びたい
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    /**
     * 搬送種毎の情報
     */

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos p_153215_, BlockState p_153216_) {
        return entityCreator().apply(p_153215_, p_153216_);
    }

    public abstract BlockEntityType<? extends TransferNodeBlockEntity> getType();

    //いちいち引数書くのめんどいから::だけで実装したい
    public abstract BiFunction<BlockPos, BlockState, BlockEntity> entityCreator();

    public static class Item extends TransferNodeBlock {

        @Override
        public BlockEntityType<? extends TransferNodeBlockEntity> getType() {
            return TPBlocks.TRANSFER_NODE_ITEM.entity();
        }

        @Override
        public BiFunction<BlockPos, BlockState, BlockEntity> entityCreator() {
            return TransferNodeBlockEntity.Item::new;
        }

        @Nullable
        @Override
        public MenuProvider getMenuProvider(BlockState p_60563_, Level level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof TransferNodeBlockEntity.Item be ? new SimpleMenuProvider(
                    (i, inv, pl) -> new TransferNodeMenu.Item(be.getItemSlotHandler(), be.getUpgrades(), be.data, i, inv, ContainerLevelAccess.create(level, pos)),
                    Component.translatable("menu.title.transferpipe.node_item"))
                    : null;
        }
    }
}
