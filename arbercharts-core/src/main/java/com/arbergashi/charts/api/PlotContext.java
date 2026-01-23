package com.arbergashi.charts.api;

import java.awt.geom.Rectangle2D;

/**
 * Context for drawing a plot.
 * Defines the visible data range and screen boundaries.
 * Used for coordinate transformations between data and pixel space.
 *
 * <p><b>Framework note:</b> A {@link ChartTheme} may be provided by the chart panel.
 * Code should prefer {@link #theme()} over any global/static theme access.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-15
 */
public interface PlotContext {

    /**
     * Minimum visible X value (data coordinates).
     */
    double minX();

    /**
     * Maximum visible X value (data coordinates).
     */
    double maxX();

    /**
     * Minimum visible Y value (data coordinates).
     */
    double minY();

    /**
     * Maximum visible Y value (data coordinates).
     */
    double maxY();

    /**
     * The pixel bounds of the plotting area.
     */
    Rectangle2D plotBounds();

    /**
     * Transforms data coordinates into pixel coordinates.
     *
     * <p><b>Zero-Allocation:</b> The result is written into the provided array.</p>
     *
     * @param x   X value (data)
     * @param y   Y value (data)
     * @param out Array of length at least 2. out[0] = pixelX, out[1] = pixelY.
     */
    void mapToPixel(double x, double y, double[] out);

    /**
     * Width of the data range.
     */
    default double rangeX() {
        return maxX() - minX();
    }

    /**
     * Height of the data range.
     */
    default double rangeY() {
        return maxY() - minY();
    }

    /**
     * Allocation-free inverse transform: pixel -> data coordinates.
     * Writes the result into dest[0]=dataX, dest[1]=dataY.
     */
    void mapToData(double pixelX, double pixelY, double[] dest);


    /**
     * Scale mode for X axis (used by helpers such as NiceScale).
     */
    default com.arbergashi.charts.util.NiceScale.ScaleMode scaleModeX() {
        return com.arbergashi.charts.util.NiceScale.ScaleMode.LINEAR;
    }

    /**
     * Scale mode for Y axis (used by helpers such as NiceScale).
     */
    default com.arbergashi.charts.util.NiceScale.ScaleMode scaleModeY() {
        return com.arbergashi.charts.util.NiceScale.ScaleMode.LINEAR;
    }

    /**
     * Whether the Y axis uses a logarithmic scale.
     */
    default boolean isLogarithmicY() {
        return false;
    }

    /**
     * Whether the X axis is inverted (max-to-min).
     */
    default boolean isInvertedX() {
        return false;
    }

    /**
     * Whether the Y axis is inverted (max-to-min).
     */
    default boolean isInvertedY() {
        return false;
    }

    /**
     * Rendering hints for the current render pass.
     */
    default ChartRenderHints renderHints() {
        return null;
    }

    /**
     * Theme associated with the current rendering context.
     *
     * <p><b>Framework contract:</b> This must be non-null during rendering.
     * Implementations that do not manage per-panel themes should return a stable default
     * such as {@link ChartThemes#defaultDark()}.</p>
     */
    default ChartTheme theme() {
        return ChartThemes.defaultDark();
    }
}
