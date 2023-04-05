package nin.transferpipe.block.state;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import nin.transferpipe.block.TransferNodeBlock;
import nin.transferpipe.util.PipeUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Flow implements StringRepresentable {
    ALL,//全方向に通す
    UP,//上にのみ通す
    DOWN,
    NORTH,
    EAST,
    SOUTH,
    WEST,
    BLOCK,//全方向に通さない(入ったっ切り)
    IGNORE;//全方向通すが、機械は無視

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }

    public static Stream<Flow> stream() {
        return Arrays.stream(Flow.values());
    }

    public static Flow fromDirection(Direction d) {
        return Flow.stream().filter(f -> f.name().equals(d.name())).findFirst().get();
    }

    public Flow nextOrdinal() {
        return Flow.stream().filter(fs -> fs.ordinal() == this.ordinal() + 1).findFirst().orElse(ALL);
    }

    /**
     * あり得る次Flowの計算
     */

    public static Flow getNext(Level level, BlockPos pos, Flow currentFlow) {
        var validFlows = calcValidFlows(level, pos);

        //今のflowから巡っていって最初にvalidFlowsにあったものを返す
        var searching = currentFlow.nextOrdinal();
        while (true) {
            if (validFlows.contains(searching))
                return searching;
            searching = searching.nextOrdinal();
        }
    }

    public static Set<Flow> calcValidFlows(Level level, BlockPos pos) {
        //方角依存のFlowとそうでないFlowとで分けて考える
        var validFlows = nonDirectionalFlows();

        //つながる方角を抽出
        var nodeDir = level.getBlockState(pos).getBlock() instanceof TransferNodeBlock ? level.getBlockState(pos).getValue(TransferNodeBlock.FACING) : null;
        var connectableDirs = Direction.stream()
                .filter(d -> d != nodeDir)
                .filter(d -> PipeUtils.isPipe(level, pos, d)).collect(Collectors.toSet());

        //見た目が変わらないパターンを除く
        if (connectableDirs.size() == 0)//どこにもつながってない場合、ALLとBLOCKが同じ見た目となる
            validFlows.remove(BLOCK);//のでBLOCKを除去
        if (connectableDirs.size() == 1)//一方向にしかつながらない場合、その方角とALLが同じ見た目となる
            connectableDirs.clear();//のでその方角をも除去
        //ブロック置いてみたらいきなりボーダーが出てきたってのもアレなのでALL優先

        validFlows.addAll(connectableDirs.stream().map(Flow::fromDirection).collect(Collectors.toSet()));
        return validFlows;
    }

    public static Set<Flow> nonDirectionalFlows() {
        var flows = stream().collect(Collectors.toSet());
        flows.removeAll(directionalFlows());
        return flows;
    }

    public static Set<Flow> directionalFlows() {
        return Direction.stream().map(Flow::fromDirection).collect(Collectors.toSet());
    }
}