package com.arbergashi.charts.api;

import com.arbergashi.charts.model.ChartModel;

import java.awt.*;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the current "focus" state of a chart interaction.
 *
 * <p>Focus is typically driven by the crosshair/mouse position and is used to power
 * interactive legends, tooltips and synchronized multi-panel cursors.</p>
 *
 * <p>This is intentionally a small immutable value object. Renderers may contribute additional
 * values (e.g., OHLC, volume, indicators) via {@link #values()}.</p>
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public record ChartFocus(
        /** Mouse/crosshair position in component coordinates (may be {@code null} when not focused). */
        Point pixel,
        /** X value in data space (domain). */
        double x,
        /** Y value in data space (range). */
        double y,
        /** Focused point index if resolved (otherwise -1). */
        int index,
        /** The model that provided the focused point (may be {@code null} if only mapped from pixel). */
        ChartModel model,
        /** Optional timestamp representation for time-series charts. */
        Instant timestamp,
        /** Renderer/model provided values for UI (e.g., Open, High, Low, Close, Volume, MACD). */
        Map<String, Object> values
) {

    public static final ChartFocus EMPTY = new ChartFocus(null, Double.NaN, Double.NaN, -1, null, null, Map.of());

    public ChartFocus {
        values = values != null ? Collections.unmodifiableMap(new LinkedHashMap<>(values)) : Map.of();
    }

    /**
     * Returns whether the focus represents a valid, active cursor.
     *
     * @return true if pixel/x/y are set to concrete values.
     */
    public boolean isActive() {
        return pixel != null && !Double.isNaN(x) && !Double.isNaN(y);
    }

    /**
     * Returns a focus value by key (renderer/model-supplied).
     *
     * @param key value key
     * @return the value or null if missing
     */
    public Object value(String key) {
        if (key == null) return null;
        return values.get(key);
    }

    /**
     * Returns a new focus instance with an additional value.
     *
     * @param key value key (non-null)
     * @param value value to add
     * @return a new {@link ChartFocus} instance
     */
    public ChartFocus withValue(String key, Object value) {
        Objects.requireNonNull(key, "key");
        Map<String, Object> map = new LinkedHashMap<>(values);
        map.put(key, value);
        return new ChartFocus(pixel, x, y, index, model, timestamp, map);
    }
}
