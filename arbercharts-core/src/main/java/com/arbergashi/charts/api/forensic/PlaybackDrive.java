package com.arbergashi.charts.api.forensic;
/**
 * Public playback drive interface for deterministic replays.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public interface PlaybackDrive extends AutoCloseable {
    void load(double[] x, double[] y, double[] min, double[] max, double[] weight,
              String[] labels, byte[] flags, short[] sourceIds, long[] timestamps);
    void start();
    void stop();
    boolean isRunning();
}
