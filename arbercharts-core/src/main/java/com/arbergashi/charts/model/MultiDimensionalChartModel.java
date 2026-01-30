package com.arbergashi.charts.model;

import java.util.List;
/**
 * An extension of ChartModel for multi-dimensional data, where each data point
 * is an array of values. Required for charts like Parallel Coordinates.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * @see ChartModel
 */
public interface MultiDimensionalChartModel extends ChartModel {

    /**
     * Returns the data as a list of multi-dimensional points.
     * Each point is represented by a double array.
     *
     * @return A list of double arrays.
     */
    List<double[]> getMultiDimensionalData();

    /**
     * Returns the labels for each dimension (axis).
     * The length of this list should match the length of the double arrays.
     *
     * @return A list of dimension labels.
     */
    List<String> getDimensionLabels();
}
