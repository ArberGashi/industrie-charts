package com.arbergashi.charts.api.forensic;
/**
 * Interface for live stream playback drives (TCP/UDP/WebSocket ingestion).
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public interface StreamPlaybackDrive extends AutoCloseable {
    void connect(String source);
    StreamPlaybackDrive setCapacity(int capacity);
    int getCapacity();
    void disconnect();
    boolean isConnected();
    StreamPlaybackDrive setBufferStrategy(StreamBufferStrategy strategy);
    StreamBufferStrategy getBufferStrategy();
    long getDroppedCount();
    boolean offer(double x, double y, double min, double max, double weight,
                  String label, byte flag, short sourceId, long timestampNanos);
}
