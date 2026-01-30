package com.arbergashi.charts.model;
/**
 * Specialized financial model for OHLC series.
 *
 * <p>Provides typed accessors for Open/High/Low/Close values in primitive arrays,
 * optimized for zero-allocation rendering in financial renderers.</p>
 *
 * @since 2025-06-01
  * @author Arber Gashi
  * @version 1.7.0
 */
public interface FinancialChartModel extends ChartModel {

    /**
     * Direct access to open values.
     */
    double[] getOpenData();

    /**
     * Direct access to high values.
     */
    @Override
    double[] getHighData();

    /**
     * Direct access to low values.
     */
    @Override
    double[] getLowData();

    /**
     * Direct access to close values.
     */
    double[] getCloseData();

    /**
     * Direct access to volume values (optional).
     */
    default double[] getVolumeData() {
        return ChartModel.EMPTY_DOUBLE;
    }

    /**
     * Returns the open value at the given index.
     */
    default double getOpen(int index) {
        double[] arr = getOpenData();
        if (arr == null || index < 0 || index >= arr.length) return 0.0;
        return arr[index];
    }

    /**
     * Returns the high value at the given index.
     */
    default double getHigh(int index) {
        double[] arr = getHighData();
        if (arr == null || index < 0 || index >= arr.length) return 0.0;
        return arr[index];
    }

    /**
     * Returns the low value at the given index.
     */
    default double getLow(int index) {
        double[] arr = getLowData();
        if (arr == null || index < 0 || index >= arr.length) return 0.0;
        return arr[index];
    }

    /**
     * Returns the close value at the given index.
     */
    default double getClose(int index) {
        double[] arr = getCloseData();
        if (arr == null || index < 0 || index >= arr.length) return 0.0;
        return arr[index];
    }

    /**
     * Returns the volume value at the given index.
     */
    default double getVolume(int index) {
        double[] arr = getVolumeData();
        if (arr == null || index < 0 || index >= arr.length) return 0.0;
        return arr[index];
    }

    @Override
    default double[] getYData() {
        return getCloseData();
    }

    @Override
    default double[] getWeightData() {
        return getOpenData();
    }

    @Override
    default double getMin(int index) {
        double[] arr = getLowData();
        if (arr == null || index < 0 || index >= arr.length) return 0.0;
        return arr[index];
    }

    @Override
    default double getMax(int index) {
        double[] arr = getHighData();
        if (arr == null || index < 0 || index >= arr.length) return 0.0;
        return arr[index];
    }
}
