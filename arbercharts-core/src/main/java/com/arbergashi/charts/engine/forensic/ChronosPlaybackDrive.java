package com.arbergashi.charts.engine.forensic;

import com.arbergashi.charts.api.forensic.PlaybackController;
import com.arbergashi.charts.api.forensic.PlaybackDrive;
import com.arbergashi.charts.api.forensic.PlaybackSink;
import com.arbergashi.charts.model.CircularChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.model.ProvenanceFlags;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Drives deterministic playback of historical ticks into a chart model.
 *
 * <p>This helper feeds ticks based on their original timestampNanos and a configurable playback speed.</p>
 */
final class ChronosPlaybackDrive implements PlaybackDrive {
    private static final long DEFAULT_TICK_INTERVAL_NANOS = 1_000_000L; // 1 ms

    private final PlaybackController playbackManager;
    private final PlaybackSink sink;

    private double[] xData = new double[0];
    private double[] yData = new double[0];
    private double[] minData = new double[0];
    private double[] maxData = new double[0];
    private double[] weightData = new double[0];
    private String[] labels = new String[0];
    private byte[] flags = new byte[0];
    private short[] sourceIds = new short[0];
    private long[] timestamps = new long[0];
    private int size;
    private int index;

    private ScheduledExecutorService scheduler;
    private boolean running;
    private long lastWallNanos;

    ChronosPlaybackDrive(PlaybackController playbackManager, PlaybackSink sink) {
        this.playbackManager = Objects.requireNonNull(playbackManager, "playbackManager");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    static ChronosPlaybackDrive forDefaultModel(DefaultChartModel model, PlaybackController manager) {
        return new ChronosPlaybackDrive(manager, (x, y, min, max, weight, label, flag, sourceId, ts) ->
                model.setPoint(x, y, min, max, weight, label, flag, sourceId, ts));
    }

    static ChronosPlaybackDrive forCircularModel(CircularChartModel model, PlaybackController manager) {
        return new ChronosPlaybackDrive(manager, (x, y, min, max, weight, label, flag, sourceId, ts) ->
                model.setPoint(x, y, min, max, weight, label, flag, sourceId, ts));
    }

    /**
     * Loads historical ticks from arrays. Arrays are used directly; no copies are made.
     */
    @Override
    public void load(double[] x, double[] y, double[] min, double[] max, double[] weight,
                     String[] labels, byte[] flags, short[] sourceIds, long[] timestamps) {
        this.xData = (x != null) ? x : new double[0];
        this.yData = (y != null) ? y : new double[0];
        this.minData = (min != null) ? min : new double[0];
        this.maxData = (max != null) ? max : new double[0];
        this.weightData = (weight != null) ? weight : new double[0];
        this.labels = (labels != null) ? labels : new String[0];
        this.flags = (flags != null) ? flags : new byte[0];
        this.sourceIds = (sourceIds != null) ? sourceIds : new short[0];
        this.timestamps = (timestamps != null) ? timestamps : new long[0];
        this.size = Math.min(this.timestamps.length, Math.min(this.xData.length, this.yData.length));
        this.index = 0;
    }

    /**
     * Starts playback using a high-resolution scheduler.
     */
    public void start() {
        if (running || size == 0) return;
        running = true;
        playbackManager.setDeterministic(true);
        playbackManager.reset(timestamps[0]);
        lastWallNanos = System.nanoTime();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "chronos-playback");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::tick, 0L, DEFAULT_TICK_INTERVAL_NANOS, TimeUnit.NANOSECONDS);
    }

    /**
     * Stops playback and shuts down the scheduler.
     */
    public void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void close() {
        stop();
        clear();
    }

    /**
     * Clears loaded playback data references.
     */
    public void clear() {
        xData = new double[0];
        yData = new double[0];
        minData = new double[0];
        maxData = new double[0];
        weightData = new double[0];
        labels = new String[0];
        flags = new byte[0];
        sourceIds = new short[0];
        timestamps = new long[0];
        size = 0;
        index = 0;
    }

    private void tick() {
        if (!running) return;
        long startNanos = System.nanoTime();
        long now = System.nanoTime();
        long delta = Math.max(0L, now - lastWallNanos);
        lastWallNanos = now;

        playbackManager.advanceByNanos(delta);
        long playbackNow = playbackManager.getResolvedNowNanos(0L);

        while (index < size && timestamps[index] <= playbackNow) {
            emit(index);
            playbackManager.stepTo(timestamps[index]);
            index++;
        }
        if (index >= size) {
            stop();
        }
        if (playbackManager instanceof DeterministicPlaybackManager manager) {
            long elapsed = System.nanoTime() - startNanos;
            manager.recordLatency(elapsed);
        }
    }

    private void emit(int i) {
        double x = xData[i];
        double y = yData[i];
        double min = (i < minData.length) ? minData[i] : y;
        double max = (i < maxData.length) ? maxData[i] : y;
        double weight = (i < weightData.length) ? weightData[i] : 1.0;
        String label = (i < labels.length) ? labels[i] : null;
        byte flag = (i < flags.length) ? flags[i] : ProvenanceFlags.ORIGINAL;
        short sourceId = (i < sourceIds.length) ? sourceIds[i] : 0;
        long ts = (i < timestamps.length) ? timestamps[i] : 0L;
        sink.setPoint(x, y, min, max, weight, label, flag, sourceId, ts);
    }

}
