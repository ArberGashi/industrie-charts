package com.arbergashi.charts.api.forensic;

import com.arbergashi.charts.model.CircularChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.util.ChartAssets;

import java.lang.reflect.Constructor;
/**
 * Factory for forensic playback components.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 2.0.0
 */
public final class PlaybackFactory {
    private static final String MANAGER_CLASS = "com.arbergashi.charts.engine.forensic.DeterministicPlaybackManager";
    private static final String DRIVE_CLASS = "com.arbergashi.charts.engine.forensic.ChronosPlaybackDrive";
    private static final String STREAM_DRIVE_CLASS = "com.arbergashi.charts.engine.forensic.StreamPlaybackDriveImpl";
    private static final String STREAM_CAPACITY_KEY = "Chart.stream.buffer.default_capacity";

    private PlaybackFactory() {
    }

    public static PlaybackController ofController() {
        try {
            Class<?> cls = Class.forName(MANAGER_CLASS);
            Constructor<?> ctor = cls.getDeclaredConstructor();
            ctor.setAccessible(true);
            return (PlaybackController) ctor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create playback controller", e);
        }
    }

    public static PlaybackDrive ofChronosDrive(DefaultChartModel model, PlaybackController controller) {
        PlaybackSink sink = (x, y, min, max, weight, label, flag, sourceId, ts) ->
                model.setPoint(x, y, min, max, weight, label, flag, sourceId, ts);
        return ofDrive(controller, sink);
    }

    public static PlaybackDrive ofChronosDrive(CircularChartModel model, PlaybackController controller) {
        PlaybackSink sink = (x, y, min, max, weight, label, flag, sourceId, ts) ->
                model.setPoint(x, y, min, max, weight, label, flag, sourceId, ts);
        return ofDrive(controller, sink);
    }

    public static StreamPlaybackDrive ofStreamDrive(DefaultChartModel model, PlaybackController controller) {
        PlaybackSink sink = (x, y, min, max, weight, label, flag, sourceId, ts) ->
                model.setPoint(x, y, min, max, weight, label, flag, sourceId, ts);
        return ofStreamDrive(controller, sink, -1);
    }

    public static StreamPlaybackDrive ofStreamDrive(CircularChartModel model, PlaybackController controller) {
        PlaybackSink sink = (x, y, min, max, weight, label, flag, sourceId, ts) ->
                model.setPoint(x, y, min, max, weight, label, flag, sourceId, ts);
        return ofStreamDrive(controller, sink, -1);
    }

    public static StreamPlaybackDrive ofStreamDrive(DefaultChartModel model, PlaybackController controller,
                                                        int capacity) {
        PlaybackSink sink = (x, y, min, max, weight, label, flag, sourceId, ts) ->
                model.setPoint(x, y, min, max, weight, label, flag, sourceId, ts);
        return ofStreamDrive(controller, sink, capacity);
    }

    public static StreamPlaybackDrive ofStreamDrive(CircularChartModel model, PlaybackController controller,
                                                        int capacity) {
        PlaybackSink sink = (x, y, min, max, weight, label, flag, sourceId, ts) ->
                model.setPoint(x, y, min, max, weight, label, flag, sourceId, ts);
        return ofStreamDrive(controller, sink, capacity);
    }

    private static PlaybackDrive ofDrive(PlaybackController controller, PlaybackSink sink) {
        if (controller == null) throw new IllegalArgumentException("controller required");
        if (sink == null) throw new IllegalArgumentException("sink required");
        try {
            Class<?> driveClass = Class.forName(DRIVE_CLASS);
            Constructor<?> ctor = driveClass.getDeclaredConstructor(PlaybackController.class, PlaybackSink.class);
            ctor.setAccessible(true);
            Object drive = ctor.newInstance(controller, sink);
            return (PlaybackDrive) drive;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create playback drive", e);
        }
    }

    private static StreamPlaybackDrive ofStreamDrive(PlaybackController controller, PlaybackSink sink,
                                                         int capacity) {
        if (controller == null) throw new IllegalArgumentException("controller required");
        if (sink == null) throw new IllegalArgumentException("sink required");
        try {
            Class<?> driveClass = Class.forName(STREAM_DRIVE_CLASS);
            Constructor<?> ctor = driveClass.getDeclaredConstructor(PlaybackController.class, PlaybackSink.class);
            ctor.setAccessible(true);
            Object drive = ctor.newInstance(controller, sink);
            StreamPlaybackDrive streamDrive = (StreamPlaybackDrive) drive;
            int resolved = capacity;
            if (resolved <= 0) {
                resolved = ChartAssets.getInt(STREAM_CAPACITY_KEY, 4096);
            }
            if (resolved > 0) {
                streamDrive.setCapacity(resolved);
            }
            return streamDrive;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create stream playback drive", e);
        }
    }
}
