package nin.transferpipe.util.java;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class FlagMap<K, V> extends UtilMap<K, V> {

    public FlagMap() {
        super();
    }

    public FlagMap(BiConsumer<K, V> removeFunc) {
        super(removeFunc);
    }

    /**
     * マーク機能
     */
    public final Set<K> marks = new HashSet<>();

    public void putMarked(K k, V v) {
        put(k, v, true);
    }

    public void put(K k, V v, Boolean marked) {
        put(k, v);
        if (marked)
            marks.add(k);
    }

    public void reset() {
        removeIf((k, v) -> !marks.contains(k));
        marks.clear();
    }

    public final UtilMap<K, Boolean> cache = new UtilMap<>();

    public void tryLoadCache(Function<K, LoadResult<V>> valueGetter) {
        cache.removeIf((k, marked) -> {
            var v = valueGetter.apply(k);
            if (v instanceof LoadResult.A<V> a)
                put(k, a.v, marked);

            return v instanceof LoadResult.A || v instanceof LoadResult.NA;
        });
    }
}
