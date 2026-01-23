package com.arbergashi.charts.model;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Optimized model for real-time medical data (ECG, EEG).
 *
 * <p>Uses primitive arrays with dynamic resizing.
 * Thread-safe for append operations (single-writer, multiple-reader scenario).</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * @see ChartModel
 */
public class FastMedicalModel implements ChartModel {

    private final String name;
    private final Object lock = new Object();
    private final List<ChartModelListener> listeners = new CopyOnWriteArrayList<>();
    private double[] xData;
    private double[] yData;
    private int size = 0;

    public FastMedicalModel(String name, int initialCapacity) {
        this.name = name;
        this.xData = new double[initialCapacity];
        this.yData = new double[initialCapacity];
    }

    public FastMedicalModel(String name) {
        this(name, 1024);
    }

    /**
     * Adds a new point.
     * Thread-safe.
     */
    public void addPoint(double x, double y) {
        synchronized (lock) {
            ensureCapacity(size + 1);
            xData[size] = x;
            yData[size] = y;
            size++;
        }
        fireDataChanged();
    }

    /**
     * Adds multiple points efficiently (bulk operation).
     */
    public void addPoints(double[] x, double[] y) {
        if (x.length != y.length) throw new IllegalArgumentException("Array lengths differ");
        synchronized (lock) {
            int len = x.length;
            ensureCapacity(size + len);
            System.arraycopy(x, 0, xData, size, len);
            System.arraycopy(y, 0, yData, size, len);
            size += len;
        }
        fireDataChanged();
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > xData.length) {
            int newCap = Math.max(minCapacity, xData.length * 2);
            xData = Arrays.copyOf(xData, newCap);
            yData = Arrays.copyOf(yData, newCap);
        }
    }

    public void clear() {
        synchronized (lock) {
            size = 0;
        }
        fireDataChanged();
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
        // Returns the direct array for O(1) access in the renderer.
        // Caller must respect the size!
        return xData;
    }

    @Override
    public double[] getYData() {
        return yData;
    }

    public void trimToSize() {
        // Optional: free memory if necessary
    }

    @Override
    public void addChangeListener(ChartModelListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeChangeListener(ChartModelListener listener) {
        listeners.remove(listener);
    }

    protected void fireDataChanged() {
        for (ChartModelListener listener : listeners) {
            listener.modelChanged();
        }
    }
}