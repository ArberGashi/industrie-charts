package com.arbergashi.charts.demo;

import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberFont;
import com.arbergashi.charts.bridge.server.ImageBufferCanvas;
import com.arbergashi.charts.bridge.swing.AwtCanvasAdapter;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.circular.CircularLatencyOverlayRenderer;
import com.arbergashi.charts.render.common.PerformanceAuditRenderer;
import com.arbergashi.charts.render.common.PhysicalScaleRenderer;
import com.arbergashi.charts.render.forensic.PlaybackStatusRenderer;
import com.arbergashi.charts.api.forensic.PlaybackController;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.LatencyTracker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class CanvasTextContractTest {
    private static final int WIDTH = 480;
    private static final int HEIGHT = 240;
    private static final ArberFont FONT = new ArberFont("SansSerif", ArberFont.PLAIN, 24f);
    private final Map<String, String> previousProperties = new LinkedHashMap<>();

    @AfterEach
    void restoreProperties() {
        previousProperties.forEach((key, value) -> {
            if (value == null) ChartAssets.removeProperty(key);
            else ChartAssets.setProperty(key, value);
        });
    }

    @ParameterizedTest
    @EnumSource(Backend.class)
    void glyphsUseTheSelectedFontAndLeftBaseline(Backend backend) {
        try (Target target = backend.create()) {
            ArberCanvas canvas = target.canvas;
            canvas.setFont(FONT);
            canvas.setColor(new ArberColor(0xFF2486C7));
            float baseline = 60f;
            float advance = canvas.measureTextWidth("Hg");
            float ascent = canvas.getTextAscent();
            float height = canvas.getTextLineHeight();
            canvas.drawText(20, baseline, "Hg");

            int count = 0;
            int solidPixels = 0;
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    int pixel = target.pixel(x, y);
                    if (pixel == 0) continue;
                    count++;
                    assertTrue(x >= 20 && x <= 20 + advance + 1);
                    assertTrue(y >= baseline - ascent && y < baseline - ascent + height);
                    assertTextColor(0x2486C7, pixel);
                    if (pixel >>> 24 == 255) solidPixels++;
                }
            }
            assertTrue(count > 50, "Backend must produce actual glyph pixels");
            assertTrue(solidPixels > 10, "Glyph interiors retain the selected color's opacity");
            assertTrue(canvas.measureTextWidth("WWW") > canvas.measureTextWidth("iii"),
                    "Metrics must measure the selected proportional font");
            assertTrue(ascent > 0 && ascent < height);
        }
    }

    @ParameterizedTest
    @EnumSource(Backend.class)
    void fontSizeChangesMetricsAndDefaultCanBeRestored(Backend backend) {
        try (Target target = backend.create()) {
            ArberCanvas canvas = target.canvas;
            canvas.setFont(null);
            float defaultWidth = canvas.measureTextWidth("Measurement");
            float defaultHeight = canvas.getTextLineHeight();
            canvas.setFont(FONT);
            assertTrue(canvas.measureTextWidth("Measurement") > defaultWidth * 1.7);
            assertTrue(canvas.getTextLineHeight() > defaultHeight * 1.5);
            canvas.setFont(null);
            assertEquals(defaultWidth, canvas.measureTextWidth("Measurement"));
            assertEquals(defaultHeight, canvas.getTextLineHeight());
            canvas.setFont(FONT);
            assertTrue(canvas.measureTextWidth("Measurement") > defaultWidth * 1.7,
                    "Restoring a cached descriptor after the default must select that font again");
            assertEquals(0, canvas.measureTextWidth(null));
            assertEquals(0, canvas.measureTextWidth(""));
        }
    }

    @ParameterizedTest
    @EnumSource(Backend.class)
    void textRespectsClipAlphaAndLeavesGeometryStateIntact(Backend backend) {
        try (Target target = backend.create()) {
            ArberCanvas canvas = target.canvas;
            ArberRect clip = new ArberRect(30, 20, 50, 85);
            canvas.setClip(clip);
            canvas.setColor(new ArberColor(0x80FF4000));
            canvas.setStroke(3f);
            canvas.setFont(FONT);
            canvas.moveTo(35, 90);
            canvas.drawText(0, 42, "MMMMMMMMMMMM");
            assertEquals(clip, canvas.getClip());
            canvas.lineTo(75, 90);
            assertEquals(128, target.pixel(50, 90) >>> 24);
            assertEquals(128, target.pixel(50, 89) >>> 24, "Text must preserve the current stroke");
            assertEquals(0, target.pixel(50, 65), "Text must not move the current geometry point");
            int glyphs = 0;
            int solidGlyphs = 0;
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    int pixel = target.pixel(x, y);
                    if (pixel == 0) continue;
                    assertTrue(x >= 30 && x < 80 && y >= 20 && y < 105);
                    int alpha = pixel >>> 24;
                    assertTrue(alpha > 0 && alpha <= 128, "Antialiased edges may reduce source alpha");
                    assertTextColor(0xFF4000, pixel);
                    if (y < 60) {
                        glyphs++;
                        if (alpha == 128) solidGlyphs++;
                    }
                }
            }
            assertTrue(glyphs > 30);
            assertTrue(solidGlyphs > 10, "Glyph interiors retain the requested alpha");
        }
    }

    @ParameterizedTest
    @EnumSource(Backend.class)
    void invalidTextCoordinatesAreIgnoredAndInvalidFontsRejected(Backend backend) {
        try (Target target = backend.create()) {
            ArberCanvas canvas = target.canvas;
            canvas.setFont(FONT);
            canvas.drawText(10, 20, null);
            canvas.drawText(10, 20, "");
            canvas.drawText(Float.NaN, 20, "Ignored");
            canvas.drawText(10, Float.POSITIVE_INFINITY, "Ignored");
            for (float size : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {
                assertThrows(IllegalArgumentException.class,
                        () -> canvas.setFont(new ArberFont("SansSerif", ArberFont.PLAIN, size)));
            }
            assertEquals(0, countPixels(target));
            assertTrue(canvas.getTextLineHeight() > 20, "Rejected font requests must retain the current font");
        }
    }

    @ParameterizedTest
    @EnumSource(Backend.class)
    void allEnabledOverlaysProduceRealRasterOutput(Backend backend) {
        property("Chart.performance.audit.enabled", "true");
        property("Chart.circular.performance.enabled", "true");
        property("Chart.playback.status.enabled", "true");
        property("Chart.watchdog.enabled", "false");
        property("Chart.scale.physical.enabled", "true");
        property("Chart.scale.physical.barMm", "10");
        property("Chart.scale.physical.marginMm", "4");
        DefaultChartModel model = new DefaultChartModel("Empty chart");
        DefaultPlotContext context = new DefaultPlotContext(new ArberRect(10, 10, 460, 220), model, 0, 1, 0, 1);
        LatencyTracker tracker = new LatencyTracker(32);
        tracker.record(3_000_000L);
        for (int overlay = 0; overlay < 4; overlay++) {
            try (Target target = backend.create()) {
                switch (overlay) {
                    case 0 -> new PerformanceAuditRenderer(tracker).render(target.canvas, context, null);
                    case 1 -> new CircularLatencyOverlayRenderer(tracker).renderEmptyState(target.canvas, model, context);
                    case 2 -> new PlaybackStatusRenderer(new ActivePlayback()).renderEmptyState(target.canvas, model, context);
                    case 3 -> new PhysicalScaleRenderer().renderEmptyState(target.canvas, model, context);
                    default -> throw new AssertionError();
                }
                assertTrue(countPixels(target) > 40, "Missing raster output from overlay " + overlay);
                assertNull(target.canvas.getClip(), "Overlay must restore the host clip");
                if (overlay == 2) {
                    assertTrue(countRunsOnPopulatedRow(target) >= 3,
                            "Playback must draw separate letter strokes, not the old solid rectangle");
                }
            }
        }
    }

    @Test
    void swingTextUsesTheHostTransformWhileMetricsRemainLogical() {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            AwtCanvasAdapter canvas = new AwtCanvasAdapter(graphics);
            canvas.setFont(FONT);
            canvas.setColor(new ArberColor(0xFF000000));
            graphics.translate(60, 20);
            graphics.scale(2, 2);
            // Pixel hinting can change advances after scaling on Linux and Windows.
            // The contract uses logical units from the current host font context.
            float logicalWidth = canvas.measureTextWidth("Host");
            assertEquals(graphics.getFontMetrics().stringWidth("Host"), logicalWidth);
            canvas.drawText(0, 30, "Host");
            int count = 0;
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    if (image.getRGB(x, y) == 0) continue;
                    count++;
                    assertTrue(x >= 60 && x < 60 + logicalWidth * 2 + 2);
                    assertTrue(y >= 20 && y < 85);
                }
            }
            assertTrue(count > 100);
        } finally {
            graphics.dispose();
        }
    }

    @Test
    void serverResetRestoresDefaultFontAndClosedCanvasRejectsTextCalls() {
        ImageBufferCanvas canvas = new ImageBufferCanvas(WIDTH, HEIGHT);
        float defaultHeight = canvas.getTextLineHeight();
        canvas.setFont(FONT);
        canvas.drawText(10, 40, "Reset");
        canvas.reset();
        assertEquals(defaultHeight, canvas.getTextLineHeight());
        for (int pixel : canvas.pixels()) assertEquals(0, pixel);
        canvas.close();
        assertThrows(IllegalStateException.class, () -> canvas.setFont(FONT));
        assertThrows(IllegalStateException.class, () -> canvas.drawText(10, 40, "Closed"));
        assertThrows(IllegalStateException.class, () -> canvas.measureTextWidth("Closed"));
        assertThrows(IllegalStateException.class, canvas::getTextLineHeight);
        assertThrows(IllegalStateException.class, canvas::getTextAscent);
    }

    private void property(String key, String value) {
        if (!previousProperties.containsKey(key)) previousProperties.put(key, ChartAssets.getString(key, null));
        ChartAssets.setProperty(key, value);
    }

    private static int countPixels(Target target) {
        int count = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (target.pixel(x, y) != 0) count++;
            }
        }
        return count;
    }

    private static void assertTextColor(int expectedRgb, int actualArgb) {
        int alpha = actualArgb >>> 24;
        assertTrue(alpha > 0);
        for (int shift = 0; shift <= 16; shift += 8) {
            double expected = ((expectedRgb >>> shift) & 0xFF) * alpha / 255.0;
            double actual = ((actualArgb >>> shift) & 0xFF) * alpha / 255.0;
            assertEquals(expected, actual, 1.0,
                    "Text color must survive alpha compositing within one premultiplied channel step");
        }
    }

    private static int countRunsOnPopulatedRow(Target target) {
        int maximum = 0;
        for (int y = 0; y < HEIGHT; y++) {
            int runs = 0;
            boolean previous = false;
            for (int x = 0; x < WIDTH; x++) {
                boolean present = target.pixel(x, y) != 0;
                if (present && !previous) runs++;
                previous = present;
            }
            maximum = Math.max(maximum, runs);
        }
        return maximum;
    }

    enum Backend {
        SWING {
            @Override Target create() {
                BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                return new Target(new AwtCanvasAdapter(graphics), image::getRGB, graphics::dispose);
            }
        },
        SERVER {
            @Override Target create() {
                ImageBufferCanvas canvas = new ImageBufferCanvas(WIDTH, HEIGHT);
                return new Target(canvas, (x, y) -> canvas.pixels()[y * WIDTH + x], canvas::close);
            }
        };

        abstract Target create();
    }

    private record Target(ArberCanvas canvas, IntBinaryOperator pixels, Runnable dispose) implements AutoCloseable {
        int pixel(int x, int y) { return pixels.applyAsInt(x, y); }
        @Override public void close() { dispose.run(); }
    }

    private static final class ActivePlayback implements PlaybackController {
        @Override public PlaybackController setDeterministic(boolean enabled) { return this; }
        @Override public boolean isDeterministic() { return true; }
        @Override public PlaybackController setSpeed(double speed) { return this; }
        @Override public double getSpeed() { return 1; }
        @Override public void reset(long timestamp) {}
        @Override public void stepTo(long timestamp) {}
        @Override public void advanceByNanos(long delta) {}
        @Override public boolean isPlaybackActive() { return true; }
        @Override public long getLastTickNanos() { return 1; }
        @Override public long getPlaybackElapsedNanos() { return 1; }
        @Override public long getResolvedNowNanos(long fallback) { return 1; }
        @Override public void appendStatus(StringBuilder builder) { builder.append("Playback 12.5s @ 1.00x"); }
    }
}
