package com.arbergashi.charts.api;
/**
 * Marks non-trading or skipped ranges on the X axis.
 * Used by grids and renderers to suppress ticks inside gaps.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public interface AxisGapModel {

    /**
     * Returns true if the provided x-value lies inside a gap.
     *
     * @param xValue data-space x value
     * @return true if the position is within a gap
     */
    boolean isGap(double xValue);
}
