package com.arbergashi.charts.model;

import java.awt.*;
import java.awt.EventQueue;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default {@link ChartModel} implementation backed by primitive arrays.
 *
 * <p>This model is designed for fast ingestion and rendering. Internally it keeps growable
 * primitive arrays for X/Y and optional metadata (min/max/weight/label). The logical size is
 * reported by {@link #getPointCount()}.</p>
 *
 * <p><b>Framework contract:</b> This implementation returns defensive copies from
 * {@link #getXData()} and {@link #getYData()} to keep consumer code safe from accidental mutation.
 * For high-frequency ingestion scenarios, use the per-point accessors ({@link #getX(int)},
 * {@link #getY(int)}, {@link #getValue(int, int)}) and avoid repeatedly requesting full arrays.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class DefaultChartModel implements ChartModel {

    // --- ARCHITECTURE STATE ---
    private final AtomicLong updateStamp = new AtomicLong(0);
    private final List<ChartModelListener> listeners = new CopyOnWriteArrayList<>();
    private String name = "Series";
    private String subtitle = null;
    private Color color = null;
    private boolean dispatchOnEdt = false;
    // --- PRIMITIVE BACKING STORES (The Engine) ---
    private double[] xData = new double[1024];
    private double[] yData = new double[1024];
    private double[] minData = new double[1024];
    private double[] maxData = new double[1024];
    private double[] weightData = new double[1024];
    private int size = 0;

    // Store labels as optional per-point metadata (not used in hot numeric paths).
    private String[] labels = new String[1024];

    // Lazy Legacy Cache

    public DefaultChartModel() {
    }

    /**
     * Creates a model with a custom series name.
     *
     * @param name series name
     */
    public DefaultChartModel(String name) {
        this.name = name;
    }

    /**
     * Creates a model with a custom series name and color.
     *
     * @param name series name
     * @param color series color
     */
    public DefaultChartModel(String name, Color color) {
        this.name = name;
        this.color = color;
    }

    // === HIGH-PERFORMANCE API (v2.1.0) ===
    @Override
    public int getPointCount() {
        return size;
    }

    /**
     * Returns the X value at the given index.
     *
     * @param index point index
     * @return X value
     */
    public double getX(int index) {
        return xData[index];
    }

    /**
     * Returns the Y value at the given index.
     *
     * @param index point index
     * @return Y value
     */
    public double getY(int index) {
        return yData[index];
    }

    @Override
    public double getMin(int index) {
        return minData[index];
    }

    @Override
    public double getMax(int index) {
        return maxData[index];
    }

    @Override
    public double getWeight(int index) {
        return weightData[index];
    }

    /**
     * Returns a component by index for multi-component renderers.
     *
     * <p>Component mapping: 0=x, 1=y, 2=weight, 3=min, 4=max.</p>
     *
     * @param index data index
     * @param component component selector
     * @return component value
     */
    public double getValue(int index, int component) {
        return switch (component) {
            case 0 -> xData[index];
            case 1 -> yData[index];
            case 2 -> weightData[index];
            case 3 -> minData[index];
            case 4 -> maxData[index];
            default -> 0.0;
        };
    }

    /**
     * Returns the update stamp incremented on each modification.
     *
     * @return update stamp
     */
    public long getUpdateStamp() {
        return updateStamp.get();
    }

    @Override
    public double[] getXData() {
        return Arrays.copyOf(xData, size);
    }

    @Override
    public double[] getYData() {
        return Arrays.copyOf(yData, size);
    }

    @Override
    public double[] getLowData() {
        return Arrays.copyOf(minData, size);
    }

    @Override
    public double[] getHighData() {
        return Arrays.copyOf(maxData, size);
    }

    @Override
    public double[] getWeightData() {
        return Arrays.copyOf(weightData, size);
    }

    /**
     * Returns a copy of the min (low) data array.
     *
     * @return min data array sized to point count
     */
    public double[] getMinData() {
        return Arrays.copyOf(minData, size);
    }

    /**
     * Returns a copy of the max (high) data array.
     *
     * @return max data array sized to point count
     */
    public double[] getMaxData() {
        return Arrays.copyOf(maxData, size);
    }

    // === DATA INGESTION (Zero-Allocation Internal) ===
    /**
     * Adds a chart point and invalidates the model.
     *
     * @param p point to add
     */
    public void addPoint(ChartPoint p) {
        if (p == null) return;
        ensureCapacity(size + 1);
        xData[size] = p.x();
        yData[size] = p.y();
        weightData[size] = p.weight();
        minData[size] = p.min();
        maxData[size] = p.max();
        labels[size] = p.label();
        size++;
        invalidate();
    }

    private void ensureCapacity(int minCap) {
        if (minCap > xData.length) {
            int newCap = Math.max(minCap, xData.length * 2);
            xData = Arrays.copyOf(xData, newCap);
            yData = Arrays.copyOf(yData, newCap);
            minData = Arrays.copyOf(minData, newCap);
            maxData = Arrays.copyOf(maxData, newCap);
            weightData = Arrays.copyOf(weightData, newCap);
            labels = Arrays.copyOf(labels, newCap);
        }
    }

    private void invalidate() {
        updateStamp.incrementAndGet();
        fireModelChanged();
    }

    /**
     * Clears all points while keeping internal buffers allocated.
     */
    public void clear() {
        size = 0;
        // Keep arrays allocated; clear metadata references to avoid retaining large strings.
        Arrays.fill(labels, null);
        invalidate();
    }

    // === BOILERPLATE ===
    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the series name.
     *
     * @param name series name
     */
    public void setName(String name) {
        this.name = name;
        invalidate();
    }

    /**
     * Returns the optional subtitle.
     *
     * @return subtitle or null
     */
    public String getSubtitle() {
        return subtitle;
    }

    /**
     * Sets the subtitle.
     *
     * @param subtitle subtitle or null
     */
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        invalidate();
    }

    /**
     * Returns the series color override.
     *
     * @return series color or null to use theme defaults
     */
    public Color getColor() {
        return color;
    }

    /**
     * Sets a series color override.
     *
     * @param color series color (null resets)
     */
    public void setColor(Color color) {
        this.color = color;
        invalidate();
    }

    /**
     * Controls whether change listeners are notified on the Swing EDT.
     *
     * <p>When enabled, background updates will dispatch listener notifications via
     * {@link java.awt.EventQueue#invokeLater(Runnable)}. Default is disabled.</p>
     */
    public DefaultChartModel setDispatchOnEdt(boolean enabled) {
        this.dispatchOnEdt = enabled;
        return this;
    }

    @Override
    public void addChangeListener(ChartModelListener l) {
        if (l != null) listeners.add(l);
    }

    @Override
    public void removeChangeListener(ChartModelListener l) {
        listeners.remove(l);
    }

    protected void fireModelChanged() {
        if (dispatchOnEdt && !EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(this::notifyListeners);
            return;
        }
        notifyListeners();
    }

    private void notifyListeners() {
        for (var l : listeners) l.modelChanged();
    }

    // Legacy Support methods remain for compatibility, redirecting to primitive flow
    /**
     * Adds multiple points from a list.
     *
     * @param pts points to add
     */
    public void addPoints(List<ChartPoint> pts) {
        if (pts != null) pts.forEach(this::addPoint);
    }

    /**
     * Replaces all points with the provided list.
     *
     * @param pts new points
     */
    public void setPoints(List<ChartPoint> pts) {
        clear();
        addPoints(pts);
    }

    // Convenience methods for quick demo usage (optional, not hot-path)
    /**
     * Adds a point with weight and label.
     *
     * @param x X value
     * @param y Y value
     * @param weight weight value
     * @param label label text
     */
    public void addPoint(double x, double y, double weight, String label) {
        addPoint(new ChartPoint(x, y, weight, y, y, label));
    }

    /**
     * Adds a point with explicit min/max, weight, and label.
     *
     * @param x X value
     * @param y Y value
     * @param min min value
     * @param max max value
     * @param weight weight value
     * @param label label text
     */
    public void addPoint(double x, double y, double min, double max, double weight, String label) {
        // ChartPoint constructor order is: (x, y, weight, min, max, label)
        addPoint(new ChartPoint(x, y, weight, min, max, label));
    }

    /**
     * Convenience method to add a simple XY point.
     *
     * @param x X value
     * @param y Y value
     */
    public void addXY(double x, double y) {
        addPoint(x, y, 1.0, "");
    }

    /**
     * Convenience method to add a simple XY point with label.
     *
     * @param x     X value
     * @param y     Y value
     * @param label Label
     */
    public void addXY(double x, double y, String label) {
        addPoint(x, y, 1.0, label);
    }

    /**
     * Add multiple chart points in bulk.
     *
     * @param points List of chart points
     */
    public void addAll(List<ChartPoint> points) {
        if (points != null) {
            points.forEach(this::addPoint);
        }
    }

    /**
     * Add XY data from parallel arrays.
     *
     * @param x Array of X values
     * @param y Array of Y values
     */
    public void addXYArrays(double[] x, double[] y) {
        if (x == null || y == null) return;
        int n = Math.min(x.length, y.length);
        for (int i = 0; i < n; i++) {
            addPoint(x[i], y[i], 1.0, "");
        }
    }

    /**
     * Add OHLC bar data.
     *
     * @param bar OHLC bar
     */
    public void addOHLC(OHLCBar bar) {
        if (bar == null) return;
        ensureCapacity(size + 1);
        xData[size] = bar.time();
        yData[size] = bar.close();
        weightData[size] = bar.open();
        minData[size] = bar.low();
        maxData[size] = bar.high();
        size++;
        invalidate();
    }

    /**
     * Add OHLC bar data from parameters.
     *
     * @param time  Time/X value
     * @param open  Open price
     * @param high  High price
     * @param low   Low price
     * @param close Close price
     */
    public void addOHLC(double time, double open, double high, double low, double close) {
        addOHLC(new OHLCBar(time, open, high, low, close));
    }

    /**
     * Add error bar point data.
     *
     * @param point Error bar point
     */
    public void addWithError(ErrorBarPoint point) {
        if (point == null) return;
        ensureCapacity(size + 1);
        xData[size] = point.x();
        yData[size] = point.y();
        minData[size] = point.errorLow();
        maxData[size] = point.errorHigh();
        weightData[size] = 1.0;
        size++;
        invalidate();
    }

    /**
     * Add error bar point with symmetric error.
     *
     * @param x     X value
     * @param y     Y value
     * @param error Symmetric error (±)
     */
    public void addWithError(double x, double y, double error) {
        addWithError(new ErrorBarPoint(x, y, y - error, y + error));
    }

    /**
     * Add error bar point with asymmetric error.
     *
     * @param x         X value
     * @param y         Y value
     * @param errorLow  Lower error bound
     * @param errorHigh Upper error bound
     */
    public void addWithError(double x, double y, double errorLow, double errorHigh) {
        addWithError(new ErrorBarPoint(x, y, errorLow, errorHigh));
    }

    @Override
    public double[] getDataRange() {
        if (size == 0) return new double[]{0, 0, 0, 0};

        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (int i = 0; i < size; i++) {
            double x = xData[i];
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;

            double mn = minData[i];
            double mx = maxData[i];
            if (mn < minY) minY = mn;
            if (mx > maxY) maxY = mx;
        }
        return new double[]{minX, maxX, minY, maxY};
    }

    @Override
    public String getLabel(int index) {
        if (index < 0 || index >= size) return null;
        return labels[index];
    }
}
