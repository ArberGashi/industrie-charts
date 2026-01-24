package com.arbergashi.charts.model;

import java.awt.Color;
import java.awt.EventQueue;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fixed-capacity ring-buffer model optimized for realtime streams.
 *
 * <p><b>Threading contract:</b> readers are lock-free and use per-slot sequencing to ensure
 * consistent field reads. Writers are serialized by an internal spin lock so multiple writer
 * threads are safe but will be ordered. For highest throughput, prefer a single writer.</p>
 *
 * <p><b>Atomic reads:</b> use {@link #readPoint(int, double[])} when you need a consistent
 * snapshot of x/y/min/max/weight. Individual getters are safe but may observe different points
 * if called separately.</p>
 *
 * <p>Capacity is rounded to the next power-of-two to enable fast index masking.</p>
 */
public final class CircularChartModel implements ChartModel {

    private static final VarHandle HEAD;
    private static final VarHandle TAIL;
    private static final VarHandle SEQ;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            HEAD = lookup.findVarHandle(CircularChartModel.class, "head", long.class);
            TAIL = lookup.findVarHandle(CircularChartModel.class, "tail", long.class);
            SEQ = MethodHandles.arrayElementVarHandle(long[].class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final int capacity;
    private final int mask;

    private final double[] xData;
    private final double[] yData;
    private final double[] minData;
    private final double[] maxData;
    private final double[] weightData;
    private final long[] sequences;
    private String[] labels;

    private final double[] xSnapshot;
    private final double[] ySnapshot;
    private final double[] minSnapshot;
    private final double[] maxSnapshot;
    private final double[] weightSnapshot;

    private final AtomicLong updateStamp = new AtomicLong(0);
    private final AtomicBoolean writeLock = new AtomicBoolean(false);
    private final List<ChartModelListener> listeners = new CopyOnWriteArrayList<>();

    private String name = "Series";
    private String subtitle = null;
    private Color color = null;
    private boolean dispatchOnEdt = false;
    private boolean labelsEnabled = true;

    // Monotonic sequence counters (multi-writer safe via atomic ops)
    private long head;
    private long tail;

    // Snapshot cache metadata
    private long snapshotHead = Long.MIN_VALUE;
    private long snapshotTail = Long.MIN_VALUE;
    private int snapshotCount = 0;

    private final ThreadLocal<ReadCache> readCache = ThreadLocal.withInitial(ReadCache::new);

    public CircularChartModel(int capacity) {
        this("Series", capacity);
    }

    public CircularChartModel(String name, int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        this.capacity = nextPowerOfTwo(capacity);
        this.mask = this.capacity - 1;
        this.name = name != null ? name : "Series";

        this.xData = new double[this.capacity];
        this.yData = new double[this.capacity];
        this.minData = new double[this.capacity];
        this.maxData = new double[this.capacity];
        this.weightData = new double[this.capacity];
        this.sequences = new long[this.capacity];
        this.labels = new String[this.capacity];

        this.xSnapshot = new double[this.capacity];
        this.ySnapshot = new double[this.capacity];
        this.minSnapshot = new double[this.capacity];
        this.maxSnapshot = new double[this.capacity];
        this.weightSnapshot = new double[this.capacity];
    }

    public int getCapacity() {
        return capacity;
    }

    public CircularChartModel setLabelsEnabled(boolean enabled) {
        this.labelsEnabled = enabled;
        if (!enabled) {
            this.labels = null;
        } else if (this.labels == null) {
            this.labels = new String[capacity];
        }
        invalidate();
        return this;
    }

    @Override
    public int getPointCount() {
        long h = (long) HEAD.getAcquire(this);
        long t = (long) TAIL.getAcquire(this);
        long count = h - t;
        if (count <= 0) return 0;
        return (int) Math.min(count, capacity);
    }

    @Override
    public double getX(int index) {
        return readValue(index, ValueComponent.X);
    }

    @Override
    public double getY(int index) {
        return readValue(index, ValueComponent.Y);
    }

    @Override
    public double getMin(int index) {
        return readValue(index, ValueComponent.MIN);
    }

    @Override
    public double getMax(int index) {
        return readValue(index, ValueComponent.MAX);
    }

    @Override
    public double getWeight(int index) {
        return readValue(index, ValueComponent.WEIGHT);
    }

    @Override
    public double getValue(int index, int component) {
        return switch (component) {
            case 0 -> getX(index);
            case 1 -> getY(index);
            case 2 -> getWeight(index);
            case 3 -> getMin(index);
            case 4 -> getMax(index);
            default -> 0.0;
        };
    }

    @Override
    public double[] getXData() {
        ensureSnapshot();
        return xSnapshot;
    }

    @Override
    public double[] getYData() {
        ensureSnapshot();
        return ySnapshot;
    }

    @Override
    public double[] getLowData() {
        ensureSnapshot();
        return minSnapshot;
    }

    @Override
    public double[] getHighData() {
        ensureSnapshot();
        return maxSnapshot;
    }

    @Override
    public double[] getWeightData() {
        ensureSnapshot();
        return weightSnapshot;
    }

    @Override
    public String getLabel(int index) {
        if (!labelsEnabled || labels == null) return null;
        int count = getPointCount();
        if (index < 0 || index >= count) return null;
        long t = (long) TAIL.getAcquire(this);
        int idx = (int) ((t + index) & mask);
        return labels[idx];
    }

    @Override
    public double[] getDataRange() {
        ensureSnapshot();
        int count = snapshotCount;
        if (count <= 0) return new double[]{0, 0, 0, 0};
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            double x = xSnapshot[i];
            double y = ySnapshot[i];
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        return new double[]{minX, maxX, minY, maxY};
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "Series";
        invalidate();
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        invalidate();
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color color) {
        this.color = color;
        invalidate();
    }

    public CircularChartModel setDispatchOnEdt(boolean enabled) {
        this.dispatchOnEdt = enabled;
        return this;
    }

    @Override
    public void addChangeListener(ChartModelListener listener) {
        if (listener != null) listeners.add(listener);
    }

    @Override
    public void removeChangeListener(ChartModelListener listener) {
        listeners.remove(listener);
    }

    @Override
    public long getUpdateStamp() {
        return updateStamp.get();
    }

    public void clear() {
        long h = (long) HEAD.getAcquire(this);
        TAIL.setRelease(this, h);
        if (labelsEnabled && labels != null) {
            Arrays.fill(labels, null);
        }
        invalidate();
    }

    public void addPoint(ChartPoint p) {
        if (p == null) return;
        addPoint(p.x(), p.y(), p.min(), p.max(), p.weight(), p.label());
    }

    public void addPoint(double x, double y, double weight, String label) {
        addPoint(x, y, y, y, weight, label);
    }

    public void addPoint(double x, double y, double min, double max, double weight, String label) {
        int writerSpins = 0;
        while (!writeLock.compareAndSet(false, true)) {
            if ((++writerSpins & 0x3F) == 0) {
                Thread.onSpinWait();
            }
        }
        try {
            long seq = (long) HEAD.getAcquire(this);
            int idx = (int) (seq & mask);

            long stamp;
            int spins = 0;
            while (true) {
                stamp = (long) SEQ.getAcquire(sequences, idx);
                if ((stamp & 1L) != 0L) {
                    if ((++spins & 0x3F) == 0) {
                        Thread.onSpinWait();
                    }
                    continue;
                }
                if (SEQ.compareAndSet(sequences, idx, stamp, stamp + 1)) {
                    break;
                }
                if ((++spins & 0x3F) == 0) {
                    Thread.onSpinWait();
                }
            }

            xData[idx] = x;
            yData[idx] = y;
            minData[idx] = min;
            maxData[idx] = max;
            weightData[idx] = weight;
            if (labelsEnabled && labels != null) {
                labels[idx] = label;
            }

            VarHandle.releaseFence();
            SEQ.setRelease(sequences, idx, stamp + 2); // even = stable

            long newHead = seq + 1;
            HEAD.setRelease(this, newHead);

            long newTail = newHead - capacity;
            long t = (long) TAIL.getAcquire(this);
            while (newTail > t) {
                if (TAIL.compareAndSet(this, t, newTail)) {
                    break;
                }
                t = (long) TAIL.getAcquire(this);
            }

            invalidate();
        } finally {
            writeLock.set(false);
        }
    }

    public void addXY(double x, double y) {
        addPoint(x, y, 1.0, "");
    }

    public void addXY(double x, double y, String label) {
        addPoint(x, y, 1.0, label);
    }

    public void addOHLC(OHLCBar bar) {
        if (bar == null) return;
        addPoint(bar.time(), bar.close(), bar.low(), bar.high(), bar.open(), null);
    }

    public void addOHLC(double time, double open, double high, double low, double close) {
        addOHLC(new OHLCBar(time, open, high, low, close));
    }

    public void addWithError(ErrorBarPoint point) {
        if (point == null) return;
        addPoint(point.x(), point.y(), point.errorLow(), point.errorHigh(), 1.0, null);
    }

    public void addWithError(double x, double y, double error) {
        addWithError(new ErrorBarPoint(x, y, y - error, y + error));
    }

    public void addWithError(double x, double y, double errorLow, double errorHigh) {
        addWithError(new ErrorBarPoint(x, y, errorLow, errorHigh));
    }

    /**
     * Reads a stable point into the provided buffer.
     *
     * <p>This method uses a per-slot sequence guard to ensure all fields are from the same write.
     * For concurrent usage, prefer this method over calling {@code getX/getY/getMin/getMax}
     * individually.</p>
     *
     * <p>Buffer order: x, y, min, max, weight.</p>
     *
     * @param index  point index in the logical series
     * @param buffer output buffer with length &gt;= 5
     * @return {@code true} when a stable point was read
     */
    public boolean readPoint(int index, double[] buffer) {
        if (buffer == null || buffer.length < 5) return false;
        int count = getPointCount();
        if (index < 0 || index >= count) return false;
        long t = (long) TAIL.getAcquire(this);
        int idx = (int) ((t + index) & mask);
        int spins = 0;
        while (true) {
            long start = (long) SEQ.getAcquire(sequences, idx);
            if ((start & 1L) != 0L) {
                if ((++spins & 0x3F) == 0) {
                    Thread.onSpinWait();
                }
                continue;
            }
            double x = xData[idx];
            double y = yData[idx];
            double min = minData[idx];
            double max = maxData[idx];
            double weight = weightData[idx];
            VarHandle.acquireFence();
            long end = (long) SEQ.getAcquire(sequences, idx);
            if (start == end && (end & 1L) == 0L) {
                buffer[0] = x;
                buffer[1] = y;
                buffer[2] = min;
                buffer[3] = max;
                buffer[4] = weight;
                return true;
            }
            if ((++spins & 0x3F) == 0) {
                Thread.onSpinWait();
            }
        }
    }

    private void ensureSnapshot() {
        long h = (long) HEAD.getAcquire(this);
        long t = (long) TAIL.getAcquire(this);
        if (h == snapshotHead && t == snapshotTail) return;

        long countLong = h - t;
        if (countLong <= 0) {
            snapshotCount = 0;
            snapshotHead = h;
            snapshotTail = t;
            return;
        }
        int count = (int) Math.min(countLong, capacity);
        int start = (int) (t & mask);

        for (int i = 0; i < count; i++) {
            int idx = (start + i) & mask;
            readStable(idx, i);
        }

        snapshotCount = count;
        snapshotHead = h;
        snapshotTail = t;
    }

    private void invalidate() {
        updateStamp.incrementAndGet();
        fireModelChanged();
    }

    private void fireModelChanged() {
        if (dispatchOnEdt && !EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(this::notifyListeners);
            return;
        }
        notifyListeners();
    }

    private void notifyListeners() {
        for (ChartModelListener listener : listeners) {
            listener.modelChanged();
        }
    }

    private static int nextPowerOfTwo(int value) {
        int v = value <= 1 ? 1 : value;
        int highest = Integer.highestOneBit(v);
        return (v == highest) ? v : highest << 1;
    }

    private double readValue(int index, ValueComponent component) {
        int count = getPointCount();
        if (index < 0 || index >= count) return 0.0;
        ReadCache cache = readCache.get();
        if (!cache.valid || cache.index != index) {
            fillCache(index, cache);
        }
        return switch (component) {
            case X -> cache.x;
            case Y -> cache.y;
            case MIN -> cache.min;
            case MAX -> cache.max;
            case WEIGHT -> cache.weight;
        };
    }

    private void fillCache(int index, ReadCache cache) {
        long t = (long) TAIL.getAcquire(this);
        int idx = (int) ((t + index) & mask);
        double x;
        double y;
        double min;
        double max;
        double weight;
        int spins = 0;
        while (true) {
            long start = (long) SEQ.getAcquire(sequences, idx);
            if ((start & 1L) != 0L) {
                if ((++spins & 0x3F) == 0) {
                    Thread.onSpinWait();
                }
                continue;
            }
            x = xData[idx];
            y = yData[idx];
            min = minData[idx];
            max = maxData[idx];
            weight = weightData[idx];
            VarHandle.acquireFence();
            long end = (long) SEQ.getAcquire(sequences, idx);
            if (start == end && (end & 1L) == 0L) {
                cache.index = index;
                cache.valid = true;
                cache.x = x;
                cache.y = y;
                cache.min = min;
                cache.max = max;
                cache.weight = weight;
                return;
            }
            if ((++spins & 0x3F) == 0) {
                Thread.onSpinWait();
            }
        }
    }

    private void readStable(int idx, int snapshotIndex) {
        int spins = 0;
        while (true) {
            long start = (long) SEQ.getAcquire(sequences, idx);
            if ((start & 1L) != 0L) {
                if ((++spins & 0x3F) == 0) {
                    Thread.onSpinWait();
                }
                continue;
            }
            double x = xData[idx];
            double y = yData[idx];
            double min = minData[idx];
            double max = maxData[idx];
            double weight = weightData[idx];
            VarHandle.acquireFence();
            long end = (long) SEQ.getAcquire(sequences, idx);
            if (start == end && (end & 1L) == 0L) {
                xSnapshot[snapshotIndex] = x;
                ySnapshot[snapshotIndex] = y;
                minSnapshot[snapshotIndex] = min;
                maxSnapshot[snapshotIndex] = max;
                weightSnapshot[snapshotIndex] = weight;
                return;
            }
            if ((++spins & 0x3F) == 0) {
                Thread.onSpinWait();
            }
        }
    }

    void readSequencePair(int index, long[] out) {
        if (out == null || out.length < 2) return;
        int count = getPointCount();
        if (index < 0 || index >= count) return;
        long t = (long) TAIL.getAcquire(this);
        int idx = (int) ((t + index) & mask);
        long start = (long) SEQ.getAcquire(sequences, idx);
        VarHandle.acquireFence();
        long end = (long) SEQ.getAcquire(sequences, idx);
        out[0] = start;
        out[1] = end;
    }

    private static final class ReadCache {
        private int index = -1;
        private boolean valid = false;
        private double x;
        private double y;
        private double min;
        private double max;
        private double weight;
    }

    private enum ValueComponent {
        X, Y, MIN, MAX, WEIGHT
    }
}
