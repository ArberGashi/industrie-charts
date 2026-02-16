package com.arbergashi.charts.util;
/**
 * Calculates "nice" numbers for graph axes.
 * <p>
 * This class computes a "nice" range and tick spacing for plotting numeric axes.
 * It expands the supplied data range to round, human-friendly values and
 * computes evenly spaced tick marks (for example: 0, 10, 20 instead of 0, 17.5, 35).
 * <p>
 * Usage example:
 * <pre>
 * NiceScale ns = new NiceScale(minValue, maxValue);
 * double[] ticks = ns.getTicks();
 * </pre>
 *
 * The algorithm is based on selecting a "nice" fraction (1, 2, 5, 10) scaled by a
 * power of ten to produce round tick spacings.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class NiceScale {

    private double minPoint;
    private double maxPoint;
    private double maxTicks = 10;
    private double tickSpacing;
    private double range;
    private double niceMin;
    private double niceMax;

    /**
     * Create a NiceScale for the provided data range.
     *
     * @param min the minimum data value
     * @param max the maximum data value
     */
    public NiceScale(double min, double max) {
        this.minPoint = min;
        this.maxPoint = max;
        calculate();
    }

    /**
     * Update the data range and recalculate tick spacing and nice bounds.
     *
     * @param min the new minimum data value
     * @param max the new maximum data value
     */
    public NiceScale setRange(double min, double max) {
        this.minPoint = min;
        this.maxPoint = max;
        calculate();
        return this;
    }

    /**
     * Set the maximum number of ticks (approximate) desired on the axis.
     * The algorithm will attempt to return at most this many ticks; the exact
     * number may be less or slightly different to preserve "nice" spacing.
     *
     * @param maxTicks maximum number of ticks to aim for (must be &gt;= 2 for sensible results)
     */
    public NiceScale setMaxTicks(double maxTicks) {
        this.maxTicks = maxTicks;
        calculate();
        return this;
    }

    private void calculate() {
        this.range = niceNum(maxPoint - minPoint, false);
        this.tickSpacing = niceNum(range / (maxTicks - 1), true);
        this.niceMin = Math.floor(minPoint / tickSpacing) * tickSpacing;
        this.niceMax = Math.ceil(maxPoint / tickSpacing) * tickSpacing;
    }

    /**
     * Convert a raw range to a "nice" number — a rounded value using 1, 2, 5 or 10
     * multiplied by a power of ten.
     *
     * @param range the raw numeric range to convert
     * @param round if true, the fraction portion will be rounded to the nearest
     *              nice fraction; if false, it will be rounded up (ceiling behavior)
     * @return a "nice" number close to the provided range
     */
    private double niceNum(double range, boolean round) {
        double exponent; /** exponent of range */
        double fraction; /** fractional part of range */
        double niceFraction; /** nice, rounded fraction */

        exponent = Math.floor(Math.log10(range));
        fraction = range / Math.pow(10, exponent);

        if (round) {
            if (fraction < 1.5) niceFraction = 1;
            else if (fraction < 3) niceFraction = 2;
            else if (fraction < 7) niceFraction = 5;
            else niceFraction = 10;
        } else {
            if (fraction <= 1) niceFraction = 1;
            else if (fraction <= 2) niceFraction = 2;
            else if (fraction <= 5) niceFraction = 5;
            else niceFraction = 10;
        }

        return niceFraction * Math.pow(10, exponent);
    }

    /**
     * Return the computed tick spacing (distance between adjacent ticks).
     *
     * @return spacing between ticks
     */
    public double getTickSpacing() {
        return tickSpacing;
    }

    /**
     * Get the smallest "nice" value that bounds the data minimum.
     *
     * @return lower bound (inclusive) of the nice range
     */
    public double getNiceMin() {
        return niceMin;
    }

    /**
     * Get the largest "nice" value that bounds the data maximum.
     *
     * @return upper bound (inclusive) of the nice range
     */
    public double getNiceMax() {
        return niceMax;
    }

    /** Cached tick array to avoid repeated allocations (Zero-GC). */
    private double[] cachedTicks = new double[32];
    private int cachedTickCount = 0;

    /**
     * Returns an array of tick positions from {@link #getNiceMin()} to {@link #getNiceMax()}
     * spaced by {@link #getTickSpacing()}.
     *
     * <p><strong>Note:</strong> The returned array is reused internally. Copy if needed beyond
     * the current frame.</p>
     *
     * @return an array of tick values. Returns an empty array if spacing is non-positive
     *         or if the computed number of ticks is non-positive.
     */
    public double[] getTicks() {
        if (tickSpacing <= 0) {
            cachedTickCount = 0;
            return cachedTicks;
        }
        int count = (int) Math.round((niceMax - niceMin) / tickSpacing) + 1;
        if (count <= 0) {
            cachedTickCount = 0;
            return cachedTicks;
        }

        // Resize cached array if needed (rare, Zero-GC friendly)
        if (count > cachedTicks.length) {
            cachedTicks = new double[Math.max(count, cachedTicks.length * 2)];
        }

        cachedTickCount = count;
        double v = niceMin;
        for (int i = 0; i < count; i++) {
            cachedTicks[i] = v;
            v += tickSpacing;
        }
        return cachedTicks;
    }

    /**
     * Returns the number of valid ticks in the array returned by {@link #getTicks()}.
     *
     * @return tick count for Zero-GC iteration
     */
    public int getTickCount() {
        return cachedTickCount;
    }

    /**
     * Iterates over tick values without allocating a new array.
     *
     * <p><strong>Zero-GC:</strong> Use this method for allocation-free tick iteration.</p>
     *
     * @param consumer callback invoked for each tick value
     */
    public void forEachTick(java.util.function.DoubleConsumer consumer) {
        if (tickSpacing <= 0) return;
        int count = (int) Math.round((niceMax - niceMin) / tickSpacing) + 1;
        if (count <= 0) return;
        double v = niceMin;
        for (int i = 0; i < count; i++) {
            consumer.accept(v);
            v += tickSpacing;
        }
    }

    /**
     * Supported scale types for axes produced by NiceScale.
     */
    public enum ScaleMode {LINEAR, LOGARITHMIC}
}