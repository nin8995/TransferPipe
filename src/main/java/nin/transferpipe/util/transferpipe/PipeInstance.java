package nin.transferpipe.util.transferpipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import nin.transferpipe.block.pipe.Connection;
import nin.transferpipe.block.pipe.Flow;
import nin.transferpipe.block.pipe.TransferPipe;
import nin.transferpipe.util.java.JavaUtils;
import nin.transferpipe.util.minecraft.MCUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static nin.transferpipe.block.pipe.Connection.*;
import static nin.transferpipe.block.pipe.Flow.*;
import static nin.transferpipe.block.pipe.TransferPipe.FLOW;

public class PipeInstance {

    public Level level;
    public BlockPos pos;
    public BlockState state;
    public TransferPipe pipe;
    public Flow flow;
    public Map<Direction, Connection> connections;
    public Direction nodeDir;

    public boolean invalid;

    /**
     * 既にパイプがあると仮定しそれを使う
     */
    protected PipeInstance(Level level, BlockPos pos) {
        this(level, pos, TPUtils.currentState(level, pos), TPUtils.currentNodeDir(level, pos));
    }

    public static Optional<PipeInstance> of(Level level, BlockPos pos) {
        return Optional.of(new PipeInstance(level, pos)).filter(it -> !it.invalid);
    }

    /**
     * パイプを指定
     */
    public PipeInstance(Level level, BlockPos pos, BlockState state) {
        this(level, pos, state, null);
    }

    /**
     * パイプもノードの方向も指定
     */
    public PipeInstance(Level level, BlockPos pos, @Nullable BlockState state, @Nullable Direction nodeDir) {
        this.level = level;
        this.pos = pos;
        this.state = state;
        if (state != null && state.getBlock() instanceof TransferPipe pipe) {
            this.pipe = pipe;
            this.flow = TPUtils.flow(state);
            this.connections = MCUtils.dirMap(d -> TPUtils.connection(state, d));
            this.nodeDir = nodeDir;
        } else {
            invalid = true;
        }
    }

    /**
     * BlockState計算
     */
    public static BlockState recalcState(Level level, BlockPos pos) {
        return new PipeInstance(level, pos).calcState();
    }

    public static BlockState precalcState(Level level, BlockPos pos, BlockState state) {
        return new PipeInstance(level, pos, state).calcState();
    }

    public static BlockState precalcState(Level level, BlockPos pos, BlockState state, Direction dir) {
        return new PipeInstance(level, pos, state, dir).calcState();
    }

    public BlockState calcState() {
        return Connection.map(state, this::calcConnection);
    }

    public Connection calcConnection(Direction dir) {
        return isWorkPlace(dir)
               ? MACHINE
               : isProceedablePipe(dir)
                 ? PIPE
                 : NONE;
    }

    public boolean isWorkPlace(Direction dir) {
        return flow != IGNORE
                && nodeDir != dir
                && relative(dir).isEmpty()
                && pipe.isWorkPlace(level, pos.relative(dir), dir.getOpposite());
    }

    public boolean isProceedablePipe(Direction dir) {
        return nodeDir != dir && isValidPipe(dir) && canFlow(dir);
    }

    public boolean isValidPipe(Direction dir) {
        return relative(dir)
                .filter(it -> it.nodeDir != dir.getOpposite())
                .filter(it -> pipe.isValidPipe(it.pipe)).isPresent();
    }

    public boolean canFlow(Direction dir) {
        return relative(dir)
                .filter(it -> flow.canFlow(it.flow, dir)).isPresent();
    }

    public Optional<PipeInstance> relative(Direction dir) {
        return of(level, pos.relative(dir));
    }

    public static BlockState cycleAndCalcState(Level level, BlockPos pos) {
        return new PipeInstance(level, pos).cycleFlow().calcState();
    }

    public PipeInstance cycleFlow() {
        return setFlow(JavaUtils.findNext(calcValidFlows(), flow, Flow.values()));
    }

    public PipeInstance setFlow(Flow flow) {
        this.flow = flow;
        state = state.setValue(FLOW, flow);
        return this;
    }

    public Set<Flow> calcValidFlows() {
        //方角依存のFlowとそうでないFlowとで分けて考える
        var validFlows = nonDirectionalFlows();

        //つながる方角を抽出
        var pipeDirs = JavaUtils.filter(Direction.stream(), this::isValidPipe);

        //見た目が変わらないパターンを除く
        if (pipeDirs.size() == 0)//どこにもつながってない場合、ALLとBLOCKが同じ見た目となる
            validFlows.remove(BLOCK);//のでBLOCKを除去
        if (pipeDirs.size() == 1)//一方向にしかつながらない場合、その方角とALLが同じ見た目となる
            pipeDirs.clear();//のでその方角をも除去
        //ブロック置いてみたらいきなりボーダーが出てきたってのもアレなのでALL優先

        validFlows.addAll(JavaUtils.map(pipeDirs, Flow::fromDir));
        return validFlows;
    }

    /**
     * 計算されたBlockStateを基に判定
     */
    public static boolean canProceed(Level level, BlockPos pos, Direction dir, Searcher searcher) {
        return of(level, pos).map(it -> it.canProceed(dir, searcher)).orElse(false);
    }

    public boolean canProceed(Direction dir, Searcher searcher) {
        return pipe.isValidSearcher(searcher) && connections.get(dir) == PIPE && flow.openTo(dir);
    }
}
