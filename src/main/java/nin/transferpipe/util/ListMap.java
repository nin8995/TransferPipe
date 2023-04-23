package nin.transferpipe.util;

import org.antlr.v4.misc.OrderedHashMap;

public class ListMap<K, V> extends OrderedHashMap<K, V> {

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
