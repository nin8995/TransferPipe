package nin.transferpipe.util.java;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class UtilMapMap<K1, K2, V> extends UtilMap<K1, Map<K2, V>> {

    /**
     * MapMapの取り扱い
     */
    public void add(K1 key1, K2 key2, V value) {
        if (containsKey(key1))
            get(key1).put(key2, value);
        else
            put(key1, new HashMap<>(Map.of(key2, value)));
    }

    public V getValue(K1 k1, K2 k2) {
        return get(k1).get(k2);
    }

    public <NU> List<NU> getValues(Function<V, NU> valGetter) {
        return values().stream().flatMap(keyMap -> keyMap.values().stream().map(valGetter)).toList();
    }

    public void flatForEachKey(BiConsumer<K1, K2> func) {
        forEach((k1, keyMap) -> keyMap.keySet().forEach(k2 -> func.accept(k1, k2)));
    }

    public void flatForEach(Consumer3<K1, K2, V> func) {
        forEach((k1, keyMap) -> keyMap.forEach((k2, v) -> func.accept(k1, k2, v)));
    }

    public boolean contains(K1 k1, K2 k2, V v) {
        return containsKey(k1) && get(k1).containsKey(k2) && get(k1).get(k2).equals(v);
    }

    public int valueCount() {
        return values().stream().mapToInt(Map::size).sum();
    }

    /**
     * 削除機能拡張
     */
    private final Consumer3<K1, K2, V> removeFunc;

    public UtilMapMap() {
        this(((k1, k2, v) -> {
        }));
    }

    public UtilMapMap(Consumer3<K1, K2, V> removeFunc) {
        super((k1, k2Map) -> k2Map.forEach((k2, v) -> removeFunc.accept(k1, k2, v)));
        this.removeFunc = removeFunc;
    }

    public void removeValueIf(Predicate3<K1, K2, V> shouldRemove) {
        var toRemove = new UtilSetMap<K1, K2>();
        flatForEach((k1, k2, v) -> {
            if (shouldRemove.test(k1, k2, v))
                toRemove.add(k1, k2);
        });

        toRemove.flatForEach(this::removeValue);
    }

    public V removeValue(K1 k1, K2 k2) {
        var v = get(k1).remove(k2);
        removeFunc.accept(k1, k2, v);
        if (get(k1).isEmpty())
            remove(k1);
        return v;
    }
}
