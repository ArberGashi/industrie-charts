package com.arbergashi.charts.internal;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight cache for AlphaComposite instances to avoid allocations in tight render loops.
 * Uses a ConcurrentHashMap to allow thread-safe access from worker threads.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class CompositePool {
    private static final ConcurrentHashMap<Integer, AlphaComposite> CACHE = new ConcurrentHashMap<>();

    private CompositePool() {
    }

    /**
     * Returns an AlphaComposite corresponding to the given alpha in the range [0,1].
     * For stability and cache hits we quantize alpha to 0..255 integer steps.
     */
    public static Composite get(float alpha) {
        if (alpha <= 0f) return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0f);
        if (alpha >= 1f) return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f);
        int key = Math.round(alpha * 255f);
        return CACHE.computeIfAbsent(key, k -> AlphaComposite.getInstance(AlphaComposite.SRC_OVER, k / 255f));
    }
}

