package com.arbergashi.charts.model;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Default {@link FinancialChartModel} implementation backed by primitive arrays.
 *
 * <p>Designed for zero-allocation rendering of OHLC data. Arrays may be larger
 * than the logical size. Consumers must always bound iteration by
 * {@link #getPointCount()}.</p>
 *
 * @since 2.0.0
  * @author Arber Gashi
  * @version 2.0.0
 */
public class DefaultFinancialChartModel implements FinancialChartModel {
    private static final Logger LOGGER = Logger.getLogger(DefaultFinancialChartModel.class.getName());

    private final AtomicLong updateStamp = new AtomicLong(0);
    private final List<ChartModelListener> listeners = new CopyOnWriteArrayList<>();
    private String name = "Financial Series";
    private boolean dispatchOnEdt = false;
    private Executor dispatchExecutor;

    private double[] xData = new double[1024];
    private double[] openData = new double[1024];
    private double[] highData = new double[1024];
    private double[] lowData = new double[1024];
    private double[] closeData = new double[1024];
    private double[] volumeData = new double[1024];
    private byte[] provenanceFlags = new byte[1024];
    private short[] sourceIds = new short[1024];
    private long[] timestampNanos = new long[1024];
    private String[] labels = new String[1024];

    private int size = 0;

    public DefaultFinancialChartModel() {
    }

    public DefaultFinancialChartModel(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public DefaultFinancialChartModel setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Controls whether change listeners are notified via a configured dispatch executor.
     */
    public DefaultFinancialChartModel setDispatchOnEdt(boolean enabled){
        this.dispatchOnEdt = enabled;
        return this;
        
    }

    /**
     * Sets the executor used when {@code dispatchOnEdt} is enabled.
     *
     * @param executor executor for listener dispatch (nullable)
     * @return this model for chaining
     */
    public DefaultFinancialChartModel setDispatchExecutor(Executor executor){
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
    public double[] getOpenData() {
        return openData;
    }

    @Override
    public double[] getHighData() {
        return highData;
    }

    @Override
    public double[] getLowData() {
        return lowData;
    }

    @Override
    public double[] getCloseData() {
        return closeData;
    }

    @Override
    public double[] getVolumeData() {
        return volumeData;
    }

    @Override
    public double getX(int index) {
        if (index < 0 || index >= size) return 0.0;
        return xData[index];
    }

    @Override
    public double getY(int index) {
        if (index < 0 || index >= size) return 0.0;
        return closeData[index];
    }

    @Override
    public double getWeight(int index) {
        if (index < 0 || index >= size) return 0.0;
        return openData[index];
    }

    @Override
    public byte[] getProvenanceFlagsData() {
        return provenanceFlags;
    }

    @Override
    public short[] getSourceIdsData() {
        return sourceIds;
    }

    @Override
    public long[] getTimestampNanosData() {
        return timestampNanos;
    }

    @Override
    public String getLabel(int index) {
        if (labels == null || index < 0 || index >= labels.length) return null;
        return labels[index];
    }

    public long getUpdateStamp() {
        return updateStamp.get();
    }

    public void setOHLC(OHLCBar bar) {
        setOHLC(bar.getTime(), bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), null);
    }

    public void setOHLC(double time, double open, double high, double low, double close) {
        setOHLC(time, open, high, low, close, null);
    }

    public void setOHLC(double time, double open, double high, double low, double close, String label) {
        ensureCapacity(size + 1);
        xData[size] = time;
        openData[size] = open;
        highData[size] = high;
        lowData[size] = low;
        closeData[size] = close;
        volumeData[size] = 0.0;
        labels[size] = label;
        size++;
        updateStamp.incrementAndGet();
        fireModelChanged();
    }

    public void setOHLC(double time, double open, double high, double low, double close, double volume, String label) {
        ensureCapacity(size + 1);
        xData[size] = time;
        openData[size] = open;
        highData[size] = high;
        lowData[size] = low;
        closeData[size] = close;
        volumeData[size] = volume;
        labels[size] = label;
        size++;
        updateStamp.incrementAndGet();
        fireModelChanged();
    }

    private void ensureCapacity(int required) {
        if (required <= xData.length) return;
        int next = Math.max(required, xData.length * 2);
        xData = Arrays.copyOf(xData, next);
        openData = Arrays.copyOf(openData, next);
        highData = Arrays.copyOf(highData, next);
        lowData = Arrays.copyOf(lowData, next);
        closeData = Arrays.copyOf(closeData, next);
        volumeData = Arrays.copyOf(volumeData, next);
        provenanceFlags = Arrays.copyOf(provenanceFlags, next);
        sourceIds = Arrays.copyOf(sourceIds, next);
        timestampNanos = Arrays.copyOf(timestampNanos, next);
        labels = Arrays.copyOf(labels, next);
    }

    @Override
    public void setChangeListener(ChartModelListener listener) {
        if (listener != null) listeners.add(listener);
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
            try {
                dispatchExecutor.execute(this::notifyListeners);
                return;
            } catch (RuntimeException ex) {
                LOGGER.log(Level.WARNING, "Dispatch executor rejected listener notification; falling back to caller thread", ex);
            }
            notifyListeners();
            return;
        }
        notifyListeners();
    }

    private void notifyListeners() {
        if (listeners.isEmpty()) return;
        for (var l : listeners) {
            try {
                l.modelChanged();
            } catch (RuntimeException ex) {
                LOGGER.log(Level.WARNING, "ChartModel listener failed and was isolated", ex);
            }
        }
    }
}
