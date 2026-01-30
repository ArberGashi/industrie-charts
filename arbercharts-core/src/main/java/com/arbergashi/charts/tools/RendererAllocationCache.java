package com.arbergashi.charts.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Core-safe cache for renderer allocations (primitive arrays, maps, lists).
 *
 * <p>Intentionally excludes any UI-framework types. Any AWT/Swing/Skia-specific
 * caches must live in bridge modules.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class RendererAllocationCache {
    private static final WeakHashMap<Object, Map<String, Object>> CACHE = new WeakHashMap<>();

    private RendererAllocationCache() {}

    private static synchronized Map<String, Object> mapFor(Object owner) {
        return CACHE.computeIfAbsent(owner, k -> new HashMap<>());
    }

    public static double[] getDoubleArray(Object owner, String key, int minLen) {
        Map<String, Object> m = mapFor(owner);
        double[] a = (double[]) m.get(key);
        if (a == null || a.length < minLen) {
            a = new double[Math.max(minLen, a == null ? 1 : a.length * 2)];
            m.put(key, a);
        }
        return a;
    }

    public static int[] getIntArray(Object owner, String key, int minLen) {
        Map<String, Object> m = mapFor(owner);
        int[] a = (int[]) m.get(key);
        if (a == null || a.length < minLen) {
            a = new int[Math.max(minLen, a == null ? 1 : a.length * 2)];
            m.put(key, a);
        }
        return a;
    }

    public static float[] getFloatArray(Object owner, String key, int minLen) {
        Map<String, Object> m = mapFor(owner);
        float[] a = (float[]) m.get(key);
        if (a == null || a.length < minLen) {
            a = new float[Math.max(minLen, a == null ? 1 : a.length * 2)];
            m.put(key, a);
        }
        return a;
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getList(Object owner, String key) {
        Map<String, Object> m = mapFor(owner);
        List<T> l = (List<T>) m.get(key);
        if (l == null) {
            l = new ArrayList<>();
            m.put(key, l);
        }
        l.clear();
        return l;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> getMap(Object owner, String key) {
        Map<String, Object> m = mapFor(owner);
        Map<K, V> mm = (Map<K, V>) m.get(key);
        if (mm == null) {
            mm = new java.util.LinkedHashMap<>();
            m.put(key, mm);
        }
        mm.clear();
        return mm;
    }

    public static Object getArray(Object owner, String key, Class<?> componentType, int minLen) {
        Map<String, Object> m = mapFor(owner);
        Object arr = m.get(key);
        int len = 0;
        if (arr != null) len = java.lang.reflect.Array.getLength(arr);
        if (arr == null || len < minLen) {
            Object n = java.lang.reflect.Array.newInstance(componentType, Math.max(minLen, len == 0 ? 1 : len * 2));
            m.put(key, n);
            return n;
        }
        return arr;
    }
}
