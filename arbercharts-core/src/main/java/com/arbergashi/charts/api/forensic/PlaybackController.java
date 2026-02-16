package com.arbergashi.charts.api.forensic;

import com.arbergashi.charts.util.LatencyTracker;
/**
 * Public playback controller interface for deterministic forensic replays.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 2.0.0
 */
public interface PlaybackController {
    PlaybackController setDeterministic(boolean enabled);
    boolean isDeterministic();
    PlaybackController setSpeed(double speed);
    double getSpeed();
    void reset(long firstTimestampNanos);
    void stepTo(long tickTimestampNanos);
    void advanceByNanos(long deltaNanos);
    boolean isPlaybackActive();
    long getLastTickNanos();
    long getPlaybackElapsedNanos();
    long getResolvedNowNanos(long fallbackNowNanos);
    void appendStatus(StringBuilder sb);

    /**
     * Returns an optional latency tracker for deterministic playback diagnostics.
     */
    default LatencyTracker getLatencyTracker() {
        return null;
    }
}
