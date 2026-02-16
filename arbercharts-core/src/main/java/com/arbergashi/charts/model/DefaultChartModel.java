package com.arbergashi.charts.model;
import com.arbergashi.charts.api.types.ArberColor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
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
 * @version 2.0.0
 * @since 2.0.0
 */
public class DefaultChartModel implements ChartModel {

    private static final Logger LOGGER = Logger.getLogger(DefaultChartModel.class.getName());

    // --- ARCHITECTURE STATE ---
    private final AtomicLong updateStamp = new AtomicLong(0);
    private final Object dataLock = new Object();
    private final List<ChartModelListener> listeners = new CopyOnWriteArrayList<>();
    private String name = "Series";
    private String subtitle = null;
    private ArberColor color = null;
    private boolean dispatchOnEdt = false;
    private Executor dispatchExecutor;
    // --- PRIMITIVE BACKING STORES (The Engine) ---
    private double[] xData = new double[1024];
    private double[] yData = new double[1024];
    private double[] minData = new double[1024];
    private double[] maxData = new double[1024];
    private double[] weightData = new double[1024];
    private byte[] provenanceFlags = new byte[1024];
    private short[] sourceIds = new short[1024];
    private long[] timestampNanos = new long[1024];
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
    public DefaultChartModel(String name, ArberColor color) {
        this.name = name;
        this.color = color;
    }

    // === HIGH-PERFORMANCE API (v2.1.0) ===
    @Override
    public int getPointCount() {
        synchronized (dataLock) {
            return size;
        }
    }

    /**
     * Returns the X value at the given index.
     *
     * @param index point index
     * @return X value
     */
    public double getX(int index) {
        synchronized (dataLock) {
            return xData[index];
        }
    }

    /**
     * Returns the Y value at the given index.
     *
     * @param index point index
     * @return Y value
     */
    public double getY(int index) {
        synchronized (dataLock) {
            return yData[index];
        }
    }

    @Override
    public double getMin(int index) {
        synchronized (dataLock) {
            return minData[index];
        }
    }

    @Override
    public double getMax(int index) {
        synchronized (dataLock) {
            return maxData[index];
        }
    }

    @Override
    public double getWeight(int index) {
        synchronized (dataLock) {
            return weightData[index];
        }
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
        synchronized (dataLock) {
            return switch (component) {
                case 0 -> xData[index];
                case 1 -> yData[index];
                case 2 -> weightData[index];
                case 3 -> minData[index];
                case 4 -> maxData[index];
                default -> 0.0;
            };
        }
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
        synchronized (dataLock) {
            return Arrays.copyOf(xData, size);
        }
    }

    @Override
    public double[] getYData() {
        synchronized (dataLock) {
            return Arrays.copyOf(yData, size);
        }
    }

    @Override
    public double[] getLowData() {
        synchronized (dataLock) {
            return Arrays.copyOf(minData, size);
        }
    }

    @Override
    public double[] getHighData() {
        synchronized (dataLock) {
            return Arrays.copyOf(maxData, size);
        }
    }

    @Override
    public double[] getWeightData() {
        synchronized (dataLock) {
            return Arrays.copyOf(weightData, size);
        }
    }

    @Override
    public byte[] getProvenanceFlagsData() {
        synchronized (dataLock) {
            return Arrays.copyOf(provenanceFlags, size);
        }
    }

    @Override
    public short[] getSourceIdsData() {
        synchronized (dataLock) {
            return Arrays.copyOf(sourceIds, size);
        }
    }

    @Override
    public long[] getTimestampNanosData() {
        synchronized (dataLock) {
            return Arrays.copyOf(timestampNanos, size);
        }
    }

    /**
     * Returns a copy of the min (low) data array.
     *
     * @return min data array sized to point count
     */
    public double[] getMinData() {
        synchronized (dataLock) {
            return Arrays.copyOf(minData, size);
        }
    }

    /**
     * Returns a copy of the max (high) data array.
     *
     * @return max data array sized to point count
     */
    public double[] getMaxData() {
        synchronized (dataLock) {
            return Arrays.copyOf(maxData, size);
        }
    }

