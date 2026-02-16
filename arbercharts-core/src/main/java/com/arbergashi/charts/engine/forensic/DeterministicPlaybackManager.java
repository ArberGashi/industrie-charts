package com.arbergashi.charts.engine.forensic;

import com.arbergashi.charts.api.forensic.PlaybackController;
import com.arbergashi.charts.util.FastNumberFormatter;
import com.arbergashi.charts.util.LatencyTracker;

/**
 * Controls deterministic playback timing for forensic replays.
 *
 * <p>When enabled, this manager supplies a synthetic "now" timestamp based on
 * recorded tick timestamps. Callers are responsible for stepping the playback
 * timeline using {@link #stepTo(long)} or {@link #advanceByNanos(long)}.</p>
 */
final class DeterministicPlaybackManager implements PlaybackController {
    private static final int LATENCY_WINDOW = 512;

    private boolean deterministic;
    private boolean playbackActive;
    private double speed = 1.0;
    private long playbackNowNanos;
    private long lastTickNanos;
    private long firstTickNanos;
    private final LatencyTracker latencyTracker = new LatencyTracker(LATENCY_WINDOW);
    private int watchdogCounter;
    private boolean watchdogCriticalSent;
    private long lastWatchdogUiNanos;

    /**
     * Enables or disables deterministic playback.
     */
    public DeterministicPlaybackManager setDeterministic(boolean enabled) {
        deterministic = enabled;
        if (!enabled) {
            playbackActive = false;
        }
        return this;
    }

    /**
     * Returns whether deterministic playback is enabled.
     */
    public boolean isDeterministic() {
        return deterministic;
    }

    /**
     * Sets the playback speed multiplier.
     */
    public DeterministicPlaybackManager setSpeed(double speed) {
        if (Double.isFinite(speed) && speed > 0.0) {
            this.speed = speed;
        }
        return this;
    }

    /**
     * Returns the current playback speed multiplier.
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Resets playback to the given timestamp and marks playback active.
     */
    public void reset(long firstTimestampNanos) {
        if (firstTimestampNanos <= 0L) return;
        playbackNowNanos = firstTimestampNanos;
        lastTickNanos = firstTimestampNanos;
        firstTickNanos = firstTimestampNanos;
        playbackActive = true;
    }

    /**
     * Steps playback to a specific tick timestamp.
     */
    public void stepTo(long tickTimestampNanos) {
        if (!deterministic || tickTimestampNanos <= 0L) return;
        lastTickNanos = tickTimestampNanos;
        if (firstTickNanos == 0L) {
            firstTickNanos = tickTimestampNanos;
        }
        if (tickTimestampNanos > playbackNowNanos) {
            playbackNowNanos = tickTimestampNanos;
        }
        playbackActive = true;
    }

    /**
     * Advances playback by a real-time delta scaled by speed.
     */
    public void advanceByNanos(long deltaNanos) {
        if (!deterministic || !playbackActive || deltaNanos <= 0L) return;
        playbackNowNanos += (long) (deltaNanos * speed);
    }

    /**
     * Returns whether playback is active.
     */
    public boolean isPlaybackActive() {
        return deterministic && playbackActive;
    }

    /**
     * Returns the last tick timestamp used by playback.
     */
    public long getLastTickNanos() {
        return lastTickNanos;
    }

    /**
     * Returns the playback elapsed time since the first tick.
     */
    public long getPlaybackElapsedNanos() {
        if (!playbackActive || firstTickNanos <= 0L) return 0L;
        return Math.max(0L, playbackNowNanos - firstTickNanos);
    }

    /**
     * Resolves the current "now" timestamp for signal-integrity checks.
     */
    public long getResolvedNowNanos(long fallbackNowNanos) {
        if (isPlaybackActive()) {
            return playbackNowNanos;
        }
        return fallbackNowNanos;
    }

    /**
     * Appends a human-readable playback status string.
     */
    public void appendStatus(StringBuilder sb) {
        if (sb == null) return;
        sb.setLength(0);
        if (!isPlaybackActive()) {
            sb.append("Playback: OFF");
            return;
        }
        sb.append("Playback ");
        double seconds = getPlaybackElapsedNanos() / 1_000_000_000.0;
        FastNumberFormatter.appendFixed(sb, seconds, 1);
        sb.append("s @ ");
        FastNumberFormatter.appendFixed(sb, speed, 2);
        sb.append('x');
    }

    @Override
    public LatencyTracker getLatencyTracker() {
        return latencyTracker;
    }

    void recordLatency(long nanos) {
        latencyTracker.record(nanos);
        watchdogCounter++;
        if ((watchdogCounter & 0x7F) == 0) {
            setWatchdogState();
        }
    }

    private void setWatchdogState() {
        if (!com.arbergashi.charts.util.ChartAssets.getBoolean("Chart.watchdog.enabled", false)) return;
        double p999 = latencyTracker.getP999Millis();
        double warn = com.arbergashi.charts.util.ChartAssets.getFloat("Chart.watchdog.warn.p999", 1.0f);
        double crit = com.arbergashi.charts.util.ChartAssets.getFloat("Chart.watchdog.crit.p999", 3.0f);
        String dropped = com.arbergashi.charts.util.ChartAssets.getString("Chart.stream.buffer.dropped", "0");
        long dropCount = parseLongSafe(dropped);
        long windowDrops = parseLongSafe(
                com.arbergashi.charts.util.ChartAssets.getString("Chart.watchdog.drop.window", "1000"));
        double dropRate = windowDrops > 0 ? Math.min(1.0, dropCount / (double) windowDrops) : 0.0;
        double dropWarn = com.arbergashi.charts.util.ChartAssets.getFloat("Chart.watchdog.warn.dropRate", 0.005f);
        double dropCrit = com.arbergashi.charts.util.ChartAssets.getFloat("Chart.watchdog.crit.dropRate", 0.02f);

        String level = "OK";
        if (p999 > crit || dropRate > dropCrit) {
            level = "CRITICAL";
        } else if (p999 > warn || dropRate > dropWarn) {
            level = "WARN";
        }
        long now = System.nanoTime();
        long throttleNs = parseLongSafe(
                com.arbergashi.charts.util.ChartAssets.getString("Chart.watchdog.uiThrottleMs", "250")) * 1_000_000L;
        if (now - lastWatchdogUiNanos > throttleNs) {
            lastWatchdogUiNanos = now;
            com.arbergashi.charts.util.ChartAssets.setProperty("Chart.watchdog.level", level);
        }
        if ("CRITICAL".equals(level) && !watchdogCriticalSent) {
            watchdogCriticalSent = true;
            com.arbergashi.charts.util.ChartAssets.setProperty("Chart.watchdog.critical.trigger", "true");
        }
        if ("OK".equals(level)) {
            watchdogCriticalSent = false;
            com.arbergashi.charts.util.ChartAssets.setProperty("Chart.watchdog.critical.trigger", "false");
        }
    }

    private static long parseLongSafe(String value) {
        if (value == null || value.isEmpty()) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
