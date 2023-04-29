package nin.transferpipe.util.transferpipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.INBTSerializable;
import nin.transferpipe.util.minecraft.PosDirsSet;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 検索をする。検索者と、検索のための情報を保持する。
 */
public class SearchInstance implements INBTSerializable<CompoundTag> {

    /**
     * 初期化処理
     */
    public Searcher searcher;
    public final PosDirsSet queue = new PosDirsSet();//探索先のキュー。探索地点と既に探索された方角の対応を保持している。決して空洞にはならない。
    public BlockPos searchingPos;//今探索している場所
    public Set<Direction> prevSearchedDirs;//その地点から見て既に探索された方角
    public Level level;

    public SearchInstance(Searcher searcher) {
        this.searcher = searcher;

        //この辺初期化しないとproceed前に参照されたときまずいから仕方なくしてる
        searchingPos = searcher.initialPos();
        prevSearchedDirs = searcher.initialNonValidDirs();
    }

    public SearchInstance resetQueue() {
        queue.clear();
        queue.addAll(searcher.initialPos(), searcher.initialNonValidDirs());

        return this;
    }

    public void onLoad(Level level) {
        this.level = level;
    }

    /**
     * 探索場所を進め、その場所での処理を行う。
     */
    public SearchInstance proceed() {
        searchingPos = getNextPos();
        prevSearchedDirs = queue.get(searchingPos);
        queue.remove(searchingPos);
        searcher.onSearchProceed(searchingPos);

        var destDirs = getDestDirs();
        if (!destDirs.isEmpty()) {
            //周囲の目的地に対する処理
            if (searcher.isMultiTask())
                destDirs.forEach(this::destRelative);
            else
                destRelative(random(destDirs));

            if (searcher.findToEnd())
                return end();
        }

        var proceedableDirs = getProceedableDirs();
        if (!proceedableDirs.isEmpty()) {
            //次の探索地点に対する処理
            if (searcher.isFullSearch())
                proceedableDirs.forEach(this::addQueueRelative);
            else
                addQueueRelative(random(proceedableDirs));
        } else {
            //行き詰った
            searcher.onSearchTerminal();
            if (!(searcher.isFullSearch() && queue.hasNext()))
                return end();
        }

        return this;
    }

    /**
     * 次の探索地点を取得
     */
    public BlockPos getNextPos() {
        return searcher.pickNext(queue);
    }

    /**
     * 探索終了
     */
    public SearchInstance end() {
        searcher.onSearchEnd();
        return resetQueue();
    }

    /**
     * 目的地の方角
     */
    public Set<Direction> getDestDirs() {
        return Direction.stream()
                .filter(d -> !prevSearchedDirs.contains(d))
                .filter(d -> searcher.isDest(searchingPos, d, searchingPos.relative(d), d.getOpposite())).collect(Collectors.toSet());
    }

    /**
     * 現在地のdir方向が目的地
     */
    public void destRelative(Direction dir) {
        searcher.onFind(searchingPos.relative(dir), dir.getOpposite());
    }

    /**
     * 進める方角
     */
    public Set<Direction> getProceedableDirs() {
        return Direction.stream()
                .filter(d -> !prevSearchedDirs.contains(d))
                .filter(d -> searcher.canProceed(searchingPos, d, searchingPos.relative(d), d.getOpposite())).collect(Collectors.toSet());
    }

    /**
     * 現在地のdir方向をキューに入れる
     */
    public void addQueueRelative(@Nullable Direction dir) {
        if (dir != null)
            queue.add(searchingPos.relative(dir), dir.getOpposite());
        else
            queue.add(searchingPos, null);
    }

    /**
     * NBT
     */
    public static String QUEUE = "Queue";

    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();

        tag.put(QUEUE, queue.serializeNBT());

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains(QUEUE))
            queue.deserializeNBT(tag.getCompound(QUEUE));
    }

    /**
     * 省略
     */
    public Direction random(Set<Direction> dirs) {
        return TPUtils.getRandomlyFrom(dirs, level.random);
    }
}