    // === DATA INGESTION (Zero-Allocation Internal) ===
    /**
     * Adds a chart point and invalidates the model.
     *
     * @param p point to add
     */
    public void setPoint(ChartPoint p) {
        if (p == null) return;
        setPoint(p.getX(), p.getY(), p.getMin(), p.getMax(), p.getWeight(), p.getLabel(),
                ProvenanceFlags.ORIGINAL, (short) 0, 0L);
    }

    /**
     * Adds a chart point with provenance metadata.
     */
    public void setPoint(double x, double y, double min, double max, double weight, String label,
                         byte provenanceFlag, short sourceId, long timestampNano) {
        synchronized (dataLock) {
            ensureCapacity(size + 1);
            xData[size] = x;
            yData[size] = y;
            weightData[size] = weight;
            minData[size] = min;
            maxData[size] = max;
            labels[size] = label;
            provenanceFlags[size] = provenanceFlag;
            sourceIds[size] = sourceId;
            timestampNanos[size] = timestampNano;
            size++;
        }
        invalidate();
    }

    /**
     * Updates provenance metadata for an existing index.
     */
    public DefaultChartModel setProvenance(int index, byte provenanceFlag, short sourceId, long timestampNano) {
        synchronized (dataLock) {
            if (index < 0 || index >= size) return this;
            provenanceFlags[index] = provenanceFlag;
            sourceIds[index] = sourceId;
            timestampNanos[index] = timestampNano;
        }
        invalidate();
        return this;
    }

