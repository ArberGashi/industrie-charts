package com.arbergashi.charts.model;

import java.util.List;

/**
 * An extension of ChartModel for 3-component data, required for Ternary Plots.
 * Each data point represents a composition of three parts that sum to a constant.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * @see ChartModel
 */
public interface TernaryChartModel extends ChartModel {

    /**
     * Returns the data as a list of ternary points.
     *
     * @return A list of TernaryPoint objects.
     */
    List<TernaryPoint> getTernaryData();

    /**
     * Returns the labels for the three components (corners of the triangle).
     *
     * @return A list of three labels for components A, B, and C.
     */
    List<String> getComponentLabels();

    /**
     * Represents a single 3-component data point.
     */
    interface TernaryPoint {
        double getA(); // Component A

        double getB(); // Component B

        double getC(); // Component C
    }
}
