package com.arbergashi.charts.util;

import java.util.LinkedHashMap;
import java.util.Map;
/**
 * Utility for bounded LRU caches.
 *
 * <p>This prevents memory leaks caused by unbounded caches in long-running applications.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class CacheUtils {

    private CacheUtils() {
    }

    /**
     * Creates a bounded LRU cache with a fixed maximum size.
     *
     * <p>Uses {@link LinkedHashMap} for automatic eviction of the eldest entry.
     *
     * @param maxEntries maximum number of entries (e.g. 64 for gradient caches)
     * @param <K>        key type
     * @param <V>        value type
     * @return a map instance with LRU behavior
     */
    public static <K, V> Map<K, V> newBoundedMap(int maxEntries) {
        return new LinkedHashMap<K, V>() {
            private static final long serialVersionUID = 1L;
            @Override/**
 * @since 1.5.0
 */
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        };
    }
}
