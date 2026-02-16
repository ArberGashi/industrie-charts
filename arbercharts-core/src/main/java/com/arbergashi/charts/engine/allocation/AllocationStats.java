package com.arbergashi.charts.engine.allocation;

/**
 * Allocation statistics for Zero-GC monitoring.
 *
 * <p>This class holds cache hit/miss statistics for the {@link ZeroAllocPool}.
 * Used to monitor the effectiveness of object pooling and identify potential
 * optimization opportunities.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * AllocationStats stats = ZeroAllocPool.getStats();
 *
 * System.out.printf("Stroke cache: %.1f%% hit rate%n",
 *     stats.getStrokeHitRate() * 100);
 * System.out.printf("Color cache: %.1f%% hit rate%n",
 *     stats.getColorHitRate() * 100);
 * }</pre>
 *
 * @since 2.0.0
 * @see ZeroAllocPool#getStats()
 */
public final class AllocationStats {

    /** Number of successful stroke cache lookups. */
    public final long strokeHits;

    /** Number of stroke cache misses (new object created). */
    public final long strokeMisses;

    /** Number of successful color cache lookups. */
    public final long colorHits;

    /** Number of color cache misses (new object created). */
    public final long colorMisses;

    /**
     * Creates allocation statistics.
     *
     * @param strokeHits stroke cache hits
     * @param strokeMisses stroke cache misses
     * @param colorHits color cache hits
     * @param colorMisses color cache misses
     */
    AllocationStats(long strokeHits, long strokeMisses, long colorHits, long colorMisses) {
        this.strokeHits = strokeHits;
        this.strokeMisses = strokeMisses;
        this.colorHits = colorHits;
        this.colorMisses = colorMisses;
    }

    /**
     * Calculates stroke cache hit rate.
     *
     * @return hit rate between 0.0 and 1.0
     */
    public double getStrokeHitRate() {
        long total = strokeHits + strokeMisses;
        return total == 0 ? 0.0 : (double) strokeHits / total;
    }

    /**
     * Calculates color cache hit rate.
     *
     * @return hit rate between 0.0 and 1.0
     */
    public double getColorHitRate() {
        long total = colorHits + colorMisses;
        return total == 0 ? 0.0 : (double) colorHits / total;
    }

    /**
     * Returns overall cache hit rate (strokes + colors combined).
     *
     * @return overall hit rate between 0.0 and 1.0
     */
    public double getOverallHitRate() {
        long totalHits = strokeHits + colorHits;
        long totalMisses = strokeMisses + colorMisses;
        long total = totalHits + totalMisses;
        return total == 0 ? 0.0 : (double) totalHits / total;
    }

    @Override
    public String toString() {
        return String.format("Stroke: %.1f%% hit rate (%d/%d), Color: %.1f%% hit rate (%d/%d)",
            getStrokeHitRate() * 100, strokeHits, strokeHits + strokeMisses,
            getColorHitRate() * 100, colorHits, colorHits + colorMisses);
    }
}

