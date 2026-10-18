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
 * @since 2025-06-01
 */
public class NiceScale {
    private static final int MAX_REQUESTED_TICKS = 10000;
    private static final int MAX_GENERATED_TICKS = 2 * MAX_REQUESTED_TICKS + 2;

    private double minPoint;
    private double maxPoint;
    private double maxTicks = 10;
    private double tickSpacing;
    private double niceMin;
    private double niceMax;
    private double firstTickIndex;
    private double lastTickIndex;

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
     * @param maxTicks approximate target, clamped to 2 through 10000;
     *                 non-finite values restore the default of 10
     */
    public NiceScale setMaxTicks(double maxTicks) {
        this.maxTicks = Double.isFinite(maxTicks) ? Math.clamp(maxTicks, 2, MAX_REQUESTED_TICKS) : 10;
        calculate();
        return this;
    }

    private void calculate() {
        cachedTickCount = 0;
        if (!Double.isFinite(minPoint) || !Double.isFinite(maxPoint) || maxPoint < minPoint) {
            tickSpacing = niceMin = niceMax = Double.NaN;
            return;
        }
        if (minPoint == maxPoint) {
            tickSpacing = minPoint == 0 ? 1 : Math.max(Math.ulp(minPoint), niceNum(Math.abs(minPoint) * 0.1, true));
            niceMin = niceMax = minPoint;
            return;
        }
        double range = maxPoint - minPoint;
        double rawSpacing = Double.isFinite(range) ? niceNum(range, false) / (maxTicks - 1)
                : maxPoint / (maxTicks - 1) - minPoint / (maxTicks - 1);
        if (!Double.isFinite(rawSpacing)) rawSpacing = Double.MAX_VALUE;
        double resolution = Math.max(Math.ulp(minPoint), Math.ulp(maxPoint));
        tickSpacing = Math.max(niceNum(rawSpacing, true), resolution);
        firstTickIndex = Math.floor(minPoint / tickSpacing);
        lastTickIndex = Math.ceil(maxPoint / tickSpacing);
        niceMin = firstTickIndex * tickSpacing;
        niceMax = lastTickIndex * tickSpacing;
        // Rounding of a large quotient must not move a bound into the data range.
        if (niceMin > minPoint) {
            firstTickIndex--;
            niceMin = firstTickIndex * tickSpacing;
        }
        if (niceMax < maxPoint) {
            lastTickIndex++;
            niceMax = lastTickIndex * tickSpacing;
        }
        if (!Double.isFinite(niceMin)) niceMin = minPoint;
        if (!Double.isFinite(niceMax)) niceMax = maxPoint;
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
        if (!(range > 0)) return Double.MIN_VALUE;
        double exponent; /** exponent of range */
        double fraction; /** fractional part of range */
        double niceFraction; /** nice, rounded fraction */

        exponent = Math.floor(Math.log10(range));
        double power = Math.pow(10, exponent);
        if (power == 0) return range;
        fraction = range / power;

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

        double nice = niceFraction * power;
        return Double.isFinite(nice) ? nice : range;
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
     * @return the reused storage; only the first {@link #getTickCount()} elements
     *         are valid. Non-finite or reversed ranges have zero ticks; constant
     *         finite ranges have one tick. Valid ticks are finite and strictly increasing.
     */
    public double[] getTicks() {
        cachedTickCount = 0;
        if (!(tickSpacing > 0) || !Double.isFinite(tickSpacing)) {
            return cachedTicks;
        }
        if (minPoint == maxPoint) {
            cachedTicks[0] = minPoint;
            cachedTickCount = 1;
            return cachedTicks;
        }
        int count = (int) Math.min(MAX_GENERATED_TICKS, lastTickIndex - firstTickIndex + 1);

        // Resize cached array if needed (rare, Zero-GC friendly)
        if (count > cachedTicks.length) {
            cachedTicks = new double[Math.max(count, cachedTicks.length * 2)];
        }

        for (int i = 0; i < count; i++) {
            // Use integer multiples instead of cumulative addition or subtracting
            // a large origin. This preserves exact zero ticks across wide ranges.
            double tick = (firstTickIndex + i) * tickSpacing;
            if (!Double.isFinite(tick)) continue;
            if (cachedTickCount > 0 && tick <= cachedTicks[cachedTickCount - 1]) break;
            cachedTicks[cachedTickCount++] = tick;
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
     * Iterates over cached tick positions.
     *
     * <p>Uses the same bounded tick sequence as {@link #getTicks()}; storage is
     * reused and can grow when the requested tick count increases.</p>
     *
     * @param consumer callback invoked for each tick value
     */
    public void forEachTick(java.util.function.DoubleConsumer consumer) {
        double[] ticks = getTicks();
        for (int i = 0; i < cachedTickCount; i++) {
            consumer.accept(ticks[i]);
        }
    }

    /**
     * Supported scale types for axes produced by NiceScale.
     */
    public enum ScaleMode {LINEAR, LOGARITHMIC}
}
