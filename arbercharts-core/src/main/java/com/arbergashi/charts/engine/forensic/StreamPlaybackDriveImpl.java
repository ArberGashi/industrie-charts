package com.arbergashi.charts.engine.forensic;

import com.arbergashi.charts.api.forensic.PlaybackController;
import com.arbergashi.charts.api.forensic.PlaybackSink;
import com.arbergashi.charts.api.forensic.StreamBufferStrategy;
import com.arbergashi.charts.api.forensic.StreamPlaybackDrive;
import com.arbergashi.charts.model.ProvenanceFlags;
import com.arbergashi.charts.util.ChartAssets;

import java.util.Objects;

/**
 * Lightweight stream playback drive with a zero-allocation ring buffer.
 */
final class StreamPlaybackDriveImpl implements StreamPlaybackDrive {
    private final PlaybackController controller;
    private final PlaybackSink sink;
    private double[] xData;
    private double[] yData;
    private double[] minData;
    private double[] maxData;
    private double[] weightData;
    private String[] labels;
    private byte[] flags;
    private short[] sourceIds;
    private long[] timestamps;

    private StreamBufferStrategy strategy = StreamBufferStrategy.DROP_OLDEST;
    private String source;
    private boolean connected;
    private int head;
    private int tail;
    private int size;
    private volatile long dropped;
    private long lastDropPublish = -1;

    StreamPlaybackDriveImpl(PlaybackController controller, PlaybackSink sink) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.sink = Objects.requireNonNull(sink, "sink");
        resizeBuffers(4096);
    }

    @Override
    public void connect(String source) {
        this.source = source;
        connected = true;
        ChartAssets.setProperty("Chart.stream.buffer.strategy", strategy.name());
    }

    @Override
    public StreamPlaybackDriveImpl setCapacity(int capacity) {
        resizeBuffers(capacity);
        return this;
    }

    @Override
    public int getCapacity() {
        return xData.length;
    }

    @Override
    public void disconnect() {
        connected = false;
        ChartAssets.setProperty("Chart.stream.buffer.dropped", Long.toString(dropped));
        clear();
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public StreamPlaybackDriveImpl setBufferStrategy(StreamBufferStrategy strategy) {
        this.strategy = (strategy != null) ? strategy : StreamBufferStrategy.DROP_OLDEST;
        ChartAssets.setProperty("Chart.stream.buffer.strategy", this.strategy.name());
        return this;
    }

    @Override
    public StreamBufferStrategy getBufferStrategy() {
        return strategy;
    }

    @Override
    public long getDroppedCount() {
        return dropped;
    }

    @Override
    public boolean offer(double x, double y, double min, double max, double weight,
                         String label, byte flag, short sourceId, long timestampNanos) {
        if (!connected) return false;
        long startNanos = System.nanoTime();
        synchronized (this) {
            if (size == xData.length) {
                if (!handleOverflow()) {
                    return false;
                }
            }
            int idx = tail;
            xData[idx] = x;
            yData[idx] = y;
            minData[idx] = min;
            maxData[idx] = max;
            weightData[idx] = weight;
            labels[idx] = label;
            flags[idx] = (flag != 0) ? flag : ProvenanceFlags.ORIGINAL;
            sourceIds[idx] = sourceId;
            timestamps[idx] = timestampNanos;
            tail = (tail + 1) % xData.length;
            size++;
        }
        drain();
        if (controller instanceof DeterministicPlaybackManager manager) {
            long elapsed = System.nanoTime() - startNanos;
            manager.recordLatency(elapsed);
        }
        return true;
    }

    @Override
    public void close() {
        disconnect();
    }

    private boolean handleOverflow() {
        return switch (strategy) {
            case DROP_OLDEST -> {
                head = (head + 1) % xData.length;
                size--;
                dropped++;
                publishDropCount();
                yield true;
            }
            case DROP_NEWEST -> false;
            case COALESCE -> {
                head = (head + 1) % xData.length;
                size--;
                dropped++;
                publishDropCount();
                yield true;
            }
            case BLOCK -> false;
        };
    }

    private void resizeBuffers(int capacity) {
        int next = Math.max(64, capacity);
        boolean hadData = size > 0;
        xData = new double[next];
        yData = new double[next];
        minData = new double[next];
        maxData = new double[next];
        weightData = new double[next];
        labels = new String[next];
        flags = new byte[next];
        sourceIds = new short[next];
        timestamps = new long[next];
        head = 0;
        tail = 0;
        size = 0;
        dropped = 0;
        lastDropPublish = -1;
        if (hadData && strategy == StreamBufferStrategy.BLOCK) {
            connected = false;
        }
    }

    private void publishDropCount() {
        if ((dropped & 0xFF) == 0 && dropped != lastDropPublish) {
            lastDropPublish = dropped;
            ChartAssets.setProperty("Chart.stream.buffer.dropped", Long.toString(dropped));
        }
    }

    private void drain() {
        while (true) {
            int idx;
            double x;
            double y;
            double min;
            double max;
            double weight;
            String label;
            byte flag;
            short sourceId;
            long ts;
            synchronized (this) {
                if (size == 0) {
                    return;
                }
                idx = head;
                x = xData[idx];
                y = yData[idx];
                min = minData[idx];
                max = maxData[idx];
                weight = weightData[idx];
                label = labels[idx];
                flag = flags[idx];
                sourceId = sourceIds[idx];
                ts = timestamps[idx];
                head = (head + 1) % xData.length;
                size--;
            }
            sink.setPoint(x, y, min, max, weight, label, flag, sourceId, ts);
            controller.stepTo(ts);
        }
    }

    private void clear() {
        head = 0;
        tail = 0;
        size = 0;
    }
}
