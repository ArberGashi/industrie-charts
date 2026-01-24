package com.arbergashi.charts.util;

import java.awt.Color;
/**
 * Flyweight cache for immutable {@link Color} instances.
 *
 * <p>Returned colors are cached and must be treated as immutable. Use this registry
 * to avoid per-frame allocations in render loops.</p>
 */
public final class ColorRegistry {

    private static final Object LOCK = new Object();
    private static int[] keys = new int[1024];
    private static Color[] values = new Color[1024];
    private static boolean[] used = new boolean[1024];
    private static int size = 0;

    private ColorRegistry() {
    }

    public static Color of(int r, int g, int b, int a) {
        int rr = clamp8(r);
        int gg = clamp8(g);
        int bb = clamp8(b);
        int aa = clamp8(a);
        int argb = (aa << 24) | (rr << 16) | (gg << 8) | bb;
        return getOrCreate(argb);
    }

    public static Color ofArgb(int argb) {
        return getOrCreate(argb);
    }

    public static Color withAlpha(Color base, float alpha) {
        if (base == null) return null;
        int a = (int) (Math.clamp(alpha, 0f, 1f) * 255);
        return of(base.getRed(), base.getGreen(), base.getBlue(), a);
    }

    public static Color interpolate(Color a, Color b, float t) {
        if (a == null || b == null) return (a != null ? a : b);
        t = Math.min(1f, Math.max(0f, t));
        int ar = a.getRed(), ag = a.getGreen(), ab = a.getBlue(), aa = a.getAlpha();
        int br = b.getRed(), bg = b.getGreen(), bb = b.getBlue(), ba = b.getAlpha();
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        int al = (int) (aa + (ba - aa) * t);
        return of(r, g, bl, al);
    }

    public static Color adjustBrightness(Color c, double factor) {
        if (c == null) return null;
        int r = (int) Math.min(255, Math.max(0, Math.round(c.getRed() * factor)));
        int g = (int) Math.min(255, Math.max(0, Math.round(c.getGreen() * factor)));
        int b = (int) Math.min(255, Math.max(0, Math.round(c.getBlue() * factor)));
        return of(r, g, b, c.getAlpha());
    }

    private static int clamp8(int v) {
        return Math.min(255, Math.max(0, v));
    }

    private static Color getOrCreate(int argb) {
        synchronized (LOCK) {
            int mask = keys.length - 1;
            int idx = smear(argb) & mask;
            while (used[idx]) {
                if (keys[idx] == argb) {
                    return values[idx];
                }
                idx = (idx + 1) & mask;
            }
            if ((size + 1) * 4 >= keys.length * 3) {
                resize();
                mask = keys.length - 1;
                idx = smear(argb) & mask;
                while (used[idx]) {
                    if (keys[idx] == argb) {
                        return values[idx];
                    }
                    idx = (idx + 1) & mask;
                }
            }
            Color created = new Color(argb, true);
            used[idx] = true;
            keys[idx] = argb;
            values[idx] = created;
            size++;
            return created;
        }
    }

    private static void resize() {
        int newSize = keys.length << 1;
        int[] oldKeys = keys;
        Color[] oldValues = values;
        boolean[] oldUsed = used;
        keys = new int[newSize];
        values = new Color[newSize];
        used = new boolean[newSize];
        size = 0;
        int mask = newSize - 1;
        for (int i = 0; i < oldKeys.length; i++) {
            if (!oldUsed[i]) continue;
            int key = oldKeys[i];
            Color value = oldValues[i];
            int idx = smear(key) & mask;
            while (used[idx]) {
                idx = (idx + 1) & mask;
            }
            used[idx] = true;
            keys[idx] = key;
            values[idx] = value;
            size++;
        }
    }

    private static int smear(int hash) {
        int h = hash;
        h ^= (h >>> 16);
        h *= 0x7feb352d;
        h ^= (h >>> 15);
        h *= 0x846ca68b;
        h ^= (h >>> 16);
        return h;
    }
}
