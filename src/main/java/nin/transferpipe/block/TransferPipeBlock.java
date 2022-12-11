package nin.transferpipe.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.items.IItemHandler;
import nin.transferpipe.TransferPipe;
import org.jetbrains.annotations.Nullable;

public class TransferPipeBlock extends PipeBlock {

    public TransferPipeBlock() {
        super(1 / 8F, BlockBehaviour.Properties.of(Material.STONE));
        registerDefaultState(stateDefinition.any()
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP_ATTACHED, false)
                .setValue(DOWN_ATTACHED, false)
                .setValue(NORTH_ATTACHED, false)
                .setValue(SOUTH_ATTACHED, false)
                .setValue(EAST_ATTACHED, false)
                .setValue(WEST_ATTACHED, false));
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
        neighborChanged(l, bp);
    }

    public void neighborChanged(Level l, BlockPos bp) {
        var os = l.getBlockState(bp);
        var ns = getState(l, bp);
        if (ns != os)
            l.setBlockAndUpdate(bp, ns);
    }

    public BlockState getState(Level l, BlockPos p) {
        var s = defaultBlockState();
        for (var d : Direction.stream().toList()) {
            s = s.setValue(DtoDP(d), isPipe(l, p, d) || canConnect(l, p, d))
                    .setValue(DtoDAP(d), canConnect(l, p, d));
        }
        return s;
    }

    public boolean isPipe(LevelAccessor l, BlockPos p, Direction d) {
        return l.getBlockState(p.relative(d)).getBlock() == TransferPipe.TRANSFER_PIPE.get();
    }

    public boolean canConnect(LevelAccessor l, BlockPos p, Direction d) {
        return l.getBlockEntity(p.relative(d)) instanceof Container;
    }


    public static final BooleanProperty NORTH_ATTACHED = BooleanProperty.create("north_attached");
    public static final BooleanProperty EAST_ATTACHED = BooleanProperty.create("east_attached");
    public static final BooleanProperty SOUTH_ATTACHED = BooleanProperty.create("south_attached");
    public static final BooleanProperty WEST_ATTACHED = BooleanProperty.create("west_attached");
    public static final BooleanProperty UP_ATTACHED = BooleanProperty.create("up_attached");
    public static final BooleanProperty DOWN_ATTACHED = BooleanProperty.create("down_attached");

    public static BooleanProperty DtoDP(Direction d) {
        return switch (d) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    public static BooleanProperty DtoDAP(Direction d) {
        return switch (d) {
            case DOWN -> DOWN_ATTACHED;
            case UP -> UP_ATTACHED;
            case NORTH -> NORTH_ATTACHED;
            case SOUTH -> SOUTH_ATTACHED;
            case WEST -> WEST_ATTACHED;
            case EAST -> EAST_ATTACHED;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, NORTH_ATTACHED, EAST_ATTACHED, SOUTH_ATTACHED, WEST_ATTACHED, UP_ATTACHED, DOWN_ATTACHED);
    }
}
