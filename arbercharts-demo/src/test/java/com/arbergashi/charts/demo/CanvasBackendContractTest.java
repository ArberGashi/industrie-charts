package com.arbergashi.charts.demo;

import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.bridge.server.ImageBufferCanvas;
import com.arbergashi.charts.bridge.swing.AwtCanvasAdapter;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.VoxelBuffer;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.circular.DonutRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Shared observable drawing semantics, without requiring identical raster edges. */
class CanvasBackendContractTest {
    private static final int SIZE = 64;
    private static final int RED = 0xFFFF0000;
    private static final int BLUE = 0xFF0000FF;
    private static final int GREEN = 0xFF00FF00;

    @ParameterizedTest(name = "{0}: lines and polylines")
    @EnumSource(Backend.class)
    void drawsLinesAndPolylines(Backend backend) {
        try (Target target = backend.create()) {
            ArberCanvas canvas = target.canvas();
            canvas.setColor(new ArberColor(RED));
            canvas.setStroke(1f);
            canvas.moveTo(5, 8);
            canvas.lineTo(25, 8);
            canvas.lineTo(25, 20);
            canvas.drawPolyline(new float[]{5, 20, 20, 50}, new float[]{30, 30, 45, 45}, 3);

            assertEquals(RED, target.pixel(12, 8));
            assertEquals(RED, target.pixel(25, 15));
            assertEquals(RED, target.pixel(12, 30));
            assertEquals(RED, target.pixel(20, 38));
            assertEquals(0, target.pixel(40, 45), "Points beyond count must not be drawn");
        }
    }

    @ParameterizedTest(name = "{0}: polyline tail beyond 65536 points")
    @EnumSource(Backend.class)
    void preservesPolylinePointsBeyondTheIntegerBufferCapacity(Backend backend) {
        int prefixCount = 65_536;
        int visibleCount = prefixCount + 1;
        float[] xs = new float[visibleCount + 1];
        float[] ys = new float[visibleCount + 1];
        Arrays.fill(xs, -100f);
        Arrays.fill(ys, -100f);
        xs[prefixCount] = 20f;
        ys[prefixCount] = 20f;
        xs[visibleCount] = 50f;
        ys[visibleCount] = 20f;

        try (Target target = backend.create()) {
            ArberCanvas canvas = target.canvas();
            canvas.setColor(new ArberColor(RED));
            canvas.setStroke(1f);
            canvas.drawPolyline(xs, ys, prefixCount);
            assertEquals(0, target.pixel(16, 16), "The first 65536 points are entirely offscreen");

            canvas.drawPolyline(xs, ys, visibleCount);
            assertEquals(RED, target.pixel(16, 16), "The segment beyond the integer buffer must remain visible");
            assertEquals(0, target.pixel(40, 20), "The array entry beyond count must remain excluded");

            canvas.drawPolyline(xs, ys, Integer.MAX_VALUE);
            assertEquals(RED, target.pixel(40, 20), "Count must be bounded by the arrays, not buffer capacity");
        }
    }

    @ParameterizedTest(name = "{0}: rectangle extents")
    @EnumSource(Backend.class)
    void drawsOutlinedAndFilledRectangles(Backend backend) {
        try (Target target = backend.create()) {
            ArberCanvas canvas = target.canvas();
            canvas.setColor(new ArberColor(RED));
            canvas.setStroke(1f);
            canvas.drawRect(4, 4, 20, 20);
            assertEquals(RED, target.pixel(12, 4));
            assertEquals(RED, target.pixel(4, 12));
            assertEquals(0, target.pixel(12, 12));

            canvas.fillRect(32, 32, 4, 3);
            int filled = 0;
            for (int y = 30; y < 40; y++) {
                for (int x = 30; x < 40; x++) {
                    if (target.pixel(x, y) != 0) filled++;
                }
            }
            assertEquals(12, filled, "An integer fillRect covers width times height pixels");
            assertEquals(RED, target.pixel(35, 34));
            assertEquals(0, target.pixel(36, 35));
        }
    }

