package com.arbergashi.charts.model;
/**
 * Optional extension for box plot models that provide explicit outliers.
 *
 * <p>Outliers are expected to be raw values in the same Y-units as the box plot.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public interface BoxPlotOutlierModel extends ChartModel {

    /**
     * Returns the outlier values for the given box plot index.
     *
     * @param index box plot index
     * @return array of outlier values (may be empty or null)
     */
    double[] getOutliers(int index);
}
