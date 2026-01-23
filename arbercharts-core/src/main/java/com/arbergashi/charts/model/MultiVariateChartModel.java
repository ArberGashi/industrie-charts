package com.arbergashi.charts.model;

import java.util.List;

/**
 * An extension of ChartModel for multivariate data, where each data point
 * consists of multiple values across different dimensions. Required for
 * charts like Parallel Coordinates.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * @see ChartModel
 */
public interface MultiVariateChartModel extends ChartModel {

    /**
     * Returns the labels for each dimension (or axis).
     */
    List<String> getDimensionLabels();

    /**
     * Returns the list of all multivariate data points.
     */
    List<? extends MultiVariatePoint> getMultiVariatePoints();

    /**
     * Represents a single data item in a multivariate dataset.
     */
    interface MultiVariatePoint {
        /**
         * Returns the values for this data point across all dimensions.
         * The order of values should correspond to the order of dimension labels.
         */
        double[] getValues();
    }
}
