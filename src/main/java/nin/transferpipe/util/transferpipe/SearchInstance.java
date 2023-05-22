package nin.transferpipe.util.transferpipe;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.INBTSerializable;
import nin.transferpipe.block.node.BaseTileNode;
import nin.transferpipe.block.pipe.FunctionChanger;
import nin.transferpipe.util.minecraft.MCUtils;
import nin.transferpipe.util.minecraft.PosDirsSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 検索をする。検索者と、検索のための情報を保持する。
 */
public class SearchInstance implements INBTSerializable<CompoundTag> {

    /**
     * 初期化処理
     */
    public Searcher searcher;//探索者
    public BlockPos searchingPos;//今探索している場所
    public Set<Direction> prevSearchedDirs;//その地点から見て既に探索された方角

    public SearchInstance(Searcher searcher) {
        this.searcher = searcher;

        //この辺初期化しないとproceed前に参照されたときまずいから仕方なくしてる
        searchingPos = searcher.initialPos();
        prevSearchedDirs = searcher.initialNonValidDirs();
    }

    public final PosDirsSet queue = new PosDirsSet();//探索先のキュー。探索地点と既に探索された方角の対応を保持している。決して空洞にはならない。
    public final LongOpenHashSet memory = new LongOpenHashSet();//必要であれば探索済み地点を保存

    public SearchInstance reset() {
        queue.clear();
        queue.addAll(searcher.initialPos(), searcher.initialNonValidDirs());

        memory.clear();
        memory.trim();

        return this;
    }

    public Level level;//省略

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


        FunctionChanger changer = null;
        BaseTileNode<?> node = null;
        Object cache = null;
        if (TPUtils.currentPipeBlock(level, searchingPos) instanceof FunctionChanger changerr && searcher instanceof BaseTileNode<?> nodee) {
            changer = changerr;
            node = nodee;
            cache = changerr.storeAndChange(searchingPos, nodee);
        }
        var destDirs = getDestDirs();
        if (!destDirs.isEmpty()) {
            //周囲の目的地に対する処理

            if (searcher.isMultiTask())
                destDirs.forEach(this::destRelative);
            else
                destRelative(random(destDirs));

            if (changer != null)
                changer.restore(cache, node);
            if (searcher.findToEnd())
                return end();
            if (searcher.stickingSearch()) {
                queue.addAll(searchingPos, prevSearchedDirs);
                return this;
            }
        } else if (changer != null)
            changer.restore(cache, node);

        var proceedableDirs = getProceedableDirs();
        if (!proceedableDirs.isEmpty()) {
            //次の探索地点に対する処理
            if (searcher.isFullSearch())
                proceedableDirs.forEach(this::addQueueRelative);
            else
                addQueueRelative(random(proceedableDirs));

            if (searcher.useMemory())
                memory.trim();
        } else {
            //行き詰った
            searcher.onSearchTerminal();
            if (!(searcher.isFullSearch() && queue.hasNext()))
                return end();
        }

        return this;
    }

    public BlockPos getNextPos() {
        //queueをremoveし次のqueueを得るまでの過程でクラッシュするなどするとempty
        if (queue.isEmpty())
            return reset().searchingPos;

        return searcher.pickNext(queue);
    }

    public SearchInstance end() {
        searcher.onSearchEnd();
        return reset();
    }

    public Set<Direction> getDestDirs() {
        return Direction.stream()
                .filter(d -> !prevSearchedDirs.contains(d))
                .filter(d -> searcher.isDest(searchingPos, d, searchingPos.relative(d), d.getOpposite())).collect(Collectors.toSet());
    }

    public void destRelative(Direction dir) {
        searcher.onFind(searchingPos.relative(dir), dir.getOpposite());
    }

    public Set<Direction> getProceedableDirs() {
        return Direction.stream()
                .filter(d -> !prevSearchedDirs.contains(d))
                .filter(d -> !(searcher.useMemory() && memory.contains(searchingPos.relative(d).asLong())))
                .filter(d -> searcher.canProceed(searchingPos, d, searchingPos.relative(d), d.getOpposite())).collect(Collectors.toSet());
    }

    public void addQueueRelative(@Nullable Direction dir) {
        if (dir != null)
            addQueue(searchingPos.relative(dir), dir.getOpposite());
        else
            addQueue(searchingPos, null);
    }

    public void addQueue(BlockPos pos, @Nullable Direction dir) {
        queue.addValue(pos, dir);
        if (searcher.useMemory())
            memory.add(pos.asLong());
    }

    /**
     * NBT
     */
    public static String QUEUE = "Queue";
    public static String MEMORY = "Memory";

    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();

        tag.put(QUEUE, queue.serializeNBT());
        tag.putLongArray(MEMORY, memory.toLongArray());

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains(QUEUE))
            queue.deserializeNBT(tag.getCompound(QUEUE));
        if (tag.contains(MEMORY)) {
            memory.addAll(LongOpenHashSet.toSet(Arrays.stream(tag.getLongArray(MEMORY))));
            memory.trim();
        }
    }

    /**
     * 省略
     */
    public Direction random(Set<Direction> dirs) {
        return MCUtils.getRandomlyFrom(dirs, level.random);
    }
}