    @ParameterizedTest(name = "{0}: empty shapes")
    @EnumSource(Backend.class)
    void ignoresEmptyFillsAndInsufficientPoints(Backend backend) {
        try (Target target = backend.create()) {
            ArberCanvas canvas = target.canvas();
            canvas.setColor(new ArberColor(RED));
            canvas.fillRect(10, 10, 0, 10);
            canvas.fillRect(10, 10, 10, 0);
            canvas.fillRect(10, 10, -5, 10);
            canvas.drawRect(10, 10, -5, 10);
            canvas.drawRect(10, 10, 10, -5);
            canvas.fillRect(-1.0e20f, -1.0e20f, -2.0e20f, 2.0e20f);
            canvas.drawRect(-1.0e20f, -1.0e20f, 2.0e20f, -2.0e20f);
            canvas.fillPolygon(new float[0], new float[0], 3);
            canvas.fillPolygon(new float[]{10, 20}, new float[]{10, 20}, 3);
            canvas.drawPolyline(new float[0], new float[0], 2);
            canvas.drawPolyline(new float[]{10}, new float[]{10}, 2);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    assertEquals(0, target.pixel(x, y));
                }
            }
        }
    }

    @ParameterizedTest(name = "{0}: polygons")
    @EnumSource(Backend.class)
    void fillsPolygonsWithoutFillingTheirBoundingBox(Backend backend) {
        try (Target target = backend.create()) {
            target.canvas().setColor(new ArberColor(RED));
            target.canvas().fillPolygon(new float[]{5, 45, 5}, new float[]{5, 5, 45}, 3);
            assertEquals(RED, target.pixel(10, 10));
            assertEquals(0, target.pixel(40, 40));
            assertEquals(0, target.pixel(2, 2));
        }
    }

    @ParameterizedTest(name = "{0}: clipping and restoration")
    @EnumSource(Backend.class)
    void replacesRestoresAndClearsClip(Backend backend) {
        try (Target target = backend.create()) {
            ArberCanvas canvas = target.canvas();
            assertNull(canvas.getClip());
            ArberRect outer = new ArberRect(8, 8, 32, 32);
            canvas.setClip(outer);
            ArberRect saved = canvas.getClip();
            assertEquals(outer, saved);
            canvas.setClip(new ArberRect(16, 16, 8, 8));
            canvas.setColor(new ArberColor(RED));
            canvas.fillRect(0, 0, SIZE, SIZE);
            assertEquals(RED, target.pixel(20, 20));
            assertEquals(0, target.pixel(10, 10));

            canvas.setClip(saved);
            canvas.setColor(new ArberColor(BLUE));
            canvas.fillRect(0, 0, SIZE, SIZE);
            assertEquals(saved, canvas.getClip());
            assertEquals(BLUE, target.pixel(10, 10));
            assertEquals(0, target.pixel(4, 4));
            assertEquals(0, target.pixel(40, 20));

            canvas.setClip(null);
            assertNull(canvas.getClip());
            canvas.setColor(new ArberColor(GREEN));
            canvas.fillRect(0, 0, 6, 6);
            assertEquals(GREEN, target.pixel(4, 4));
        }
    }

    @ParameterizedTest(name = "{0}: stroked geometry clipping")
    @EnumSource(Backend.class)
    void clipsLinesAndPolygons(Backend backend) {
        try (Target target = backend.create()) {
            ArberCanvas canvas = target.canvas();
            canvas.setColor(new ArberColor(RED));
            canvas.setStroke(5f);
            canvas.setClip(new ArberRect(16, 16, 16, 16));
            canvas.drawLine(0, 20, 60, 20);
            canvas.fillPolygon(new float[]{0, 60, 60, 0}, new float[]{24, 24, 60, 60}, 4);
            assertEquals(RED, target.pixel(20, 20));
            assertEquals(RED, target.pixel(20, 28));
            assertEquals(0, target.pixel(10, 20));
            assertEquals(0, target.pixel(40, 28));
            assertEquals(0, target.pixel(20, 40));
        }
    }

    @Test
    void swingReportsHostClipChangesInLogicalCoordinates() {
        BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.scale(2, 2);
            AwtCanvasAdapter canvas = new AwtCanvasAdapter(graphics);
            graphics.setClip(new Rectangle2D.Double(8, 12, 24, 20));
            ArberRect saved = canvas.getClip();
            assertEquals(new ArberRect(8, 12, 24, 20), saved);
            canvas.setClip(new ArberRect(10, 14, 4, 4));
            canvas.setClip(saved);
            canvas.setColor(new ArberColor(RED));
            canvas.fillRect(0, 0, 64, 64);
            assertEquals(RED, image.getRGB(20, 28));
            assertEquals(0, image.getRGB(12, 28));
            assertEquals(0, image.getRGB(64, 28));
            graphics.setClip(null);
            assertNull(canvas.getClip(), "A host-cleared clip must not remain cached");
        } finally {
            graphics.dispose();
        }
    }

    @Test
    void donutCenterTextPreservesTheCurrentSwingHostClipForFollowingDraws() {
        BufferedImage image = new BufferedImage(320, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            AwtCanvasAdapter canvas = new AwtCanvasAdapter(graphics);
            DefaultChartModel model = new DefaultChartModel("Donut");
            model.setPoint(0, 1, 0, 0, 1, "A");
            model.setPoint(1, 2, 0, 0, 2, "B");
            ArberRect plot = new ArberRect(20, 20, 280, 200);
            DefaultPlotContext context = new DefaultPlotContext(plot, model,
                    Double.NaN, Double.NaN, Double.NaN, Double.NaN, ChartThemes.getLightTheme(), null);
            // Like the host, establish the plot clip after creating the adapter.
            graphics.setClip(new Rectangle2D.Double(20, 20, 140, 200));
            new DonutRenderer().setCenterText("42").setCenterSubText("Total").render(canvas, model, context);
            assertEquals(new ArberRect(20, 20, 140, 200), canvas.getClip());
            assertEquals(0, image.getRGB(170, 120), "Center text must respect the inherited host clip");
            canvas.setColor(new ArberColor(GREEN));
            canvas.fillRect(0, 0, 320, 240);
            assertEquals(GREEN, image.getRGB(50, 50));
            assertEquals(0, image.getRGB(10, 50));
            assertEquals(0, image.getRGB(170, 120), "Later layers must retain the original clip");
        } finally {
            graphics.dispose();
        }
    }

    @ParameterizedTest(name = "{0}: stroke width")
    @EnumSource(Backend.class)
    void appliesStrokeWidthToLinesAndRectangles(Backend backend) {
        try (Target target = backend.create()) {
            ArberCanvas canvas = target.canvas();
            canvas.setColor(new ArberColor(RED));
            canvas.setStroke(1f);
            canvas.drawLine(8, 10, 50, 10);
            canvas.setStroke(7f);
            canvas.drawLine(8, 30, 50, 30);
            assertTrue(coloredPixels(target, 24, 25, 36) > coloredPixels(target, 24, 5, 16));
            assertEquals(RED, target.pixel(24, 32));
            assertEquals(0, target.pixel(24, 36));

            canvas.drawRect(8, 45, 40, 10);
            assertEquals(RED, target.pixel(24, 47));
            assertEquals(0, target.pixel(24, 50));
        }
    }

    @ParameterizedTest(name = "{0}: source-over on opaque destination")
    @EnumSource(Backend.class)
    void compositesOverOpaquePixels(Backend backend) {
        try (Target target = backend.create()) {
            ArberCanvas canvas = target.canvas();
            canvas.setColor(new ArberColor(BLUE));
            canvas.fillRect(4, 4, 32, 32);
            canvas.setColor(new ArberColor(0x80FF0000));
            canvas.fillRect(20, 20, 32, 32);
            assertArgbNear(0xFF80007F, target.pixel(28, 28));
            assertEquals(BLUE, target.pixel(10, 10));
            assertArgbNear(0x80FF0000, target.pixel(45, 45));
            assertEquals(0, target.pixel(60, 60));
        }
    }

    @ParameterizedTest(name = "{0}: source-over on translucent destination")
    @EnumSource(Backend.class)
    void compositesOverTranslucentPixels(Backend backend) {
        try (Target target = backend.create()) {
            ArberCanvas canvas = target.canvas();
            canvas.setColor(new ArberColor(0x800000FF));
            canvas.fillRect(4, 4, 32, 32);
            canvas.setColor(new ArberColor(0x80FF0000));
            canvas.fillPolygon(new float[]{16, 50, 16}, new float[]{16, 16, 50}, 3);
            assertArgbNear(0xC0AA0055, target.pixel(24, 24));
            assertArgbNear(0x800000FF, target.pixel(10, 10));
        }
    }

    @ParameterizedTest(name = "{0}: transparent source")
    @EnumSource(Backend.class)
    void transparentSourcePreservesDestination(Backend backend) {
        try (Target target = backend.create()) {
            ArberCanvas canvas = target.canvas();
            canvas.setColor(new ArberColor(BLUE));
            canvas.fillRect(4, 4, 32, 32);
            canvas.setColor(new ArberColor(0x00FF0000));
            canvas.fillRect(0, 0, SIZE, SIZE);
            assertEquals(BLUE, target.pixel(20, 20));
            assertEquals(0, target.pixel(50, 50));
        }
    }

    @ParameterizedTest(name = "{0}: offscreen geometry")
    @EnumSource(Backend.class)
    void clipsExtremeOffscreenGeometryWithoutWalkingItsEntireExtent(Backend backend) {
        try (Target target = backend.create()) {
            assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                ArberCanvas canvas = target.canvas();
                canvas.setColor(new ArberColor(RED));
                canvas.setStroke(1f);
                canvas.drawLine(-1.0e20f, 20, 1.0e20f, 20);
                canvas.drawLine(16, -1.0e20f, 16, 1.0e20f);
                canvas.drawPolyline(new float[]{-1.0e20f, 1.0e20f}, new float[]{30, 30}, 2);
                canvas.drawRect(1.0e9f, 1.0e9f, 100, 100);
                canvas.fillRect(1.0e9f, 1.0e9f, 100, 100);
                canvas.fillPolygon(new float[]{-1.0e9f, 1.0e9f, 0},
                        new float[]{-1.0e9f, -1.0e9f, -10}, 3);
            });
            assertEquals(RED, target.pixel(32, 20));
            assertEquals(RED, target.pixel(16, 32));
            assertEquals(RED, target.pixel(32, 30));
            assertEquals(0, target.pixel(32, 40));
        }
    }

    @ParameterizedTest(name = "{0}: large crossing rectangles")
    @EnumSource(Backend.class)
    void drawsTheVisiblePartOfLargeRectangles(Backend backend) {
        try (Target target = backend.create()) {
            assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                ArberCanvas canvas = target.canvas();
                canvas.setColor(new ArberColor(BLUE));
                canvas.fillRect(-1.0e20f, 10, 2.0e20f, 10);
                canvas.setColor(new ArberColor(RED));
                canvas.setStroke(1f);
                canvas.drawRect(-1.0e20f, 32, 2.0e20f, 16);
            });
            assertEquals(BLUE, target.pixel(32, 15));
            assertEquals(RED, target.pixel(32, 32));
            assertEquals(RED, target.pixel(32, 48));
            assertEquals(0, target.pixel(32, 40), "Large outlines must remain unfilled");
        }
    }

    private static int coloredPixels(Target target, int x, int fromY, int toY) {
        int count = 0;
        for (int y = fromY; y < toY; y++) {
            if (target.pixel(x, y) != 0) count++;
        }
        return count;
    }

    @ParameterizedTest(name = "{0}: voxel colors and canvas state")
    @EnumSource(Backend.class)
    void drawsVoxelColorsAndRestoresTheCanvasColor(Backend backend) {
        try (Target target = backend.create()) {
            ArberCanvas canvas = target.canvas();
            canvas.setColor(new ArberColor(BLUE));
            canvas.setStroke(3f);
            canvas.fillRect(10, 10, 8, 8);
            canvas.drawVoxelField(new VoxelBuffer(new float[]{4, 12, 20}, new float[]{4, 12, 20},
                    null, new int[]{RED, 0x80FF0000}, 3));
            assertEquals(RED, target.pixel(5, 5));
            assertArgbNear(0xFF80007F, target.pixel(13, 13));
            assertEquals(BLUE, target.pixel(21, 21), "Missing voxel colors use the canvas color");
            canvas.fillRect(30, 30, 4, 4);
            assertEquals(BLUE, target.pixel(31, 31), "Voxel colors must not leak into later draws");
        }
    }

    @ParameterizedTest(name = "{0}: bounded voxel input and clipping")
    @EnumSource(Backend.class)
    void boundsVoxelInputAndClipsVoxelSquares(Backend backend) {
        try (Target target = backend.create()) {
            ArberCanvas canvas = target.canvas();
            canvas.setColor(new ArberColor(GREEN));
            canvas.setStroke(5f);
            canvas.setClip(new ArberRect(0, 0, 20, 20));
            canvas.drawVoxelField(null);
            canvas.drawVoxelField(new VoxelBuffer(null, new float[]{1}, null, null, 1));
            canvas.drawVoxelField(new VoxelBuffer(new float[]{1}, null, null, null, 1));
            canvas.drawVoxelField(new VoxelBuffer(new float[]{8, 18, 28}, new float[]{8, 18},
                    null, null, 100));
            assertEquals(GREEN, target.pixel(9, 9));
            assertEquals(GREEN, target.pixel(19, 19));
            assertEquals(0, target.pixel(21, 19), "Voxel square must obey the clip");
            canvas.setClip(null);
            canvas.drawVoxelField(new VoxelBuffer(new float[]{40, 50}, new float[]{40, 50},
                    null, null, 1));
            assertEquals(GREEN, target.pixel(41, 41));
            assertEquals(0, target.pixel(51, 51), "Points beyond count must not be drawn");
        }
    }

    private static void assertArgbNear(int expected, int actual) {
        for (int shift = 0; shift <= 24; shift += 8) {
            int expectedChannel = (expected >>> shift) & 0xFF;
            int actualChannel = (actual >>> shift) & 0xFF;
            assertTrue(Math.abs(expectedChannel - actualChannel) <= 1,
                    () -> "Expected ARGB " + Integer.toHexString(expected)
                            + " but got " + Integer.toHexString(actual));
        }
    }

    enum Backend {
        SWING {
            @Override Target create() {
                BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                return new Target(new AwtCanvasAdapter(graphics), image::getRGB, graphics::dispose);
            }
        },
        SERVER {
            @Override Target create() {
                ImageBufferCanvas canvas = new ImageBufferCanvas(SIZE, SIZE);
                return new Target(canvas, (x, y) -> canvas.pixels()[y * SIZE + x], canvas::close);
            }
        };

        abstract Target create();
    }

    private record Target(ArberCanvas canvas, IntBinaryOperator readPixel, Runnable dispose) implements AutoCloseable {
        int pixel(int x, int y) { return readPixel.applyAsInt(x, y); }
        @Override public void close() { dispose.run(); }
    }
}
