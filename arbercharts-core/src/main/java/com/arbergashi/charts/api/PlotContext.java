package com.arbergashi.charts.api;

import com.arbergashi.charts.core.geometry.ArberRect;
/**
 * Context for drawing a plot.
 * Defines the visible data range and screen boundaries.
 * Used for coordinate transformations between data and pixel space.
 *
 * <p><b>Framework note:</b> A {@link ChartTheme} may be provided by the chart panel.
 * Code should prefer {@link #getTheme()} over any global/static theme access.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2.0.0
 */
public interface PlotContext {

    /**
     * Minimum visible X value (data coordinates).
     */
    double getMinX();

    /**
     * Maximum visible X value (data coordinates).
     */
    double getMaxX();

    /**
     * Minimum visible Y value (data coordinates).
     */
    double getMinY();

    /**
     * Maximum visible Y value (data coordinates).
     */
    double getMaxY();

    /**
     * The pixel bounds of the plotting area.
     */
    ArberRect getPlotBounds();

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
        return getMaxX() - getMinX();
    }

    /**
     * Height of the data range.
     */
    default double rangeY() {
        return getMaxY() - getMinY();
    }

    /**
     * Allocation-free inverse transform: pixel -> data coordinates.
     * Writes the result into dest[0]=dataX, dest[1]=dataY.
     */
    void mapToData(double pixelX, double pixelY, double[] dest);


    /**
     * Scale mode for X axis (used by helpers such as NiceScale).
     */
    default com.arbergashi.charts.util.NiceScale.ScaleMode getScaleModeX() {
        return com.arbergashi.charts.util.NiceScale.ScaleMode.LINEAR;
    }

    /**
     * Scale mode for Y axis (used by helpers such as NiceScale).
     */
    default com.arbergashi.charts.util.NiceScale.ScaleMode getScaleModeY() {
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
    default ChartRenderHints getRenderHints() {
        return null;
    }

    /**
     * Indicates whether the current data stream is considered lost/stale.
     */
    default boolean isSignalLost() {
        return false;
    }

    /**
     * Optional gap model for non-trading periods on the X axis.
     */
    default AxisGapModel getGapModel() {
        return null;
    }

    /**
     * Theme associated with the current rendering context.
     *
     * <p><b>Framework contract:</b> This must be non-null during rendering.
     * Implementations that do not manage per-panel themes should return a stable default
     * such as {@link ChartThemes#getDarkTheme()}.</p>
     */
    default ChartTheme getTheme() {
        return ChartThemes.getDarkTheme();
    }

    /**
     * Animation profile active for this render pass.
     */
    default AnimationProfile getAnimationProfile() {
        return AnimationProfile.ACADEMIC;
    }

    /**
     * Snap a pixel coordinate to a crisp device pixel boundary.
     *
     * <p>Policy: grids and thin strokes align to half-pixel coordinates to avoid
     * blurry lines when using 1px strokes.</p>
     *
     * @param pixel pixel-space coordinate
     * @return snapped pixel coordinate (n + 0.5)
     */
    default double snapPixel(double pixel) {
        return Math.floor(pixel) + 0.5;
    }
}
