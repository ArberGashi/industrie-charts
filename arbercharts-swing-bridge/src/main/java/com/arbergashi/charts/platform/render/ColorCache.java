package com.arbergashi.charts.platform.render;

import java.awt.Color;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Lightweight ARGB -> Color cache for Swing rendering.
 */
public final class ColorCache {
    private static final Map<Integer, Color> CACHE = new ConcurrentHashMap<>();

    private ColorCache() {
    }

    public static Color get(int argb) {
        return CACHE.computeIfAbsent(argb, Color::new);
    }
}
