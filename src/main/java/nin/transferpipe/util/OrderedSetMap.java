package nin.transferpipe.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class OrderedSetMap<K, V> extends ListMap<K, Set<V>> {

    public void add(K key, V value) {
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
        values.forEach(v -> add(key, v));
    }

    @SafeVarargs
    public final void addAll(K key, V... values) {
        addAll(key, Arrays.stream(values).toList());
    }

    public boolean hasNext() {
        return size() != 0;
    }
}
