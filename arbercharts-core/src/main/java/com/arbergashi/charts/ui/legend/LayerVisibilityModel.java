package com.arbergashi.charts.ui.legend;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds per-series visibility state.
 *
 * <p>This model is intentionally lightweight and thread-safe. Swing users typically
 * interact with it on the EDT, but renderers and overlays may consult it during paint.
 * Series ids usually come from {@code BaseRenderer#getId()}.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class LayerVisibilityModel {

    private final Map<String, Integer> indexMap = new ConcurrentHashMap<>();
    private final AtomicLong visibilityMask = new AtomicLong(0L);
    private final AtomicReference<String> soloId = new AtomicReference<>(null);
    private final AtomicInteger nextIndex = new AtomicInteger(0);

    /** Returns whether the series is considered visible (default: {@code true}). */
    public boolean isVisible(String seriesId) {
        if (seriesId == null || seriesId.isBlank()) return true;
        int idx = indexMap.getOrDefault(seriesId, -1);
        if (idx < 0 || idx >= Long.SIZE) return true;
        long mask = visibilityMask.get();
        return (mask & (1L << idx)) != 0L;
    }

    /** Sets the series visibility. */
    public void setVisible(String seriesId, boolean visible) {
        if (seriesId == null || seriesId.isBlank()) return;
        int idx = ensureIndex(seriesId);
        if (idx >= Long.SIZE) return;
        long bit = 1L << idx;
        long prev;
        long next;
        do {
            prev = visibilityMask.get();
            next = visible ? (prev | bit) : (prev & ~bit);
        } while (!visibilityMask.compareAndSet(prev, next));
    }

    /** Toggles visibility and returns the new state. */
    public boolean toggle(String seriesId) {
        boolean next = !isVisible(seriesId);
        setVisible(seriesId, next);
        return next;
    }

    /** Clears all overrides (all series visible by default). */
    public void clear() {
        visibilityMask.set(0L);
        indexMap.clear();
        soloId.set(null);
        nextIndex.set(0);
    }

    /** Sets the series as solo (others remain visible but can be dimmed). */
    public void setSolo(String seriesId) {
        if (seriesId == null || seriesId.isBlank()) {
            soloId.set(null);
        } else {
            soloId.set(seriesId);
        }
    }

    /** Clears solo mode. */
    public void clearSolo() {
        soloId.set(null);
    }

    /** Returns true when solo mode is active. */
    public boolean isSoloActive() {
        return soloId.get() != null;
    }

    /** Returns true when the series should be dimmed due to solo mode. */
    public boolean isDimmed(String seriesId) {
        String solo = soloId.get();
        return solo != null && seriesId != null && !solo.equals(seriesId);
    }

    private int ensureIndex(String seriesId) {
        Integer existing = indexMap.get(seriesId);
        if (existing != null) return existing;
        int next = nextIndex.getAndIncrement();
        Integer prior = indexMap.putIfAbsent(seriesId, next);
        int idx = (prior != null) ? prior : next;
        if (idx < Long.SIZE) {
            long bit = 1L << idx;
            long prev;
            long updated;
            do {
                prev = visibilityMask.get();
                updated = prev | bit;
            } while (!visibilityMask.compareAndSet(prev, updated));
        }
        return idx;
    }
}
