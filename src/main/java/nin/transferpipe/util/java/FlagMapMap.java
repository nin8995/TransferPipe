package nin.transferpipe.util.java;

import java.util.function.BiFunction;

public class FlagMapMap<K1, K2, V> extends UtilMapMap<K1, K2, V> {

    public FlagMapMap() {
        super();
    }

    public FlagMapMap(Consumer3<K1, K2, V> removeFunc) {
        super(removeFunc);
    }

    /**
     * マーク機能
     */
    public final UtilSetMap<K1, K2> marks = new UtilSetMap<>();

    public void addMarked(K1 k1, K2 k2, V v) {
        add(k1, k2, v, true);
    }

    public void add(K1 k1, K2 k2, V v, boolean marked) {
        add(k1, k2, v);
        if (marked)
            marks.addValue(k1, k2);
    }

    public void reset() {
        removeValueIf((k1, k2, v) -> !marks.contains(k1, k2));
        marks.clear();
    }

    public final UtilMapMap<K1, K2, Boolean> cache = new UtilMapMap<>();

    public void tryLoadCache(BiFunction<K1, K2, LoadResult<V>> valueGetter) {
        cache.removeValueIf((k1, k2, marked) -> {
            var v = valueGetter.apply(k1, k2);
            if (v instanceof LoadResult.A<V> a)
                add(k1, k2, a.v, marked);

            return v instanceof LoadResult.A || v instanceof LoadResult.NA;
        });
    }
}
