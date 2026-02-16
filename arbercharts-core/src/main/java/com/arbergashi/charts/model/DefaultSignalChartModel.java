package com.arbergashi.charts.model;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Default {@link SignalChartModel} implementation with optional circular buffering.
 *
 * <p>Designed for high-frequency signal streams with multiple channels.</p>
 *
 * @since 2025-06-01
  * @author Arber Gashi
  * @version 1.7.0
 */
public class DefaultSignalChartModel implements SignalChartModel {
    private static final Logger LOGGER = Logger.getLogger(DefaultSignalChartModel.class.getName());

    private final AtomicLong updateStamp = new AtomicLong(0);
    private final List<ChartModelListener> listeners = new CopyOnWriteArrayList<>();
    private String name = "Signal";

    private double sampleRateHz;
    private boolean circular;
    private boolean dispatchOnEdt;
    private Executor dispatchExecutor;

    private double[] xData;
    private double[][] channelData;
    private long[] timestampNanos;
    private String[] labels;
    private int size;
    private int writeIndex;

    public DefaultSignalChartModel(int channelCount, int capacity) {
        this(channelCount, capacity, false);
    }

    public DefaultSignalChartModel(int channelCount, int capacity, boolean circular) {
        if (channelCount <= 0) throw new IllegalArgumentException("channelCount must be > 0");
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.circular = circular;
        this.xData = new double[capacity];
        this.timestampNanos = new long[capacity];
        this.labels = new String[capacity];
        this.channelData = new double[channelCount][capacity];
    }

    public DefaultSignalChartModel setName(String name) {
        this.name = name;
        return this;
        
    }

    public DefaultSignalChartModel setSampleRateHz(double sampleRateHz){
        this.sampleRateHz = sampleRateHz;
        return this;
        
    }

    public DefaultSignalChartModel setDispatchOnEdt(boolean enabled){
        this.dispatchOnEdt = enabled;
        return this;
        
    }

    /**
     * Sets the executor used when {@code dispatchOnEdt} is enabled.
     *
     * @param executor executor for listener dispatch (nullable)
     * @return this model for chaining
     */
    public DefaultSignalChartModel setDispatchExecutor(Executor executor){
        this.dispatchExecutor = executor;
        return this;
    }

    @Override
    public String getName() {
        return name;
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
    public double[] getYData() {
        return channelData[0];
    }

    @Override
    public double getX(int index) {
        return xData[physicalIndex(index)];
    }

    @Override
    public double getY(int index) {
        return channelData[0][physicalIndex(index)];
    }

    @Override
    public double getValue(int index, int channel) {
        return channelData[channel][physicalIndex(index)];
    }

    @Override
    public long[] getTimestampNanosData() {
        return timestampNanos;
    }

    @Override
    public String getLabel(int index) {
        int idx = physicalIndex(index);
        if (idx < 0 || idx >= labels.length) return null;
        return labels[idx];
    }

    @Override
    public int getChannelCount() {
        return channelData.length;
    }

    @Override
    public double getSampleRateHz() {
        return sampleRateHz;
    }

    @Override
    public double[] getChannelData(int channel) {
        return channelData[channel];
    }

    @Override
    public boolean isCircular() {
        return circular;
    }

    @Override
    public int getWriteIndex() {
        return writeIndex;
    }

    @Override
    public int getCapacity() {
        return xData.length;
    }

    public long getUpdateStamp() {
        return updateStamp.get();
    }

    /**
     * Adds a sample across all channels.
     *
     * @param x      X value (time)
     * @param values channel values (must match channel count)
     */
    public void setSample(double x, double[] values) {
        setSample(x, 0L, values, null);
    }

    public void setSample(double x, long timestamp, double[] values, String label) {
        if (values == null || values.length != channelData.length) {
            throw new IllegalArgumentException("values length must match channel count");
        }
        int idx = allocateIndex();
        xData[idx] = x;
        timestampNanos[idx] = timestamp;
        labels[idx] = label;
        for (int c = 0; c < channelData.length; c++) {
            channelData[c][idx] = values[c];
        }
        updateStamp.incrementAndGet();
        fireModelChanged();
    }

    private int allocateIndex() {
        if (!circular) {
            if (size >= xData.length) {
                grow();
            }
            return size++;
        }
        int idx = writeIndex;
        writeIndex = (writeIndex + 1) % xData.length;
        if (size < xData.length) size++;
        return idx;
    }

    private int physicalIndex(int logicalIndex) {
        if (!circular) return logicalIndex;
        if (size == 0) return 0;
        int start = (writeIndex - size);
        if (start < 0) start += xData.length;
        int idx = start + logicalIndex;
        if (idx >= xData.length) idx -= xData.length;
        return idx;
    }

    private void grow() {
        int next = xData.length * 2;
        xData = Arrays.copyOf(xData, next);
        timestampNanos = Arrays.copyOf(timestampNanos, next);
        labels = Arrays.copyOf(labels, next);
        for (int c = 0; c < channelData.length; c++) {
            channelData[c] = Arrays.copyOf(channelData[c], next);
        }
    }

    @Override
    public void setChangeListener(ChartModelListener listener) {
        if (listener != null) listeners.add(listener);
    }

    @Override
    public void removeChangeListener(ChartModelListener listener) {
        listeners.remove(listener);
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
