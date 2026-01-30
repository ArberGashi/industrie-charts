package com.arbergashi.charts.engine.forensic;

import com.arbergashi.charts.api.forensic.PlaybackController;
import com.arbergashi.charts.api.forensic.PlaybackFactory;
import com.arbergashi.charts.api.forensic.StreamBufferStrategy;
import com.arbergashi.charts.api.forensic.StreamPlaybackDrive;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.LatencyTracker;

/**
 * Headless load generator for stream playback latency testing.
 */
public final class HighFrequencyStubDriver {
    private static final int[] RATES = {10_000, 25_000, 50_000, 75_000, 100_000};
    private static final long PHASE_NANOS = 10_000_000_000L; // 10s

    private HighFrequencyStubDriver() {
    }

    public static void main(String[] args) {
        DefaultChartModel model = new DefaultChartModel();
        PlaybackController controller = PlaybackFactory.ofController();
        StreamPlaybackDrive drive = PlaybackFactory.ofStreamDrive(model, controller, 8192);
        drive.setBufferStrategy(StreamBufferStrategy.DROP_OLDEST);
        drive.connect("stub://high-frequency");

        long start = System.nanoTime();
        for (int rate : RATES) {
            runPhase(drive, controller, rate);
        }
        long elapsed = System.nanoTime() - start;
        System.out.println("Stub run finished in " + (elapsed / 1_000_000_000.0) + "s");
        drive.disconnect();
    }

    private static void runPhase(StreamPlaybackDrive drive, PlaybackController controller, int rate) {
        LatencyTracker tracker = controller.getLatencyTracker();
        long interval = 1_000_000_000L / rate;
        long phaseStart = System.nanoTime();
        long nextTick = phaseStart;
        int ticks = 0;
        long[] samples = new long[(int) (PHASE_NANOS / interval) + 1];
        while (System.nanoTime() - phaseStart < PHASE_NANOS) {
            long now = System.nanoTime();
            if (now < nextTick) {
                continue;
            }
            double x = ticks * 1.0;
            double y = Math.sin(ticks * 0.01);
            long start = System.nanoTime();
            drive.offer(x, y, y, y, 1.0, null, (byte) 0, (short) 1, now);
            if (ticks < samples.length) {
                samples[ticks] = System.nanoTime() - start;
            }
            ticks++;
            nextTick += interval;
        }
        long p99n = percentileNanos(samples, ticks, 0.99);
        long p999n = percentileNanos(samples, ticks, 0.999);
        String dropped = ChartAssets.getString("Chart.stream.buffer.dropped", "0");
        System.out.println("Rate " + rate + "/s | p99 " + fmtMicros(p99n) + "us | p99.9 " + fmtMicros(p999n)
                + "us | dropped " + dropped);
        if (tracker != null) {
            double p99 = tracker.getP99Millis();
            double p999 = tracker.getP999Millis();
            System.out.println("Tracker p99 " + fmt(p99) + "ms | p99.9 " + fmt(p999) + "ms");
        }
    }

    private static long percentileNanos(long[] samples, int count, double quantile) {
        if (count <= 0) return 0L;
        long[] copy = new long[count];
        System.arraycopy(samples, 0, copy, 0, count);
        java.util.Arrays.sort(copy);
        int idx = (int) Math.ceil(quantile * count) - 1;
        if (idx < 0) idx = 0;
        if (idx >= copy.length) idx = copy.length - 1;
        return copy[idx];
    }

    private static String fmtMicros(long nanos) {
        long micros = Math.round(nanos / 1000.0);
        long intPart = micros / 100L;
        long fracPart = micros % 100L;
        return intPart + "." + (fracPart < 10 ? "0" : "") + fracPart;
    }

    private static String fmt(double ms) {
        long scaled = Math.round(ms * 100.0);
        long intPart = scaled / 100L;
        long fracPart = scaled % 100L;
        return intPart + "." + (fracPart < 10 ? "0" : "") + fracPart;
    }
}
