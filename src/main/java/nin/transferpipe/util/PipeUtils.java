package nin.transferpipe.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.TransferNodeBlock;
import nin.transferpipe.block.TransferNodeBlockEntity;
import nin.transferpipe.block.TransferPipeBlock;
import nin.transferpipe.block.state.Connection;
import nin.transferpipe.block.state.Flow;
import org.jetbrains.annotations.Nullable;

//外部からパイプの各パラメーターを弄る
public class PipeUtils {

    /**
     * パラメーター取得
     */

    public static BlockState defaultState() {
        return TPBlocks.TRANSFER_PIPE.get().defaultBlockState();
    }

    //パイプだけがPipeStateを持つとは限らない
    public static BlockState currentState(Level level, BlockPos pos) {
        var bs = level.getBlockState(pos);
        return bs.getBlock() instanceof TransferPipeBlock ? bs
                : level.getBlockEntity(pos) instanceof TransferNodeBlockEntity be ? be.getPipeState()
                : null;//PipeStateを得得ないときにnull
    }

    @Nullable
    public static Flow currentFlow(Level l, BlockPos bp) {
        var bs = currentState(l, bp);
        return bs != null ? bs.getValue(TransferPipeBlock.FLOW) : null;
    }

    @Nullable
    public static Connection currentConnection(Level l, BlockPos bp, Direction d) {
        var bs = currentState(l, bp);
        return bs != null ? bs.getValue(TransferPipeBlock.CONNECTIONS.get(d)) : null;
    }

    /**
     * パラメーターを基にBlockState計算
     */

    //現在のFlowで再計算
    public static BlockState recalcConnections(Level l, BlockPos bp) {
        return calcConnections(l, bp, currentFlow(l, bp));
    }

    //上と同じに見えて、置く前はLevelからFlow取得できないから、Flow指定しないとエラー
    public static BlockState calcInitialState(Level l, BlockPos bp) {
        return calcConnections(l, bp, defaultState().getValue(TransferPipeBlock.FLOW));
    }

    //回したFlowで再計算
    public static BlockState cycleFlowAndRecalc(Level l, BlockPos bp) {
        return calcConnections(l, bp, Flow.getNext(l, bp, currentFlow(l, bp)));
    }

    public static BlockState calcConnections(Level l, BlockPos bp, Flow f) {
        var state = defaultState().setValue(TransferPipeBlock.FLOW, f);
        state = Connection.map(state, d -> calcConnection(l, bp, f, d));
        return state;
    }

    public static Connection calcConnection(Level l, BlockPos bp, Flow f, Direction d) {
        return shouldConnectToMachine(f, l, bp, d) ? Connection.MACHINE
                : shouldConnectToPipe(f, l, bp, d) ? Connection.PIPE
                : Connection.NONE;
    }

    /**
     * もろもろの判定
     */

    public static boolean shouldConnectToPipe(Flow myFlow, Level l, BlockPos bp, Direction d) {
        if (isPipe(l, bp, d) && isPipe(l, bp.relative(d), d.getOpposite())) {//パイプ同士になっているか
            var yourFlow = currentFlow(l, bp.relative(d));//パイプである確証があるので絶対に非null
            return canGoThroughPipe(myFlow, d) || canGoThroughPipe(yourFlow, d.getOpposite());//自分と相手との間をどちらか一方でも実際に進めるか
        }
        return false;
    }

    //bpからdを見たらパイプか
    public static boolean isPipe(Level l, BlockPos bp, Direction d) {
        var bs = l.getBlockState(bp.relative(d));
        return bs.getBlock() instanceof TransferPipeBlock//パイプならOK
                || (bs.getBlock() instanceof TransferNodeBlock && bs.getValue(TransferNodeBlock.FACING) != d.getOpposite());//ノードでも接地面じゃなければOK
    }

    //（先にパイプがあるとして）このflowはd方向に開いているか
    public static boolean canGoThroughPipe(Flow f, Direction d) {
        return f == Flow.fromDirection(d) || f == Flow.ALL || f == Flow.IGNORE;
    }

    public static boolean shouldConnectToMachine(Flow f, Level l, BlockPos p, Direction d) {
        return f != Flow.IGNORE && isWorkPlace(l, p.relative(d));
    }

    public static boolean isWorkPlace(Level l, BlockPos pos) {
        return l.getBlockEntity(pos) instanceof Container || l.getBlockState(pos).getBlock() instanceof WorldlyContainerHolder;
    }

    public static boolean centerOnly(BlockState bs) {
        return Direction.stream().allMatch(d -> bs.getValue(TransferPipeBlock.CONNECTIONS.get(d)) == Connection.NONE);
    }

    public static boolean usingWrench(Player pl, InteractionHand hand) {
        var item = pl.getItemInHand(hand).getItem();
        return item == Items.STICK;
    }
}
