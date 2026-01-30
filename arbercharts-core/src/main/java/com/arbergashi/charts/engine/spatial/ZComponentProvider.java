package com.arbergashi.charts.engine.spatial;

/**
 * Pluggable Z-component provider for spatial mapping.
 *
 * <p>Designed for zero-allocation hot paths; implementations should avoid boxing.</p>
 *
 * @since 1.7.0
 */
@FunctionalInterface
public interface ZComponentProvider {
    /**
     * Calculates the Z component for the given data point.
     *
     * @param index index of the data point
     * @param x     raw X value
     * @param y     raw Y value
     * @param weight optional weight value (may be 0.0 if not available)
     * @return z component to store in the spatial buffer
     */
    double getCalculatedZ(int index, double x, double y, double weight);
}
