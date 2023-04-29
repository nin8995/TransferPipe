package nin.transferpipe.util.transferpipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;
import nin.transferpipe.block.TPBlocks;
import nin.transferpipe.block.node.BlockTransferNode;
import nin.transferpipe.block.node.TileBaseTransferNode;
import nin.transferpipe.block.pipe.Connection;
import nin.transferpipe.block.pipe.Flow;
import nin.transferpipe.block.pipe.TransferPipe;
import nin.transferpipe.util.forge.ForgeUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;

import static nin.transferpipe.block.node.BlockTransferNode.FacingNode.FACING;
import static nin.transferpipe.block.pipe.Connection.MACHINE;
import static nin.transferpipe.block.pipe.TransferPipe.FLOW;

//外部からパイプの各パラメーターを弄る
public class PipeUtils {

    /**
     * パラメーター取得
     */
    //パイプだけがPipeStateを持つとは限らない
    public static BlockState currentState(Level level, BlockPos pos) {
        var bs = level.getBlockState(pos);
        return bs.getBlock() instanceof TransferPipe
               ? bs
               : level.getBlockEntity(pos) instanceof TileBaseTransferNode be
                 ? be.pipeState
                 : null;//PipeStateを得得ないときにnull
    }

    public static Block currentPipeBlock(Level level, BlockPos pos) {
        var state = currentState(level, pos);
        return state != null ? state.getBlock() : null;
    }

    @Nullable
    public static Flow currentFlow(Level l, BlockPos bp) {
        var state = currentState(l, bp);
        return state != null ? currentFlow(state) : null;
    }

    public static Flow currentFlow(BlockState state) {
        return state.getValue(FLOW);
    }

    @Nullable
    public static Connection currentConnection(Level l, BlockPos bp, Direction d) {
        var state = currentState(l, bp);
        return state != null ? currentConnection(state, d) : null;
    }

    public static Connection currentConnection(BlockState state, Direction d) {
        return state.getValue(TransferPipe.CONNECTIONS.get(d));
    }

    /**
     * パラメーターを基にBlockState計算
     */

    //現在のFlowで再計算
    public static BlockState recalcConnections(Level l, BlockPos bp) {
        return calcConnections(l, bp, currentState(l, bp));
    }

    //上と同じに見えて、置く前はLevelからFlow取得できないから、Flow指定しないとエラー
    public static BlockState calcInitialState(Level l, BlockPos bp, BlockState defaultState) {
        return calcConnections(l, bp, defaultState);
    }

    //回したFlowで再計算
    public static BlockState cycleFlowAndRecalc(Level l, BlockPos bp) {
        return calcConnections(l, bp, currentState(l, bp), Flow.getNext(l, bp));
    }

    public static BlockState calcConnections(Level l, BlockPos bp, BlockState state, Flow f) {
        return calcConnections(l, bp, state.setValue(FLOW, f));
    }

    public static BlockState calcConnections(Level l, BlockPos bp, BlockState state) {
        return Connection.map(state, d -> calcConnection(l, bp, state.getValue(FLOW), d));
    }

    public static Connection calcConnection(Level l, BlockPos bp, Flow f, Direction d) {
        return shouldConnectToMachine(f, l, bp, d) ? MACHINE
                                                   : shouldConnectToPipe(f, l, bp, d) ? Connection.PIPE
                                                                                      : Connection.NONE;
    }

    public static boolean shouldConnectToMachine(Flow f, Level l, BlockPos p, Direction d) {
        return f != Flow.IGNORE
                && !(l.getBlockState(p).getBlock() instanceof BlockTransferNode.FacingNode<?> node && node.facing(l, p) == d)
                && isWorkPlace(l, p.relative(d), d.getOpposite());
    }

    public static boolean isWorkPlace(Level level, BlockPos pos, @Nullable Direction dir) {
        return !(level.getBlockState(pos).getBlock() instanceof TransferPipe || level.getBlockEntity(pos) instanceof TileBaseTransferNode)
                && (ForgeUtils.hasItemHandler(level, pos, dir)
                || ForgeUtils.hasFluidHandler(level, pos, dir)
                || ForgeUtils.hasEnergyStorage(level, pos, dir));
    }

    public static boolean shouldConnectToPipe(Flow myFlow, Level l, BlockPos bp, Direction d) {
        if (eachOtherIsPipe(l, bp, d)) {//パイプ同士になっているか
            var yourFlow = currentFlow(l, bp.relative(d));//パイプである確証があるので絶対に非null
            return canFlow(myFlow, yourFlow, d);
        }
        return false;
    }

    public static boolean eachOtherIsPipe(Level l, BlockPos bp, Direction d) {
        return isPipe(l, bp, d) && isPipe(l, bp.relative(d), d.getOpposite());
    }

    //posのdir方向はパイプか
    public static boolean isPipe(Level level, BlockPos pos, Direction dir) {
        return currentState(level, pos.relative(dir)) != null && !isFacingNodeWithDirection(level, pos.relative(dir), dir.getOpposite());
    }

    //自分と相手との間をどちらか一方でも実際に進めるか
    public static boolean canFlow(Flow myFlow, Flow yourFlow, Direction d) {
        return myFlow.openToPipe(d) || yourFlow.openToPipe(d.getOpposite());
    }

    //このflowはd方向に開いているか
    public static boolean isFlowOpenToPipe(Flow f, Direction d) {
        return f == Flow.fromDir(d) || f == Flow.ALL || f == Flow.IGNORE;
    }

    /**
     * 計算済みのものから判定＆その他パイプ関連
     */

    public static boolean canProceedPipe(Level level, BlockPos pos, Direction dir, TileBaseTransferNode node) {
        return isPipeConnection(level, pos, dir) && canNodeProceedTo(level, pos, dir, node) && isFlowOpenToPipe(level, pos, dir);
    }

    public static boolean isPipeConnection(Level level, BlockPos pos, Direction dir) {
        return currentConnection(level, pos, dir) == Connection.PIPE;
    }

    public static boolean isFlowOpenToPipe(Level level, BlockPos pos, Direction dir) {
        return isFlowOpenToPipe(currentFlow(level, pos), dir);
    }

    public static boolean canNodeProceedTo(Level level, BlockPos pos, Direction dir, TileBaseTransferNode node) {
        return currentPipeBlock(level, pos.relative(dir)) instanceof TransferPipe pipe && pipe.isValidSearcher(node);
    }

    public static boolean isFacingNodeWithDirection(Level level, BlockPos pos, Direction dir) {
        return isFacingNodeWithDirection(level.getBlockState(pos), dir);
    }

    public static boolean isFacingNodeWithDirection(BlockState state, Direction dir) {
        return state.getBlock() instanceof BlockTransferNode.FacingNode<?> && state.getValue(FACING) == dir;
    }

    public static Set<BlockState> centers = TPBlocks.PIPES.stream().map(RegistryObject::get)
            .map(Block::defaultBlockState)
            .flatMap(state -> Flow.stream().map(flow -> state.setValue(FLOW, flow)))
            .collect(Collectors.toSet());

    public static boolean centerOnly(BlockState bs) {
        return centers.contains(bs);
    }

    public static final TagKey<Item> WRENCH_TAG = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "tools/wrench"));

    public static boolean usingWrench(Player pl, InteractionHand hand) {
        var item = pl.getItemInHand(hand);
        return item.is(Items.STICK) || item.is(WRENCH_TAG);
    }
}
