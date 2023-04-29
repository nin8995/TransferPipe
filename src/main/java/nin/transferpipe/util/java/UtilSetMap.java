package nin.transferpipe.util.java;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class UtilSetMap<K, V> extends UtilMap<K, Set<V>> {

    /**
     * SetMapの取り扱い
     */
    public void add(K key, @Nullable V value) {
        if (containsKey(key)) {
            if (value != null)
                get(key).add(value);
        } else {
            var valueSet = new HashSet<V>();
            if (value != null)
                valueSet.add(value);
            put(key, valueSet);
        }
    }

    public void addAll(K key, Collection<V> values) {
        if (values.isEmpty())
            add(key, null);
        else
            values.forEach(v -> add(key, v));
    }

    @SafeVarargs
    public final void addAll(K key, V... values) {
        addAll(key, Arrays.stream(values).toList());
    }

    public boolean hasNext() {
        return size() != 0;
    }

    public void flatForEach(BiConsumer<K, V> func) {
        forEach((k1, valueMap) -> valueMap.forEach(value -> func.accept(k1, value)));
    }

    public boolean contains(K k, V v) {
        return containsKey(k) && get(k).contains(v);
    }

    /**
     * 削除機能拡張
     */
    private final BiConsumer<K, V> removeFunc;

    public UtilSetMap() {
        this((k, v) -> {
        });
    }

    public UtilSetMap(BiConsumer<K, V> removeFunc) {
        super((k, vSet) -> vSet.forEach(v -> removeFunc.accept(k, v)));
        this.removeFunc = removeFunc;
    }

    public void removeValueIf(BiPredicate<K, V> shouldRemove) {
        var toRemove = new HashMap<K, V>();
        flatForEach((k, v) -> {
            if (shouldRemove.test(k, v))
                toRemove.put(k, v);
        });

        toRemove.forEach(this::removeValue);
    }

    public V removeValue(K k, V v) {
        get(k).remove(v);
        removeFunc.accept(k, v);
        if (get(k).isEmpty())
            remove(k);
        return v;
    }
}
