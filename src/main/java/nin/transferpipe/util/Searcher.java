package nin.transferpipe.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Collections;
import java.util.Set;

/**
 * 検索者。検索方法の情報提供者。
 */
public interface Searcher {

    /**
     * 初期化
     */
    BlockPos initialPos();

    default Direction initialNonValidDir() {
        return null;
    }

    default Set<Direction> initialNonValidDirs() {
        return initialNonValidDir() != null ? Set.of(initialNonValidDir()) : Collections.emptySet();
    }

    /**
     * 必須機能
     */
    boolean isDest(BlockPos pos, Direction dir, BlockPos relativePos, Direction workDir);

    void onFind(BlockPos pos, Direction dir);

    boolean canProceed(BlockPos pos, Direction dir, BlockPos relativePos, Direction workDir);

    /**
     * 検索方法変更の余地
     */
    default boolean findToEnd() {
        return true;
    }

    default boolean isFullSearch() {
        return false;
    }

    default boolean isMultiTask() {
        return false;
    }

    default BlockPos pickNext(OrderedSetMap<BlockPos, Direction> queue) {
        return queue.getLastKey();
    }

    /**
     * 介入の余地
     */
    default void onSearchProceed(BlockPos pos) {
    }

    default void onSearchTerminal() {
    }

    default void onSearchEnd() {
    }
}
