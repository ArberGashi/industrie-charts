package com.arbergashi.charts.tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * Cache for expensive renderer allocations, such as BufferedImage, Path2D, etc.
 * Uses weak references to avoid memory leaks and allows for efficient reuse of resources.
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

    public static Stroke getStroke(Object owner, String key, Supplier<Stroke> factory) {
        Map<String, Object> m = mapFor(owner);
        Stroke s = (Stroke) m.get(key);
        if (s == null) {
            s = factory.get();
            m.put(key, s);
        }
        return s;
    }

    public static Stroke getBasicStroke(Object owner, String key, float width, int cap, int join, float miter, float[] dash, float phase) {
        Map<String, Object> m = mapFor(owner);
        Stroke s = (Stroke) m.get(key);
        if (s == null) {
            s = new BasicStroke(width, cap, join, miter, dash, phase);
            m.put(key, s);
        }
        return s;
    }

    public static java.awt.Font getFont(Object owner, String key, Supplier<java.awt.Font> factory) {
        Map<String, Object> m = mapFor(owner);
        java.awt.Font f = (java.awt.Font) m.get(key);
        if (f == null) {
            f = factory.get();
            m.put(key, f);
        }
        return f;
    }

    public static Color getColor(Object owner, String key, int r, int g, int b) {
        return getColor(owner, key, r, g, b, 255);
    }

    public static Color getColor(Object owner, String key, int r, int g, int b, int a) {
        Map<String, Object> m = mapFor(owner);
        Color c = (Color) m.get(key);
        if (c == null) {
            c = new Color(r, g, b, a);
            m.put(key, c);
        }
        return c;
    }

    public static java.awt.Font getFont(Object owner, String key, String name, int style, int size) {
        Map<String, Object> m = mapFor(owner);
        java.awt.Font f = (java.awt.Font) m.get(key);
        if (f == null) {
            f = new java.awt.Font(name, style, size);
            m.put(key, f);
        }
        return f;
    }

    public static Path2D getPath(Object owner, String key) {
        Map<String, Object> m = mapFor(owner);
        Path2D p = (Path2D) m.get(key);
        if (p == null) {
            p = new Path2D.Double();
            m.put(key, p);
        } else {
            p.reset();
        }
        return p;
    }

    public static AffineTransform getAffineTransform(Object owner, String key) {
        Map<String, Object> m = mapFor(owner);
        AffineTransform t = (AffineTransform) m.get(key);
        if (t == null) {
            t = new AffineTransform();
            m.put(key, t);
        } else {
            t.setToIdentity();
        }
        return t;
    }

    public static Rectangle2D getRectangle(Object owner, String key) {
        Map<String, Object> m = mapFor(owner);
        Rectangle2D r = (Rectangle2D) m.get(key);
        if (r == null) {
            r = new Rectangle2D.Double();
            m.put(key, r);
        }
        return r;
    }

    public static BufferedImage getBufferedImage(Object owner, String key, int w, int h, int type) {
        Map<String, Object> m = mapFor(owner);
        BufferedImage b = (BufferedImage) m.get(key);
        if (b == null || b.getWidth() != w || b.getHeight() != h || b.getType() != type) {
            b = new BufferedImage(w, h, type);
            m.put(key, b);
        }
        return b;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> java.util.Map<K, V> getMap(Object owner, String key) {
        Map<String, Object> m = mapFor(owner);
        java.util.Map<K, V> mm = (java.util.Map<K, V>) m.get(key);
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
