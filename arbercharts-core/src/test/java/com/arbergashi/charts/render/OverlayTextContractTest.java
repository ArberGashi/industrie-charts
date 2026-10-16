package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.forensic.PlaybackController;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberMatrices;
import com.arbergashi.charts.core.rendering.ArberMatrix;
import com.arbergashi.charts.core.rendering.VoxelBuffer;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.circular.CircularLatencyOverlayRenderer;
import com.arbergashi.charts.render.common.PerformanceAuditRenderer;
import com.arbergashi.charts.render.common.PhysicalScaleRenderer;
import com.arbergashi.charts.render.forensic.PlaybackStatusRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.LatencyTracker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OverlayTextContractTest {
    private final Map<String, String> previousProperties = new LinkedHashMap<>();
    private final DefaultChartModel emptyModel = new DefaultChartModel("Empty overlay host");

    @AfterEach
    void restoreProperties() {
        previousProperties.forEach((key, value) -> {
            if (value == null) ChartAssets.removeProperty(key);
            else ChartAssets.setProperty(key, value);
        });
    }

    @Test
    void auditDrawsMeasuredTextAndRestoresIntersectedClip() {
        property("Chart.performance.audit.enabled", "true");
        PerformanceAuditRenderer renderer = new PerformanceAuditRenderer(latency());
        RecordingCanvas canvas = new RecordingCanvas();
        ArberRect bounds = new ArberRect(20, 30, 420, 200);
        ArberRect originalClip = new ArberRect(25, 35, 400, 190);
        canvas.setClip(originalClip);
        renderer.render(canvas, context(bounds, 4), null);

        Text text = canvas.texts.getFirst();
        assertEquals("PERF p99: 3.00ms | p99.9: 3.00ms", text.value);
        assertEquals(bounds.maxX() - 8 - canvas.measureTextWidth(text.value), text.x, 0.001);
        assertEquals(bounds.y() + 8 + canvas.getTextAscent(), text.y, 0.001);
        assertEquals(originalClip, text.clip);
        assertSame(originalClip, canvas.getClip());
        renderer.render(canvas, context(bounds, 4), null);
        assertSame(text.value, canvas.texts.getLast().value, "Unchanged timing labels should be reused");
    }

    @Test
    void circularLatencyUsesBottomCenterAndWorksWithoutModelSamples() {
        property("Chart.circular.performance.enabled", "true");
        CircularLatencyOverlayRenderer renderer = new CircularLatencyOverlayRenderer(latency());
        RecordingCanvas canvas = new RecordingCanvas();
        ArberRect bounds = new ArberRect(20, 30, 420, 200);
        assertTrue(renderer.supportsEmptyState());
        assertFalse(renderer.isLegendRequired());
        renderer.renderEmptyState(canvas, emptyModel, context(bounds, 4));

        Text text = canvas.texts.getFirst();
        assertEquals("p99: 3.00ms | p99.9: 3.00ms", text.value);
        assertEquals(bounds.centerX(), text.x + canvas.measureTextWidth(text.value) / 2, 0.001);
        assertEquals(bounds.maxY() - 8, text.y + canvas.getTextLineHeight() - canvas.getTextAscent(), 0.001);
        assertEquals(bounds, text.clip);
        assertNull(canvas.getClip());
    }

    @Test
    void playbackDrawsStatusInsteadOfRectangleAndDoesNotAccumulatePrefixes() {
        property("Chart.playback.status.enabled", "true");
        property("Chart.playback.status.alpha", "0.6");
        property("Chart.watchdog.enabled", "true");
        property("Chart.watchdog.level", "WARN");
        property("Chart.watchdog.message", "Delayed");
        property("Chart.watchdog.color.warn", "#FF9900");
        PlaybackStatusRenderer renderer = new PlaybackStatusRenderer(new TestPlayback());
        RecordingCanvas canvas = new RecordingCanvas();
        PlotContext context = context(new ArberRect(10, 15, 440, 200), 4);
        renderer.renderEmptyState(canvas, emptyModel, context);
        renderer.renderEmptyState(canvas, emptyModel, context);

        assertEquals(2, canvas.texts.size());
        assertEquals("Delayed Playback 12.5s @ 2.00x", canvas.texts.getFirst().value);
        assertSame(canvas.texts.getFirst().value, canvas.texts.getLast().value);
        assertEquals(0x99FF9900, canvas.texts.getLast().argb);
        assertEquals(0, canvas.fillCount, "Status must be readable text, not a filled placeholder");
        assertTrue(renderer.supportsEmptyState());
        assertFalse(renderer.isLegendRequired());
    }

    @Test
    void disabledAndUnavailableOverlaysProduceNoText() {
        property("Chart.performance.audit.enabled", "false");
        property("Chart.circular.performance.enabled", "false");
        property("Chart.playback.status.enabled", "false");
        property("Chart.scale.physical.enabled", "false");
        RecordingCanvas canvas = new RecordingCanvas();
        PlotContext context = context(new ArberRect(0, 0, 400, 200), 4);
        new PerformanceAuditRenderer(latency()).render(canvas, context, null);
        new CircularLatencyOverlayRenderer(latency()).render(canvas, emptyModel, context);
        new PlaybackStatusRenderer(new TestPlayback()).render(canvas, emptyModel, context);
        new PhysicalScaleRenderer().render(canvas, emptyModel, context);
        property("Chart.performance.audit.enabled", "true");
        new PerformanceAuditRenderer(new LatencyTracker(32)).render(canvas, context, null);
        assertTrue(canvas.texts.isEmpty());
        assertTrue(canvas.lines.isEmpty());
    }

    @Test
    void millimeterBarsUseContextDensityAcrossResizeAndDeviceScale() {
        property("Chart.scale.physical.enabled", "true");
        property("Chart.scale.physical.y.enabled", "true");
        property("Chart.scale.physical.barMm", "10");
        property("Chart.scale.physical.marginMm", "4");
        PhysicalScaleRenderer renderer = new PhysicalScaleRenderer();
        for (float deviceScale : new float[]{1, 2}) {
            for (int width : new int[]{240, 480}) {
                RecordingCanvas canvas = new RecordingCanvas();
                ChartScale.applyScale(deviceScale,
                        () -> renderer.renderEmptyState(canvas, emptyModel,
                                context(new ArberRect(17, 23, width, 240), 5)));
                assertEquals(6, canvas.lines.size());
                Line horizontal = canvas.lines.getFirst();
                Line vertical = canvas.lines.get(3);
                assertEquals(50, horizontal.x2 - horizontal.x1, 0.001);
                assertEquals(horizontal.y1, horizontal.y2);
                assertEquals(50, vertical.y1 - vertical.y2, 0.001);
                assertEquals(vertical.x1, vertical.x2);
                assertEquals("10 mm", canvas.texts.getFirst().value);
                assertEquals(0, canvas.fillCount);
            }
        }
    }

    @Test
    void physicalMarkersNeverShrinkOrDrawWithInvalidCalibration() {
        property("Chart.scale.physical.enabled", "true");
        property("Chart.scale.physical.barMm", "10");
        property("Chart.scale.physical.marginMm", "4");
        PhysicalScaleRenderer renderer = new PhysicalScaleRenderer();
        for (double density : new double[]{Double.NaN, Double.POSITIVE_INFINITY, 0, -1}) {
            RecordingCanvas canvas = new RecordingCanvas();
            renderer.render(canvas, emptyModel, context(new ArberRect(0, 0, 480, 240), density));
            assertTrue(canvas.lines.isEmpty());
        }
        RecordingCanvas small = new RecordingCanvas();
        renderer.render(small, emptyModel, context(new ArberRect(0, 0, 40, 40), 5));
        assertTrue(small.lines.isEmpty());
        property("Chart.scale.physical.barMm", "2.5");
        RecordingCanvas changed = new RecordingCanvas();
        renderer.render(changed, emptyModel, context(new ArberRect(0, 0, 480, 240), 5));
        assertEquals(12.5, changed.lines.getFirst().x2 - changed.lines.getFirst().x1, 0.001);
        assertEquals("2.5 mm", changed.texts.getFirst().value);
    }

    @Test
    void explicitActivationDoesNotChangeOtherChartsGlobalSettings() {
        property("Chart.performance.audit.enabled", "false");
        property("Chart.scale.physical.enabled", "false");
        RecordingCanvas enabled = new RecordingCanvas();
        PlotContext context = context(new ArberRect(0, 0, 480, 240), 4);
        new PerformanceAuditRenderer(latency()).setEnabled(true).render(enabled, context, null);
        new PhysicalScaleRenderer().setEnabled(true).render(enabled, emptyModel, context);
        assertEquals(2, enabled.texts.size());
        assertFalse(ChartAssets.getBoolean("Chart.performance.audit.enabled", true));
        assertFalse(ChartAssets.getBoolean("Chart.scale.physical.enabled", true));
        RecordingCanvas otherChart = new RecordingCanvas();
        new PerformanceAuditRenderer(latency()).render(otherChart, context, null);
        new PhysicalScaleRenderer().render(otherChart, emptyModel, context);
        assertTrue(otherChart.texts.isEmpty());
        assertTrue(otherChart.lines.isEmpty());
    }

    private void property(String key, String value) {
        if (!previousProperties.containsKey(key)) previousProperties.put(key, ChartAssets.getString(key, null));
        ChartAssets.setProperty(key, value);
    }

    private static LatencyTracker latency() {
        LatencyTracker tracker = new LatencyTracker(32);
        tracker.record(3_000_000L);
        return tracker;
    }

    private static PlotContext context(ArberRect bounds, double density) {
        return new PlotContext() {
            @Override public double getMinX() { return 0; }
            @Override public double getMaxX() { return 1; }
            @Override public double getMinY() { return 0; }
            @Override public double getMaxY() { return 1; }
            @Override public ArberRect getPlotBounds() { return bounds; }
            @Override public double getPixelsPerMillimeter() { return density; }
            @Override public void mapToPixel(double x, double y, double[] out) { out[0] = x; out[1] = y; }
            @Override public void mapToData(double x, double y, double[] out) { out[0] = x; out[1] = y; }
        };
    }

    private record Text(float x, float y, String value, ArberRect clip, int argb) {}
    private record Line(float x1, float y1, float x2, float y2) {}

    private static final class RecordingCanvas implements ArberCanvas {
        private final List<Text> texts = new ArrayList<>();
        private final List<Line> lines = new ArrayList<>();
        private ArberRect clip;
        private int argb;
        private float lastX;
        private float lastY;
        private int fillCount;

        @Override public void setColor(ArberColor color) { argb = color.argb(); }
        @Override public void setStroke(float width) {}
        @Override public void moveTo(float x, float y) { lastX = x; lastY = y; }
        @Override public void lineTo(float x, float y) { lines.add(new Line(lastX, lastY, x, y)); moveTo(x, y); }
        @Override public void drawPolyline(float[] xs, float[] ys, int count) {}
        @Override public void drawRect(float x, float y, float width, float height) {}
        @Override public void fillRect(float x, float y, float width, float height) { fillCount++; }
        @Override public void fillPolygon(float[] xs, float[] ys, int count) { fillCount++; }
        @Override public void drawVoxelField(VoxelBuffer buffer) {}
        @Override public ArberMatrix getTransform() { return ArberMatrices.identity(); }
        @Override public void setClip(ArberRect value) { clip = value; }
        @Override public ArberRect getClip() { return clip; }
        @Override public void drawText(float x, float y, String value) { texts.add(new Text(x, y, value, clip, argb)); }
    }

    private static final class TestPlayback implements PlaybackController {
        @Override public PlaybackController setDeterministic(boolean enabled) { return this; }
        @Override public boolean isDeterministic() { return true; }
        @Override public PlaybackController setSpeed(double speed) { return this; }
        @Override public double getSpeed() { return 2; }
        @Override public void reset(long firstTimestampNanos) {}
        @Override public void stepTo(long tickTimestampNanos) {}
        @Override public void advanceByNanos(long deltaNanos) {}
        @Override public boolean isPlaybackActive() { return true; }
        @Override public long getLastTickNanos() { return 12_500_000_000L; }
        @Override public long getPlaybackElapsedNanos() { return getLastTickNanos(); }
        @Override public long getResolvedNowNanos(long fallbackNowNanos) { return getLastTickNanos(); }
        @Override public void appendStatus(StringBuilder builder) { builder.append("Playback 12.5s @ 2.00x"); }
    }
}
