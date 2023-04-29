package nin.transferpipe.util.java;

import org.antlr.v4.misc.OrderedHashMap;

import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class UtilMap<K, V> extends OrderedHashMap<K, V> {

    /**
     * 削除機能拡張
     */
    private final BiConsumer<K, V> removeFunc;

    public UtilMap() {
        this((k, v) -> {
        });
    }

    public UtilMap(BiConsumer<K, V> removeFunc) {
        this.removeFunc = removeFunc;
    }

    public void removeIf(BiPredicate<K, V> shouldRemove) {
        var toRemove = new HashSet<K>();
        forEach((pos, v) -> {
            if (shouldRemove.test(pos, v))
                toRemove.add(pos);
        });

        toRemove.forEach(this::remove);
    }

    @Override
    public V remove(Object key) {
        try {
            removeFunc.accept((K) key, get(key));
        } catch (Exception ignored) {
        }
        return super.remove(key);
    }

    public void invalidate() {
        forEach(removeFunc);
    }

    /**
     * 追加番号から取得
     */
    public K getFirstKey() {
        return getKey(0);
    }

    public K getLastKey() {
        return getKey(size() - 1);
    }

    public V getFirstValue() {
        return get(getFirstKey());
    }

    public V getLastValue() {
        return get(getLastKey());
    }
}