    private void ensureCapacity(int minCap) {
        if (minCap > xData.length) {
            int newCap = Math.max(minCap, xData.length * 2);
            xData = Arrays.copyOf(xData, newCap);
            yData = Arrays.copyOf(yData, newCap);
            minData = Arrays.copyOf(minData, newCap);
            maxData = Arrays.copyOf(maxData, newCap);
            weightData = Arrays.copyOf(weightData, newCap);
            provenanceFlags = Arrays.copyOf(provenanceFlags, newCap);
            sourceIds = Arrays.copyOf(sourceIds, newCap);
            timestampNanos = Arrays.copyOf(timestampNanos, newCap);
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
        synchronized (dataLock) {
            size = 0;
            // Keep arrays allocated; clear metadata references to avoid retaining large strings.
            Arrays.fill(labels, null);
            Arrays.fill(provenanceFlags, ProvenanceFlags.ORIGINAL);
            Arrays.fill(sourceIds, (short) 0);
            Arrays.fill(timestampNanos, 0L);
        }
        invalidate();
    }

    // === BOILERPLATE ===
    @Override
    public String getName() {
        synchronized (dataLock) {
            return name;
        }
    }

    /**
     * Sets the series name.
     *
     * @param name series name
     */
    public DefaultChartModel setName(String name) {
        synchronized (dataLock) {
            this.name = name;
        }
        invalidate();
        return this;
    }

    /**
     * Returns the optional subtitle.
     *
     * @return subtitle or null
     */
    public String getSubtitle() {
        synchronized (dataLock) {
            return subtitle;
        }
    }

    /**
     * Sets the subtitle.
     *
     * @param subtitle subtitle or null
     */
    public DefaultChartModel setSubtitle(String subtitle) {
        synchronized (dataLock) {
            this.subtitle = subtitle;
        }
        invalidate();
        return this;
    }

    /**
     * Returns the series color override.
     *
     * @return series color or null to use theme defaults
     */
    public ArberColor getColor() {
        synchronized (dataLock) {
            return color;
        }
    }

    /**
     * Sets a series color override.
     *
     * @param color series color (null resets)
     */
    public DefaultChartModel setColor(ArberColor color) {
        synchronized (dataLock) {
            this.color = color;
        }
        invalidate();
        return this;
    }

    /**
     * Controls whether change listeners are notified via a configured dispatch executor.
     *
     * <p>When enabled, background updates will dispatch listener notifications via
     * the configured executor. Default is disabled.</p>
     */
    public DefaultChartModel setDispatchOnEdt(boolean enabled){
        this.dispatchOnEdt = enabled;
        return this;
        
    }

    /**
     * Sets the executor used when {@code dispatchOnEdt} is enabled.
     *
     * @param executor executor for listener dispatch (nullable)
     * @return this model for chaining
     */
    public DefaultChartModel setDispatchExecutor(Executor executor){
        this.dispatchExecutor = executor;
        return this;
    }

    @Override
    public void setChangeListener(ChartModelListener l) {
        if (l != null) listeners.add(l);
    }

    @Override
    public void removeChangeListener(ChartModelListener l) {
        listeners.remove(l);
    }
    /**
     * @since 1.5.0
    */

    protected void fireModelChanged() {
        if (dispatchOnEdt && dispatchExecutor != null) {
            dispatchExecutor.execute(this::notifyListeners);
            return;
        }
        notifyListeners();
    }

    private void notifyListeners() {
        for (var l : listeners) {
            try {
                l.modelChanged();
            } catch (RuntimeException ex) {
                LOGGER.log(Level.WARNING, "ChartModel listener failed and was isolated", ex);
            }
        }
    }

    // Legacy Support methods remain for compatibility, redirecting to primitive flow
    /**
     * Replaces all points with the provided list.
     *
     * @param pts new points
     */
    public DefaultChartModel setPoints(List<ChartPoint> pts) {
        clear();
        setAll(pts);
        return this;
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
    public void setPoint(double x, double y, double weight, String label) {
        setPoint(new ChartPoint(x, y, weight, y, y, label));
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
    public void setPoint(double x, double y, double min, double max, double weight, String label) {
        // ChartPoint constructor order is: (x, y, weight, min, max, label)
        setPoint(new ChartPoint(x, y, weight, min, max, label));
    }

    /**
     * Convenience method to add a simple XY point.
     *
     * @param x X value
     * @param y Y value
     */
    public void setXY(double x, double y) {
        setPoint(x, y, 1.0, "");
    }

    /**
     * Convenience method to add a simple XY point with label.
     *
     * @param x     X value
     * @param y     Y value
     * @param label Label
     */
    public void setXY(double x, double y, String label) {
        setPoint(x, y, 1.0, label);
    }

    /**
     * Add multiple chart points in bulk.
     *
     * @param points List of chart points
     */
    public void setAll(List<ChartPoint> points) {
        if (points != null) {
            points.forEach(this::setPoint);
        }
    }

    /**
     * Add XY data from parallel arrays.
     *
     * @param x Array of X values
     * @param y Array of Y values
     */
    public void setXYArrays(double[] x, double[] y) {
        if (x == null || y == null) return;
        int n = Math.min(x.length, y.length);
        for (int i = 0; i < n; i++) {
            setPoint(x[i], y[i], 1.0, "");
        }
    }

    /**
     * Add OHLC bar data.
     *
     * @param bar OHLC bar
     */
    public void setOHLC(OHLCBar bar) {
        if (bar == null) return;
        synchronized (dataLock) {
            ensureCapacity(size + 1);
            xData[size] = bar.getTime();
            yData[size] = bar.getClose();
            weightData[size] = bar.getOpen();
            minData[size] = bar.getLow();
            maxData[size] = bar.getHigh();
            size++;
        }
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
    public void setOHLC(double time, double open, double high, double low, double close) {
        setOHLC(new OHLCBar(time, open, high, low, close));
    }

    /**
     * Add error bar point data.
     *
     * @param point Error bar point
     */
    public void setWithError(ErrorBarPoint point) {
        if (point == null) return;
        synchronized (dataLock) {
            ensureCapacity(size + 1);
            xData[size] = point.getX();
            yData[size] = point.getY();
            minData[size] = point.getErrorLow();
            maxData[size] = point.getErrorHigh();
            weightData[size] = 1.0;
            size++;
        }
        invalidate();
    }

    /**
     * Add error bar point with symmetric error.
     *
     * @param x     X value
     * @param y     Y value
     * @param error Symmetric error (±)
     */
    public void setWithError(double x, double y, double error) {
        setWithError(new ErrorBarPoint(x, y, y - error, y + error));
    }

    /**
     * Add error bar point with asymmetric error.
     *
     * @param x         X value
     * @param y         Y value
     * @param errorLow  Lower error bound
     * @param errorHigh Upper error bound
     */
    public void setWithError(double x, double y, double errorLow, double errorHigh) {
        setWithError(new ErrorBarPoint(x, y, errorLow, errorHigh));
    }

    @Override
    public double[] getDataRange() {
        synchronized (dataLock) {
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
    }

    @Override
    public String getLabel(int index) {
        synchronized (dataLock) {
            if (index < 0 || index >= size) return null;
            return labels[index];
        }
    }
}
