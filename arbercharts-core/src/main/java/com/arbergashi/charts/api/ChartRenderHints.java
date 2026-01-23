package com.arbergashi.charts.api;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;

/**
 * Rendering hint container for chart rendering.
 *
 * <p>Provides a minimal API for anti-aliasing and stroke width control,
 * while allowing extensible rendering hints via {@link #setHint}.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class ChartRenderHints {

    private boolean antialiasing = true;
    private Float strokeWidth = null;
    private final Map<RenderingHints.Key, Object> extraHints = new HashMap<>();

    /**
     * Enables or disables anti-aliasing.
     */
    public ChartRenderHints setAntialiasing(boolean enabled) {
        this.antialiasing = enabled;
        return this;
    }

    /**
     * Fluent alias for {@link #setAntialiasing(boolean)}.
     */
    public ChartRenderHints antialiasing(boolean enabled) {
        return setAntialiasing(enabled);
    }

    /**
     * Sets the preferred base stroke width in design units (dp).
     * The framework will scale this value via {@link ChartScale#scale(float)}.
     */
    public ChartRenderHints setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
        return this;
    }

    /**
     * Fluent alias for {@link #setStrokeWidth(float)}.
     */
    public ChartRenderHints strokeWidth(float strokeWidth) {
        return setStrokeWidth(strokeWidth);
    }

    /**
     * Sets an arbitrary rendering hint for extensibility.
     */
    public ChartRenderHints setHint(RenderingHints.Key key, Object value) {
        if (key == null) return this;
        if (value == null) {
            extraHints.remove(key);
        } else {
            extraHints.put(key, value);
        }
        return this;
    }

    /**
     * Returns the preferred stroke width (design units), or null if not set.
     */
    public Float getStrokeWidth() {
        return strokeWidth;
    }

    /**
     * Applies the configured hints to the graphics context.
     */
    public void applyTo(Graphics2D g2) {
        if (g2 == null) return;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                antialiasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        for (Map.Entry<RenderingHints.Key, Object> e : extraHints.entrySet()) {
            g2.setRenderingHint(e.getKey(), e.getValue());
        }
    }
}
