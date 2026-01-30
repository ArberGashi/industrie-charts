package com.arbergashi.charts.engine.spatial;

/**
 * Strategy for handling Z clipping decisions in spatial pipelines.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 1.7.0
 */
@FunctionalInterface
public interface ClippingStrategy {
    /**
     * Returns true when the point is visible for the given zMin.
     *
     * @param z    point depth
     * @param zMin near-plane threshold
     * @return true if the point should be kept
     */
    boolean isVisible(double z, double zMin);

    /**
     * Returns a transformed Z value after clipping (default: pass-through).
     *
     * @param z    original depth
     * @param zMin near-plane threshold
     * @return depth to store
     */
    default double getCalculatedZ(double z, double zMin) {
        return z;
    }
}
