package com.arbergashi.charts.util;

import com.arbergashi.charts.api.types.ArberColor;
/**
 * Flyweight cache for immutable {@link ArberColor} instances.
 *
 * <p>Returned colors are cached and must be treated as immutable. Use this registry
 * to avoid per-frame allocations in render loops.</p>
 * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class ColorRegistry {

    private static final Object LOCK = new Object();
    private static int[] keys = new int[1024];
    private static ArberColor[] values = new ArberColor[1024];
    private static boolean[] used = new boolean[1024];
    private static int size = 0;

    private ColorRegistry() {
    }

    public static ArberColor of(int r, int g, int b, int a) {
        int rr = clamp8(r);
        int gg = clamp8(g);
        int bb = clamp8(b);
        int aa = clamp8(a);
        int argb = (aa << 24) | (rr << 16) | (gg << 8) | bb;
        return getOrCreate(argb);
    }

    public static ArberColor ofArgb(int argb) {
        return getOrCreate(argb);
    }

    public static ArberColor applyAlpha(ArberColor base, float alpha) {
        if (base == null) return null;
        int a = (int) (Math.clamp(alpha, 0f, 1f) * 255);
        return of(base.red(), base.green(), base.blue(), a);
    }

    public static ArberColor interpolate(ArberColor a, ArberColor b, float t) {
        if (a == null || b == null) return (a != null ? a : b);
        t = Math.min(1f, Math.max(0f, t));
        int ar = a.red(), ag = a.green(), ab = a.blue(), aa = a.alpha();
        int br = b.red(), bg = b.green(), bb = b.blue(), ba = b.alpha();
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        int al = (int) (aa + (ba - aa) * t);
        return of(r, g, bl, al);
    }

    public static ArberColor adjustBrightness(ArberColor c, double factor) {
        if (c == null) return null;
        int r = (int) Math.min(255, Math.max(0, Math.round(c.red() * factor)));
        int g = (int) Math.min(255, Math.max(0, Math.round(c.green() * factor)));
        int b = (int) Math.min(255, Math.max(0, Math.round(c.blue() * factor)));
        return of(r, g, b, c.alpha());
    }

    private static int clamp8(int v) {
        return Math.min(255, Math.max(0, v));
    }

    private static ArberColor getOrCreate(int argb) {
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
            ArberColor created = new ArberColor(argb);
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
        ArberColor[] oldValues = values;
        boolean[] oldUsed = used;
        keys = new int[newSize];
        values = new ArberColor[newSize];
        used = new boolean[newSize];
        size = 0;
        int mask = newSize - 1;
        for (int i = 0; i < oldKeys.length; i++) {
            if (!oldUsed[i]) continue;
            int key = oldKeys[i];
            ArberColor value = oldValues[i];
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
