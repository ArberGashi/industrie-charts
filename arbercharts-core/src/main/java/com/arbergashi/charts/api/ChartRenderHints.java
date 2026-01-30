package com.arbergashi.charts.api;

/**
 * Rendering hint container for chart rendering.
 *
 * <p>Provides a minimal API for anti-aliasing and stroke width control.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class ChartRenderHints {

    private boolean antialiasing = true;
    private Float strokeWidth = null;

    /**
     * Enables or disables anti-aliasing.
     */
    public ChartRenderHints setAntialiasing(boolean enabled) {
        this.antialiasing = enabled;
        return this;
    }

    /**
     * Returns whether anti-aliasing is enabled.
     */
    public boolean isAntialiasing() {
        return antialiasing;
    }

    /**
     * Sets the preferred base stroke width in design units (dp).
     * The framework will scale this value via {@link com.arbergashi.charts.util.ChartScale#scale(float)}.
     */
    public ChartRenderHints setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
        return this;
    }

    /**
     * Returns the preferred stroke width (design units), or null if not set.
     */
    public Float getStrokeWidth() {
        return strokeWidth;
    }
}
