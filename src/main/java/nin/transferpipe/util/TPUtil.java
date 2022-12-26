package nin.transferpipe.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import nin.transferpipe.TransferPipe;
import nin.transferpipe.block.TransferNodeBlock;
import nin.transferpipe.block.TransferNodeBlockEntity;
import nin.transferpipe.block.TransferPipeBlock;
import nin.transferpipe.block.property.ConnectionStates;
import nin.transferpipe.block.property.FlowStates;
import nin.transferpipe.block.property.TPProperties;

public class TPUtil {

    public static BlockState defaultPipeState() {
        return TransferPipe.TRANSFER_PIPE.get().defaultBlockState();
    }

    public static BlockState currentPipeState(Level l, BlockPos bp) {
        var bs = l.getBlockState(bp);
        return bs.getBlock() instanceof TransferPipeBlock ?
                    bs.hasProperty(TPProperties.FLOW) ? bs : defaultPipeState()
                : l.getBlockEntity(bp) instanceof TransferNodeBlockEntity be ? be.getPipeState()
                : null;//TransferPipeのBlockStateを得得ないときにnull
    }

    public static FlowStates currentFlowState(Level l, BlockPos bp) {
        var bs = currentPipeState(l, bp);
        return bs != null ? bs.getValue(TPProperties.FLOW) : null;
    }

    public static BlockState calcInitialPipeState(Level l, BlockPos bp) {
        return calcConnections(l, bp, FlowStates.ALL);
    }

    public static BlockState recalcPipeState(Level l, BlockPos bp) {
        return calcConnections(l, bp, currentFlowState(l, bp));
    }

    public static BlockState cycleFlow(Level l, BlockPos bp) {
        return calcConnections(l, bp, FlowStates.getNext(l, bp, currentFlowState(l, bp)));
    }

    public static BlockState calcConnections(Level l, BlockPos bp, FlowStates f) {
        var bs = defaultPipeState().setValue(TPProperties.FLOW, f);
        for (Direction d : Direction.values())
            bs = bs.setValue(TPProperties.CONNECTIONS.get(d), calcConnection(l, bp, f, d));
        return bs;
    }

    public static ConnectionStates calcConnection(Level l, BlockPos bp, FlowStates f, Direction d) {
        return shouldConnectToPipe(f, l, bp, d) ? ConnectionStates.PIPE
                : shouldConnectToMachine(f, l, bp, d) ? ConnectionStates.MACHINE
                : ConnectionStates.NONE;
    }

    public static boolean shouldConnectToPipe(FlowStates myFlow, Level l, BlockPos bp, Direction d) {
        if (isPipe(l, bp, d) && isPipe(l, bp.relative(d), d.getOpposite())) {//双方から見てもパイプであるか
            var yourFlow = currentFlowState(l, bp.relative(d));//パイプである確証があるので絶対に非null
            return canConnectToPipe(myFlow, d) || canConnectToPipe(yourFlow, d.getOpposite());//自分と相手との間をどちらか一方でも実際に進めるか
        }
        return false;
    }

    public static boolean isPipe(Level l, BlockPos bp, Direction d) {
        var bs = l.getBlockState(bp.relative(d));
        return bs.getBlock() instanceof TransferPipeBlock//相手はパイプか
                || (bs.getBlock() instanceof TransferNodeBlock && bs.getValue(TransferNodeBlock.FACING) != d.getOpposite());//相手はノードであって接地面ではないか
    }

    public static boolean canConnectToPipe(FlowStates f, Direction d) {
        return f == FlowStates.fromDirection(d) || f == FlowStates.ALL || f == FlowStates.IGNORE;//このflowでd方向に進めるか
    }

    public static boolean shouldConnectToMachine(FlowStates f, Level l, BlockPos p, Direction d) {
        return f != FlowStates.IGNORE && //機械無視モードではないか
                (l.getBlockEntity(p.relative(d)) instanceof Container || l.getBlockState(p.relative(d)).getBlock() instanceof WorldlyContainerHolder);//トランスファーパイプの働く先か
    }

    public static boolean hasNoConnection(BlockState bs){
        for (Direction d : Direction.values())
            if(bs.getValue(TPProperties.CONNECTIONS.get(d)) != ConnectionStates.NONE)
                return false;
        return true;
    }
}
