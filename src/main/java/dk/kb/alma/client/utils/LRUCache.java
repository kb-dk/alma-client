package dk.kb.alma.client.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple LRUcache based on the LinkedHashMap
 *
 * @see LinkedHashMap
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private int initialCapacity;

    public LRUCache(int initialCapacity,
                    boolean accessOrder) {
        super(initialCapacity + 1, 0.75f, accessOrder);
        this.initialCapacity = initialCapacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        if (size() > initialCapacity) {
            return true;
        }
        return false;
    }
}


