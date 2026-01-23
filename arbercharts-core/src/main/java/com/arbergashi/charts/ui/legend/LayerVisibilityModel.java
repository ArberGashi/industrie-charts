package com.arbergashi.charts.ui.legend;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<String, Boolean> visibility = new ConcurrentHashMap<>();

    /** Returns whether the series is considered visible (default: {@code true}). */
    public boolean isVisible(String seriesId) {
        if (seriesId == null || seriesId.isBlank()) return true;
        return visibility.getOrDefault(seriesId, Boolean.TRUE);
    }

    /** Sets the series visibility. */
    public void setVisible(String seriesId, boolean visible) {
        if (seriesId == null || seriesId.isBlank()) return;
        visibility.put(seriesId, visible);
    }

    /** Toggles visibility and returns the new state. */
    public boolean toggle(String seriesId) {
        boolean next = !isVisible(seriesId);
        setVisible(seriesId, next);
        return next;
    }

    /** Clears all overrides (all series visible by default). */
    public void clear() {
        visibility.clear();
    }
}
