package nin.transferpipe.util.java;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class UtilMap<K, V> extends LinkedHashMap<K, V> {

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
            keys.remove(key);
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
    public List<K> keys = new ArrayList<>();

    @Override
    public V put(K key, V value) {
        keys.add(key);
        return super.put(key, value);
    }

    @Override
    public void clear() {
        keys.clear();
        super.clear();
    }

    public K getFirstKey() {
        return keys.get(0);
    }

    public K getLastKey() {
        return keys.get(size() - 1);
    }

    public V getFirstValue() {
        return get(getFirstKey());
    }

    public V getLastValue() {
        return get(getLastKey());
    }
}
