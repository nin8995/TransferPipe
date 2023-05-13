package nin.transferpipe.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import nin.transferpipe.util.transferpipe.PipeInstance;
import nin.transferpipe.util.transferpipe.TPUtils;

/**
 * 基本のパイプ。しかしこれだけが流れる方向を制御できる。（全部制御できるようにしてたら起動に時間かかる）
 */
public class TransferPipe extends Pipe {

    public TransferPipe() {
        super();

        var defaultState = stateDefinition.any();
        defaultState = defaultState.setValue(FLOW, Flow.ALL);
        defaultState = Connection.map(defaultState, d -> Connection.NONE);
        registerDefaultState(defaultState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        if (!allPipeHasFlow)
            builder.add(FLOW);
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
}
