package com.arbergashi.charts.api;

import com.arbergashi.charts.core.geometry.ArberRect;
/**
 * Standard {@link PlotContext} implementation for Cartesian coordinate systems.
 *
 * <p>This context is optimized for rendering performance. It precomputes linear scaling factors
 * so {@link #mapToPixel(double, double, double[])} and {@link #mapToData(double, double, double[])}
 * run in constant time with no intermediate allocations.</p>
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2.0.0
 */
public class CartesianPlotContext implements PlotContext {
    private final ArberRect bounds;
    private final double minX, maxX, minY, maxY;
    private final double scaleX, scaleY;

    /**
     * Creates a new context for the given data range and plot bounds.
     *
     * @param bounds the plot area in pixel coordinates
     * @param minX   minimum visible X value (data coordinates)
     * @param maxX   maximum visible X value (data coordinates)
     * @param minY   minimum visible Y value (data coordinates)
     * @param maxY   maximum visible Y value (data coordinates)
     * @throws IllegalArgumentException if {@code minX >= maxX} or {@code minY >= maxY}
     */
    public CartesianPlotContext(ArberRect bounds, double minX, double maxX, double minY, double maxY) {
        this.bounds = bounds;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        double rx = maxX - minX;
        double ry = maxY - minY;
        this.scaleX = (rx == 0) ? 0 : bounds.width() / rx;
        this.scaleY = (ry == 0) ? 0 : bounds.height() / ry;
    }

    /**
     * Maps data coordinates to pixel coordinates.
     *
     * <p><b>Allocation-free:</b> writes the result to {@code dest} where dest[0] is X and dest[1] is Y.</p>
     *
     * @param dataX X in data coordinates
     * @param dataY Y in data coordinates
     * @param dest  output array of length at least 2
     */
    @Override
    public void mapToPixel(double dataX, double dataY, double[] dest) {
        // Linear transformation: pixel = offset + (data - min) * scale
        // NOTE: Swing Y-axis is inverted (0 is top)
        dest[0] = bounds.x() + (dataX - minX) * scaleX;
        dest[1] = (bounds.y() + bounds.height()) - (dataY - minY) * scaleY;
    }

    /**
     * Maps pixel coordinates to data coordinates.
     *
     * <p><b>Allocation-free:</b> writes the result to {@code dest} where dest[0] is X and dest[1] is Y.</p>
     *
     * @param pixelX X in pixel coordinates
     * @param pixelY Y in pixel coordinates
     * @param dest   output array of length at least 2
     */
    @Override
    public void mapToData(double pixelX, double pixelY, double[] dest) {
        // Inverse of the linear transform used in mapToPixel
        dest[0] = minX + (pixelX - bounds.x()) / scaleX;
        dest[1] = minY + ((bounds.y() + bounds.height()) - pixelY) / scaleY;
    }

    /**
     * Returns the plot bounds in pixel coordinates.
     */
    @Override
    public ArberRect getPlotBounds() {
        return bounds;
    }

    /**
     * @return minimum visible X value (data coordinates)
     */
    @Override
    public double getMinX() {
        return minX;
    }

    /**
     * @return maximum visible X value (data coordinates)
     */
    @Override
    public double getMaxX() {
        return maxX;
    }

    /**
     * @return minimum visible Y value (data coordinates)
     */
    @Override
    public double getMinY() {
        return minY;
    }

    /**
     * @return maximum visible Y value (data coordinates)
     */
    @Override
    public double getMaxY() {
        return maxY;
    }
}
