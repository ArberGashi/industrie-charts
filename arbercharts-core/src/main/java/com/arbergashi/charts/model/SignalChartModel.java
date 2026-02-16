package com.arbergashi.charts.model;
/**
 * Specialized model for high-frequency signal data (medical/telemetry).
 *
 * <p>Provides multi-channel accessors and optional circular-buffer semantics
 * for real-time streams. Implementations should be zero-allocation in the
 * render path.</p>
 *
 * @since 2.0.0
  * @author Arber Gashi
  * @version 2.0.0
 */
public interface SignalChartModel extends ChartModel {

    /**
     * Number of signal channels contained in this model.
     */
    int getChannelCount();

    /**
     * Sampling rate in Hz (optional). Default: 0 when unknown.
     */
    default double getSampleRateHz() {
        return 0.0;
    }

    /**
     * Returns the per-channel data array (may be larger than logical size).
     */
    double[] getChannelData(int channel);

    /**
     * Returns the signal value at logical index for the given channel.
     */
    default double getValue(int index, int channel) {
        int count = getPointCount();
        if (index < 0 || index >= count) return 0.0;
        double[] arr = getChannelData(channel);
        if (arr == null || index >= arr.length) return 0.0;
        return arr[index];
    }

    /**
     * Whether the backing store is circular.
     */
    default boolean isCircular() {
        return false;
    }

    /**
     * Optional write index for circular buffers (physical index).
     */
    default int getWriteIndex() {
        return -1;
    }

    /**
     * Capacity of the buffer (may exceed logical size).
     */
    default int getCapacity() {
        return getPointCount();
    }
}
