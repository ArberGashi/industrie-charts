package com.arbergashi.charts.util;
/**
 * Rolling latency tracker for render timings (zero-allocation).
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class LatencyTracker {
    private static final long BIN_NS = 100_000L; // 0.1ms
    private static final long MAX_NS = 50_000_000L; // 50ms
    private static final int BIN_COUNT = (int) (MAX_NS / BIN_NS) + 1;

    private final long[] samples;
    private final int[] histogram;
    private int index;
    private int count;

    public LatencyTracker(int windowSize) {
        int size = Math.max(32, windowSize);
        this.samples = new long[size];
        this.histogram = new int[BIN_COUNT];
    }

    public void record(long nanos) {
        if (nanos < 0L) return;
        long clipped = nanos > MAX_NS ? MAX_NS : nanos;
        int bin = (int) (clipped / BIN_NS);
        if (count == samples.length) {
            long old = samples[index];
            int oldBin = (int) (old / BIN_NS);
            if (oldBin >= 0 && oldBin < histogram.length) {
                histogram[oldBin]--;
            }
        } else {
            count++;
        }
        samples[index] = clipped;
        histogram[bin]++;
        index++;
        if (index >= samples.length) index = 0;
    }

    public int getSampleCount() {
        return count;
    }

    public double getP99Millis() {
        return percentileMillis(0.99);
    }

    public double getP999Millis() {
        return percentileMillis(0.999);
    }

    private double percentileMillis(double quantile) {
        if (count == 0) return 0.0;
        int target = (int) Math.ceil(quantile * count);
        int acc = 0;
        for (int i = 0; i < histogram.length; i++) {
            acc += histogram[i];
            if (acc >= target) {
                return (i * BIN_NS) / 1_000_000.0;
            }
        }
        return MAX_NS / 1_000_000.0;
    }
}
