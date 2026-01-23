package com.arbergashi.charts.model;

import java.awt.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Circular buffer implementation for high-performance real-time medical data (e.g. ECG, Spirometry).
 * Provides zero-copy, zero-GC access for FastMedicalModel.
 * <p>
 * <b>Design:</b> No ChangeListeners, no legacy API (addPoint, setPoints, etc.).
 * Optimized for DSP and real-time rendering.
 * <p>
 * <b>Important for Renderers:</b> For sweep-erase/monitor visualization, use getSweepIndex() to identify the current write position and avoid drawing a line across the buffer start.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-12-30
 */
public class CircularFastMedicalModel extends FastMedicalModel {
    private final int capacity;
    private final double[] x;
    private final double[][] y; // [channel][capacity]
    private final int channels;
    private final AtomicLong updateStamp = new AtomicLong(0);
    private int head = 0;
    private int size = 0;
    private int sweepIndex = 0;
    private String name = "MedicalSeries";
    private Color color = null;

    public CircularFastMedicalModel(int capacity, int channels) {
        super("CircularMedical", capacity);
        this.capacity = capacity;
        this.channels = channels;
        this.x = new double[capacity];
        this.y = new double[channels][capacity];
    }

    /**
     * Adds a new data point (X and all Y channels).
     *
     * @param xVal  X value (e.g., time)
     * @param yVals Y values for all channels (e.g., pressure, flow, volume)
     */
    public void add(double xVal, double[] yVals) {
        if (yVals == null || yVals.length != channels)
            throw new IllegalArgumentException("yVals must have length=" + channels);
        x[head] = xVal;
        for (int c = 0; c < channels; c++) {
            y[c][head] = yVals[c];
        }
        head = (head + 1) % capacity;
        if (size < capacity) size++;
        sweepIndex = head;
        updateStamp.incrementAndGet();
    }

    @Override
    public int getPointCount() {
        return size;
    }

    @Override
    public double getX(int index) {
        checkIndex(index);
        int idx = (head - size + index + capacity) % capacity;
        return x[idx];
    }

    /**
     * Returns the Y value for an index (ChartModel standard, channel 0).
     */
    @Override
    public double getY(int index) {
        return getY(index, 0);
    }

    /**
     * Returns the Y value for an index and channel (medical semantics).
     */
    @Override
    public double getY(int index, int channel) {
        checkIndex(index);
        checkChannel(channel);
        int idx = (head - size + index + capacity) % capacity;
        return y[channel][idx];
    }

    /**
     * Returns the value for an index and component (ChartModel standard, multivariate).
     * Component 0 = X, 1 = Y[0], 2 = Y[1], ...
     */
    @Override
    public double getValue(int index, int component) {
        checkIndex(index);
        if (component == 0) return getX(index);
        if (component >= 1 && component <= channels) return getY(index, component - 1);
        return 0.0;
    }

    @Override
    public long getUpdateStamp() {
        return updateStamp.get();
    }

    /**
     * Returns the internal array for a channel (DSP/filter operations, Read-Only!).
     */
    public double[] getRawChannelArray(int channel) {
        checkChannel(channel);
        return y[channel];
    }

    public int getHeadIndex() {
        return head;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getSweepIndex() {
        return sweepIndex;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color color) {
        this.color = color;
    }


    @Override
    public void clear() {
        head = 0;
        size = 0;
        updateStamp.incrementAndGet();
    }

    @Override
    public void addChangeListener(ChartModelListener listener) {
        // No listener support (design decision for real-time performance)
    }

    @Override
    public void removeChangeListener(ChartModelListener listener) {
        // No listener support
    }

    // --- Utility: Defensive index checks ---
    private void checkIndex(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }

    private void checkChannel(int channel) {
        if (channel < 0 || channel >= channels)
            throw new IndexOutOfBoundsException("Channel: " + channel + ", Channels: " + channels);
    }

    @Override
    public double[] getXData() {
        return x;
    }

    @Override
    public double[] getYData() {
        return y[0];
    }

    /**
     * Returns the current head index (sweep position).
     */
    public int getRawHeadIndex() {
        return head;
    }

    /**
     * Returns the current logical size (number of valid points).
     */
    public int getRawSize() {
        return size;
    }

    /**
     * Returns the buffer capacity.
     */
    public int getRawCapacity() {
        return capacity;
    }

    /**
     * Returns the Y value at the absolute buffer index (no head/size mapping).
     * Used for Sweep-Erase/Monitor-Style rendering.
     */
    public double getYRaw(int internalIndex, int channel) {
        checkChannel(channel);
        if (internalIndex < 0 || internalIndex >= capacity)
            throw new IndexOutOfBoundsException("InternalIndex: " + internalIndex + ", Capacity: " + capacity);
        return y[channel][internalIndex];
    }

    @Override
    public double[] getDataRange() {
        if (size == 0) return new double[]{0, 0, 0, 0};

        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        // Iterate over valid points only
        for (int i = 0; i < size; i++) {
            double xv = getX(i);
            if (xv < minX) minX = xv;
            if (xv > maxX) maxX = xv;

            for (int c = 0; c < channels; c++) {
                double yv = getY(i, c);
                if (yv < minY) minY = yv;
                if (yv > maxY) maxY = yv;
            }
        }
        return new double[]{minX, maxX, minY, maxY};
    }
}
