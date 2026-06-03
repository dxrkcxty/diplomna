package util;

import java.util.*;

public class Cache<K, V> {
    private final long ttlMillis;
    private final Map<K, Entry<V>> map = new HashMap<>();
    public Cache(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }
    public synchronized V get(K key) {
        Entry<V> entry = map.get(key);
        if (entry == null) return null;
        if (System.currentTimeMillis() - entry.timestamp > ttlMillis) {
            map.remove(key);
            return null;
        }
        return entry.value;
    }
    public synchronized void put(K key, V value) {
        map.put(key, new Entry<>(value));
    }
    public synchronized void clear() {
        map.clear();
    }
    private static class Entry<V> {
        V value;
        long timestamp;
        Entry(V value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
