package com.arbergashi.charts.model;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
/**
 * Default {@link StatisticalChartModel} implementation backed by primitive arrays.
 *
 * <p>Designed for zero-allocation rendering of statistical series and box plots.</p>
 *
 * @since 2025-06-01
  * @author Arber Gashi
  * @version 1.7.0
 */
public class DefaultStatisticalChartModel implements StatisticalChartModel {
    private final AtomicLong updateStamp = new AtomicLong(0);
    private final List<ChartModelListener> listeners = new CopyOnWriteArrayList<>();
    private String name = "Statistical Series";
    private boolean dispatchOnEdt = false;
    private Executor dispatchExecutor;

    private double[] xData = new double[256];
    private double[] medianData = new double[256];
    private double[] q1Data = new double[256];
    private double[] q3Data = new double[256];
    private double[] lowData = new double[256];
    private double[] highData = new double[256];
    private double[] iqrData = new double[256];
    private byte[] provenanceFlags = new byte[256];
    private short[] sourceIds = new short[256];
    private long[] timestampNanos = new long[256];
    private String[] labels = new String[256];
    private int size = 0;

    public DefaultStatisticalChartModel() {
    }

    public DefaultStatisticalChartModel(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public DefaultStatisticalChartModel setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Controls whether change listeners are notified via a configured dispatch executor.
     */
    public DefaultStatisticalChartModel setDispatchOnEdt(boolean enabled){
        this.dispatchOnEdt = enabled;
        return this;
        
    }

    /**
     * Sets the executor used when {@code dispatchOnEdt} is enabled.
     *
     * @param executor executor for listener dispatch (nullable)
     * @return this model for chaining
     */
    public DefaultStatisticalChartModel setDispatchExecutor(Executor executor){
        this.dispatchExecutor = executor;
        return this;
    }

    @Override
    public int getPointCount() {
        return size;
    }

    @Override
    public double[] getXData() {
        return xData;
    }

    @Override
    public double[] getMedianData() {
        return medianData;
    }

    @Override
    public double[] getYData() {
        return medianData;
    }

    @Override
    public double[] getQ1Data() {
        return q1Data;
    }

    @Override
    public double[] getQ3Data() {
        return q3Data;
    }

    @Override
    public double[] getLowData() {
        return lowData;
    }

    @Override
    public double[] getHighData() {
        return highData;
    }

    @Override
    public double getValue(int index, int component) {
        return switch (component) {
            case 0 -> getX(index);
            case 1 -> getMedian(index);
            case 2 -> getIqr(index);
            case 3 -> getMin(index);
            case 4 -> getMax(index);
            default -> 0.0;
        };
    }

    @Override
    public double[] getWeightData() {
        return iqrData;
    }

    @Override
    public String getLabel(int index) {
        if (labels == null || index < 0 || index >= labels.length) return null;
        return labels[index];
    }

    public long getUpdateStamp() {
        return updateStamp.get();
    }

    public void setBoxPlot(double x, double median, double q1, double q3, double min, double max, String label) {
        ensureCapacity(size + 1);
        xData[size] = x;
        medianData[size] = median;
        q1Data[size] = q1;
        q3Data[size] = q3;
        lowData[size] = min;
        highData[size] = max;
        iqrData[size] = q3 - q1;
        labels[size] = label;
        size++;
        updateStamp.incrementAndGet();
        fireModelChanged();
    }

    public void setQuantilePoint(double x, double median, double q1, double q3, String label) {
        double iqr = q3 - q1;
        setBoxPlot(x, median, q1, q3, median - iqr, median + iqr, label);
    }

    private void ensureCapacity(int required) {
        if (required <= xData.length) return;
        int next = Math.max(required, xData.length * 2);
        xData = Arrays.copyOf(xData, next);
        medianData = Arrays.copyOf(medianData, next);
        q1Data = Arrays.copyOf(q1Data, next);
        q3Data = Arrays.copyOf(q3Data, next);
        lowData = Arrays.copyOf(lowData, next);
        highData = Arrays.copyOf(highData, next);
        iqrData = Arrays.copyOf(iqrData, next);
        provenanceFlags = Arrays.copyOf(provenanceFlags, next);
        sourceIds = Arrays.copyOf(sourceIds, next);
        timestampNanos = Arrays.copyOf(timestampNanos, next);
        labels = Arrays.copyOf(labels, next);
    }

    @Override
    public void setChangeListener(ChartModelListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeChangeListener(ChartModelListener listener) {
        listeners.remove(listener);
    }

    private void fireChanged() {
        fireModelChanged();
    }

    protected void fireModelChanged() {
        if (dispatchOnEdt && dispatchExecutor != null) {
            dispatchExecutor.execute(this::notifyListeners);
            return;
        }
        notifyListeners();
    }

    private void notifyListeners() {
        if (listeners.isEmpty()) return;
        for (var l : listeners) l.modelChanged();
    }
}
