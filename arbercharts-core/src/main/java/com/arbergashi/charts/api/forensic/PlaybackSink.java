package com.arbergashi.charts.api.forensic;

@FunctionalInterface
/**
 * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public interface PlaybackSink {
    void setPoint(double x, double y, double min, double max, double weight, String label,
                  byte flag, short sourceId, long timestampNanos);
}
