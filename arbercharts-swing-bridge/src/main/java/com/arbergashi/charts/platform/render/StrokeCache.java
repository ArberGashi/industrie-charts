package com.arbergashi.charts.platform.render;

import java.awt.BasicStroke;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Simple stroke cache keyed by width.
 */
public final class StrokeCache {
    private static final Map<Float, BasicStroke> CACHE = new ConcurrentHashMap<>();

    private StrokeCache() {
    }

    public static BasicStroke get(float width) {
        float w = (Float.isFinite(width) && width > 0f) ? width : 1f;
        return CACHE.computeIfAbsent(w, BasicStroke::new);
    }
}
