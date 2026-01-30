package com.arbergashi.charts.model;
/**
 * Specialized statistical model for distributions and quantiles.
 *
 * <p>Provides typed accessors for median, quartiles and whiskers while retaining
 * the {@link ChartModel} contract for renderers.</p>
 *
 * @since 2025-06-01
  * @author Arber Gashi
  * @version 1.7.0
 */
public interface StatisticalChartModel extends ChartModel {

    /**
     * Direct access to median values (mapped to Y by default).
     */
    default double[] getMedianData() {
        return getYData();
    }

    /**
     * Direct access to Q1 values.
     */
    double[] getQ1Data();

    /**
     * Direct access to Q3 values.
     */
    double[] getQ3Data();

    /**
     * Returns the median at the given index.
     */
    default double getMedian(int index) {
        double[] arr = getMedianData();
        if (arr == null || index < 0 || index >= arr.length) return 0.0;
        return arr[index];
    }

    /**
     * Returns the first quartile at the given index.
     */
    default double getQ1(int index) {
        double[] arr = getQ1Data();
        if (arr == null || index < 0 || index >= arr.length) return 0.0;
        return arr[index];
    }

    /**
     * Returns the third quartile at the given index.
     */
    default double getQ3(int index) {
        double[] arr = getQ3Data();
        if (arr == null || index < 0 || index >= arr.length) return 0.0;
        return arr[index];
    }

    /**
     * Returns interquartile range at the given index.
     */
    default double getIqr(int index) {
        return getQ3(index) - getQ1(index);
    }

    @Override
    default double getMin(int index) {
        return getLowData()[index];
    }

    @Override
    default double getMax(int index) {
        return getHighData()[index];
    }

    @Override
    default double getWeight(int index) {
        return getIqr(index);
    }
}
