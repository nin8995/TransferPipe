package nin.transferpipe.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import nin.transferpipe.block.pipe.Connection;
import nin.transferpipe.util.PipeUtils;
import nin.transferpipe.util.TPUtils;
import org.antlr.v4.misc.OrderedHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Search {

    /**
     * 基本情報
     */

    private BlockPos currentPos = BlockPos.ZERO;//なぜか置いた瞬間にwriteが呼ばれるから初期化しないとエラー
    private BlockPos nextPos;
    private Set<Direction> prevSearchedDirs = new HashSet<>();//これも
    private final OrderedHashMap<BlockPos, Set<Direction>> queue = new OrderedHashMap<>();

    public static String CURRENT_POS = "CurrentPos";
    public static String NEXT_POS = "NextPos";
    public static String QUEUE = "Queue";
    public TileBaseTransferNode be;
    public Level level;//beから取れるけど簡略化
    private boolean initialized;//levelをフィールドに使うということは最初の動作時に初期化しないといけないことを意味する

    public Search(TileBaseTransferNode be) {
        this.be = be;
        reset();
    }

    public Search reset() {
        queue.clear();
        nextPos = be.POS;
        if (be.FACING == null)
            queue.put(nextPos, new HashSet<>());
        else
            addQueue(nextPos, be.FACING);

        if (be.initialized)//インスタンス生成時に呼ばれるresetじゃない→探索が終わったときのreset
            be.onSearchEnd();

        return this;
    }

    public BlockPos getCurrentPos() {
        return currentPos;
    }

    public BlockPos getNextPos() {
        return nextPos;
    }

    public void setPos(int x, int y, int z) {
        currentPos = new BlockPos(x, y, z);
    }

    public CompoundTag write() {
        var tag = new CompoundTag();
        tag.put(CURRENT_POS, NbtUtils.writeBlockPos(currentPos));
        tag.put(NEXT_POS, NbtUtils.writeBlockPos(nextPos));
        tag.put(QUEUE, TPUtils.writePosDirsSetMap(queue));

        return tag;
    }

    public Search read(CompoundTag tag) {
        if (tag.contains(CURRENT_POS))
            currentPos = NbtUtils.readBlockPos(tag.getCompound(CURRENT_POS));
        if (tag.contains(NEXT_POS))
            nextPos = NbtUtils.readBlockPos(tag.getCompound(NEXT_POS));
        if (tag.contains(QUEUE))
            TPUtils.readPosDirs(tag.getCompound(QUEUE), queue::put);

        return this;
    }

    /**
     * 機能
     */

    public Search proceed() {
        if (!initialized) {
            this.level = be.getLevel();
            initialized = true;
        }

        //進
        currentPos = nextPos;
        if (PipeUtils.currentPipeBlock(level, currentPos) != null)
            be.onProceedPipe(currentPos);
        if (!queue.containsKey(currentPos))//何らかの原因でqueueにないものがnextPosになってたらリセット。ロード前にnbt直接編集かアドオンのコード？
            return reset();
        prevSearchedDirs = queue.get(currentPos);
        queue.remove(currentPos);

        //分かりやすさのための検索状況パーティクル
        if (be.addParticle)
            be.addPipeParticle(currentPos.getCenter());

        //仕事先があれば即出勤
        var workableDirs = getWorkableDirs();
        if (!workableDirs.isEmpty()) {
            if (be.canWorkMultipleAtTime())
                workableDirs.forEach(this::onTerminal);
            else
                onTerminal(random(workableDirs));

            if (!be.pseudoRoundRobin)
                return reset();
        }

        //次の地点を取得＆探索すべき地点を追加
        var proceedables = getProceedablePipeDirs();
        if (proceedables.isEmpty()) {//末端処理
            if (!be.isNormalSearch() && hasNextQueue())
                nextPos = be.depthFirst ? queue.getKey(queue.size() - 1) : queue.getKey(0);
            else
                return reset();
        } else {//進めるとき
            var randomProceedableDir = random(proceedables);
            var randomProceedablePos = currentPos.relative(randomProceedableDir);

            if (be.isNormalSearch())
                addQueue(randomProceedablePos, randomProceedableDir.getOpposite());
            else
                proceedables.forEach(d -> addQueue(currentPos.relative(d), d.getOpposite()));

            nextPos = be.breadthFirst ? queue.getKey(0) : randomProceedablePos;
        }

        return this;
    }

    public Set<Direction> getProceedablePipeDirs() {
        return Direction.stream()
                .filter(d -> !prevSearchedDirs.contains(d))
                .filter(d -> PipeUtils.canProceedPipe(level, currentPos, d, be)).collect(Collectors.toSet());
    }

    public Set<Direction> getWorkableDirs() {
        return Direction.stream()
                .filter(d -> !prevSearchedDirs.contains(d))
                .filter(d -> PipeUtils.currentConnection(level, currentPos, d) == Connection.MACHINE)
                .filter(d -> be.canWork(currentPos.relative(d), d.getOpposite())).collect(Collectors.toSet());
    }

    public Direction random(Set<Direction> dirs) {
        return TPUtils.getRandomlyFrom(dirs, level.random);
    }

    public void addQueue(BlockPos pos, @NotNull Direction dir) {
        TPUtils.addToSetMap(queue, pos, dir);
    }

    public boolean hasNextQueue() {
        return queue.entrySet().iterator().hasNext();
    }

    public void onTerminal(Direction dir) {
        be.terminal(currentPos.relative(dir), dir.getOpposite());
        if (be.addParticle)
            be.addBlockParticle(currentPos.relative(dir).getCenter());
    }
}
