package com.arbergashi.charts.model;
/**
 * High-Performance Chart Model Interface.
 *
 * <p>Designed for Zero-Allocation Rendering. Instead of lists of objects,
 * primitive arrays are exposed. This enables CPU-cache-friendly
 * iterations and avoids boxing overhead.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public interface ChartModel {

    // Reused empty arrays to avoid per-call zero-length allocations
    double[] EMPTY_DOUBLE = new double[0];
    byte[] EMPTY_BYTE = new byte[0];
    short[] EMPTY_SHORT = new short[0];
    long[] EMPTY_LONG = new long[0];

    /**
     * Returns the title of the data series.
     *
     * @return Name of the series
     */
    String getName();

    /**
     * Updates the title of the data series.
     *
     * @param name new series name
     */
    default ChartModel setName(String name) {
        return this;
    }

    /**
     * Returns the number of data points.
     *
     * @return count
     */
    int getPointCount();

    /**
     * Direct access to the X-coordinate array.
     *
     * <p><b>Framework contract:</b> Implementations may return either:</p>
     * <ul>
     *   <li>a defensive copy sized to {@link #getPointCount()} (safe, but potentially slower), or</li>
     *   <li>a backing array with a capacity larger than {@link #getPointCount()} (fast, zero-allocation).</li>
     * </ul>
     *
     * <p>Consumers must never assume {@code getXData().length == getPointCount()}.
     * Always bound iteration by the logical size:</p>
     * <pre>
     * int n = Math.min(model.getPointCount(), model.getXData().length);
     * </pre>
     *
     * <p><b>Performance note:</b> If you need strict size and immutability semantics, use
     * a model implementation that returns copies (e.g. {@code DefaultChartModel}).</p>
     *
     * @return X values array (may be larger than logical size)
     */
    default double[] getXData() {
        return EMPTY_DOUBLE;
    }

    /**
     * Returns the X value at the given index.
     *
     * @param index data point index
     * @return X value or 0.0 if out of range
     */
    default double getX(int index) {
        double[] arr = getXData();
        if (arr == null || index < 0 || index >= arr.length) return 0.0;
        return arr[index];
    }

    /**
     * Direct access to the Y-coordinate array.
     * <p>Warning: For performance reasons, the internal array is often returned.
     * Do not modify!</p>
     *
     * @return double array of Y values
     */
    default double[] getYData() {
        return EMPTY_DOUBLE;
    }

    /**
     * Returns the Y value at the given index.
     *
     * @param index data point index
     * @return Y value or 0.0 if out of range
     */
    default double getY(int index) {
        double[] arr = getYData();
        if (arr == null || index < 0 || index >= arr.length) return 0.0;
        return arr[index];
    }

    /**
     * Returns the {minX, maxX, minY, maxY} data range based on available X/Y arrays.
     *
     * @return range array or {0,0,0,0} if data is empty
     */
    default double[] getDataRange() {
        double[] xs = getXData();
        double[] ys = getYData();
        if (xs == null || xs.length == 0 || ys == null || ys.length == 0) return new double[]{0, 0, 0, 0};
        int count = getPointCount();
        if (count <= 0) return new double[]{0, 0, 0, 0};
        count = Math.min(count, Math.min(xs.length, ys.length));
        if (count <= 0) return new double[]{0, 0, 0, 0};
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            double v = xs[i];
            if (!Double.isFinite(v)) continue;
            if (v < minX) minX = v;
            if (v > maxX) maxX = v;
        }
        for (int i = 0; i < count; i++) {
            double v = ys[i];
            if (!Double.isFinite(v)) continue;
            if (v < minY) minY = v;
            if (v > maxY) maxY = v;
        }
        if (minX == Double.MAX_VALUE || minY == Double.MAX_VALUE) return new double[]{0, 0, 0, 0};
        return new double[]{minX, maxX, minY, maxY};
    }

    /**
     * Optional weight array for pie/donut-like renderers. Default: fallback to Y-data.
     * Renderers should prefer this primitive API for zero-allocation rendering.
     */
    default double[] getWeightData() {
        return getYData();
    }

    /**
     * Returns the weight value at the given index.
     *
     * @param index data point index
     * @return weight value or 0.0 if out of range
     */
    default double getWeight(int index) {
        double[] arr = getWeightData();
        if (arr == null || index < 0 || index >= arr.length) return 0.0;
        return arr[index];
    }

    /**
     * Returns the provenance flag for a specific point.
     *
     * <p>Default: {@link ProvenanceFlags#ORIGINAL}.</p>
     */
    default byte getProvenanceFlag(int index) {
        byte[] arr = getProvenanceFlagsData();
        if (arr == null || index < 0 || index >= arr.length) return ProvenanceFlags.ORIGINAL;
        return arr[index];
    }

    /**
     * Returns the source identifier for a specific point.
     * Default: 0 (unknown).
     */
    default short getSourceId(int index) {
        short[] arr = getSourceIdsData();
        if (arr == null || index < 0 || index >= arr.length) return 0;
        return arr[index];
    }

    /**
     * Returns the timestamp (nanoseconds) for a specific point.
     * Default: 0 (unknown).
     */
    default long getTimestampNanos(int index) {
        long[] arr = getTimestampNanosData();
        if (arr == null || index < 0 || index >= arr.length) return 0L;
        return arr[index];
    }

    /**
     * Direct access to provenance flags (optional).
     */
    default byte[] getProvenanceFlagsData() {
        return EMPTY_BYTE;
    }

    /**
     * Direct access to source identifiers (optional).
     */
    default short[] getSourceIdsData() {
        return EMPTY_SHORT;
    }

    /**
     * Direct access to timestamps in nanoseconds (optional).
     */
    default long[] getTimestampNanosData() {
        return EMPTY_LONG;
    }

    /**
     * Optional multi-channel getter for models that expose multiple components per index (e.g. medical data).
     * Default: fallback to single-channel Y.
     */
    default double getY(int index, int channel) {
        return getY(index);
    }

    /**
     * Generic multi-component accessor used by statistical/financial renderers.
     * Default: fallback to `getY(index)`; implementors may override for boxplot/candlestick semantics.
     */
    default double getValue(int index, int component) {
        return getY(index);
    }

    /**
     * Direct access to open values (for financial charts).
     * Default: Fallback to Y-data.
     */
    default double[] getOpenData() {
        return getYData();
    }

    /**
     * Direct access to high values (for financial charts).
     * Default: Fallback to Y-data.
     */
    default double[] getHighData() {
        return getYData();
    }

    /**
     * Direct access to low values (for financial charts).
     * Default: Fallback to Y-data.
     */
    default double[] getLowData() {
        return getYData();
    }

    /**
     * Optional per-point minimum value. Default: fallback to the Y value for that index.
     */
    default double getMin(int index) {
        return getY(index);
    }

    /**
     * Optional per-point maximum value. Default: fallback to the Y value for that index.
     */
    default double getMax(int index) {
        return getY(index);
    }

    /**
     * Returns a label for a specific index (optional).
     *
     * @param index Index of the point
     * @return Label or null
     */
    default String getLabel(int index) {
        return null;
    }

    /**
     * Checks if the model is empty.
     */
    default boolean isEmpty() {
        return getPointCount() == 0;
    }

    /**
     * Registers a listener for data changes.
     *
     * @param listener The listener
     */
    void setChangeListener(ChartModelListener listener);

    /**
     * Removes a listener.
     *
     * @param listener The listener
     */
    void removeChangeListener(ChartModelListener listener);

    /**
     * Optional timestamp for model updates; default 0 for simple models.
     */
    default long getUpdateStamp() {
        return 0L;
    }

    /**
     * Optional visual color for a series; default is null.
     */
    default com.arbergashi.charts.api.types.ArberColor getColor() {
        return null;
    }

    /**
     * Optional setter for a visual color; default no-op.
     */
    default ChartModel setColor(com.arbergashi.charts.api.types.ArberColor color) {
        return this;
    }

    /**
     * Interface for change events.
     */
    interface ChartModelListener {
        /**
         * Called when the model data changes.
         */
        void modelChanged();
    }

}
