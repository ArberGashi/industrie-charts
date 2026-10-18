package com.arbergashi.charts.bridge.swing;

import com.arbergashi.charts.api.types.ArberColor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwtCanvasAdapterTest {
    private static final int LINE_COLOR = 0xB8FF0000;

    static {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void lineToDrawsLineFromLastMove() {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        AwtCanvasAdapter canvas = new AwtCanvasAdapter(g2);
        canvas.setColor(new ArberColor(0xFFFF0000));
        canvas.setStroke(1f);
        canvas.moveTo(1f, 1f);
        canvas.lineTo(8f, 1f);

        g2.dispose();

        int red = 0xFFFF0000;
        int hitCount = 0;
        for (int x = 1; x <= 8; x++) {
            if (image.getRGB(x, 1) == red) {
                hitCount++;
            }
        }
        assertTrue(hitCount >= 4, "Expected multiple red pixels on the line");
        assertEquals(red, image.getRGB(1, 1));
        assertEquals(red, image.getRGB(8, 1));
    }

    @ParameterizedTest(name = "{0}, {1}x, width {2}")
    @MethodSource("lineGeometry")
    void antialiasedTwoPointLinesMatchIndependentSquareCapGeometry(
            Segment segment, int scale, float width) {
        BufferedImage actual = new BufferedImage(64 * scale, 64 * scale, BufferedImage.TYPE_INT_ARGB);
        BufferedImage expected = new BufferedImage(64 * scale, 64 * scale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = graphics(actual, scale, true);
        Graphics2D reference = graphics(expected, scale, true);
        try {
            AwtCanvasAdapter canvas = new AwtCanvasAdapter(graphics);
            canvas.setColor(new ArberColor(LINE_COLOR));
            canvas.setStroke(width);
            canvas.drawPolyline(new float[]{segment.x1, segment.x2},
                    new float[]{segment.y1, segment.y2}, 2);

            // Construct the square-ended stroke as a filled quadrilateral, without
            // invoking any line, polyline or stroke operation in the reference.
            reference.setColor(new Color(LINE_COLOR, true));
            reference.fill(squareCapOutline(segment, width));
        } finally {
            graphics.dispose();
            reference.dispose();
        }

        int totalAlpha = 0;
        int partialPixels = 0;
        int sourceAlpha = LINE_COLOR >>> 24;
        // Marlin's mask path and Java2D's parallelogram path quantize edge
        // coverage differently. Permit at most 1/16 of the source alpha per
        // pixel; missing, shifted, widened or non-antialiased lines exceed this.
        int edgeTolerance = (sourceAlpha + 15) / 16;
        for (int y = 0; y < actual.getHeight(); y++) {
            for (int x = 0; x < actual.getWidth(); x++) {
                int pixel = actual.getRGB(x, y);
                int alpha = pixel >>> 24;
                int referenceAlpha = expected.getRGB(x, y) >>> 24;
                assertTrue(Math.abs(alpha - referenceAlpha) <= edgeTolerance,
                        "Stroke coverage differs at " + x + "," + y
                                + ": expected " + referenceAlpha + ", actual " + alpha);
                assertTrue(alpha <= sourceAlpha, "A single translucent segment must not overlap itself");
                if (alpha > 0) assertEquals(0xFF0000, pixel & 0xFFFFFF, "Stroke color must be preserved");
                if (alpha > 0 && alpha < sourceAlpha) partialPixels++;
                totalAlpha += alpha;
            }
        }
        assertTrue(totalAlpha > 0, "The segment must be visible");
        if (width < 1f) assertTrue(partialPixels > 0, "Thin antialiased lines need fractional edge coverage");
    }

    @ParameterizedTest(name = "{0}, {1}x, width {2}")
    @MethodSource("lineGeometry")
    void nonAntialiasedTwoPointLinesPreserveCenterlineAndFiniteStrokeExtent(
            Segment segment, int scale, float width) {
        BufferedImage image = new BufferedImage(64 * scale, 64 * scale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = graphics(image, scale, false);
        try {
            AwtCanvasAdapter canvas = new AwtCanvasAdapter(graphics);
            canvas.setColor(new ArberColor(LINE_COLOR));
            canvas.setStroke(width);
            canvas.drawPolyline(new float[]{segment.x1, segment.x2},
                    new float[]{segment.y1, segment.y2}, 2);
        } finally {
            graphics.dispose();
        }
        int steps = Math.max(1, (int) Math.ceil(Math.hypot(segment.x2 - segment.x1,
                segment.y2 - segment.y1) * scale));
        for (int step = 0; step <= steps; step++) {
            double t = (double) step / steps;
            int x = (int) Math.round((segment.x1 + t * (segment.x2 - segment.x1)) * scale);
            int y = (int) Math.round((segment.y1 + t * (segment.y2 - segment.y1)) * scale);
            if (x < 8 * scale || x >= 56 * scale || y < 8 * scale || y >= 56 * scale) continue;
            // With STROKE_PURE, AA-off can assign a thin integer-coordinate
            // centerline to either adjacent pixel row. Require visible coverage
            // along the complete line, without assuming an AA sampling convention.
            boolean covered = false;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    covered |= image.getRGB(x + dx, y + dy) == LINE_COLOR;
                }
            }
            assertTrue(covered, "Centerline must remain covered near " + x + "," + y);
        }
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                assertTrue(pixel == 0 || pixel == LINE_COLOR, "AA-off rendering must not introduce partial coverage");
                if (pixel != 0) {
                    double distance = java.awt.geom.Line2D.ptSegDist(
                            segment.x1 * scale, segment.y1 * scale,
                            segment.x2 * scale, segment.y2 * scale, x, y);
                    double capRadius = Math.sqrt(2) * width * scale * 0.5;
                    assertTrue(distance <= capRadius + 1.5, "Pixels must remain within the stroked segment");
                    assertTrue(x >= 8 * scale && x < 56 * scale && y >= 8 * scale && y < 56 * scale,
                            "Stroke must retain the host clip");
                }
            }
        }
    }

    @Test
    void countControlsWhetherTheSecondSegmentIsDrawn() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = graphics(image, 1, false);
        try {
            AwtCanvasAdapter canvas = new AwtCanvasAdapter(graphics);
            canvas.setColor(new ArberColor(LINE_COLOR));
            canvas.setStroke(3f);
            float[] xs = {12, 48, 48};
            float[] ys = {24, 24, 48};
            canvas.drawPolyline(xs, ys, 2);
            assertEquals(LINE_COLOR, image.getRGB(30, 24));
            assertEquals(0, image.getRGB(48, 40), "Points after count must remain excluded");
            canvas.drawPolyline(xs, ys, 3);
            assertEquals(LINE_COLOR, image.getRGB(48, 40), "A multi-point path must retain its later segments");
        } finally {
            graphics.dispose();
        }
    }

    @Test
    void crossingExtremeTwoPointLinesRetainTheClippedFallbackAtBothScales() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            for (boolean antialias : new boolean[]{false, true}) {
                for (int scale : new int[]{1, 2}) {
                    // The first extent fits int coordinates but its difference
                    // overflows int. The second exceeds the coordinate range.
                    for (float extent : new float[]{1.5e9f, 3.0e9f}) {
                        BufferedImage image = new BufferedImage(64 * scale, 64 * scale, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D graphics = graphics(image, scale, antialias);
                        try {
                            AwtCanvasAdapter canvas = new AwtCanvasAdapter(graphics);
                            canvas.setColor(new ArberColor(LINE_COLOR));
                            canvas.setStroke(3f);
                            canvas.drawPolyline(new float[]{-extent, extent}, new float[]{24, 24}, 2);
                            canvas.drawPolyline(new float[]{40, 40}, new float[]{-extent, extent}, 2);
                            assertEquals(LINE_COLOR, image.getRGB(24 * scale, 24 * scale));
                            assertEquals(LINE_COLOR, image.getRGB(40 * scale, 40 * scale));
                            assertEquals(0, image.getRGB(4 * scale, 24 * scale));
                            assertEquals(0, image.getRGB(40 * scale, 60 * scale));
                            assertEquals(0, image.getRGB(24 * scale, 40 * scale));
                        } finally {
                            graphics.dispose();
                        }
                    }
                }
            }
        });
    }

    private static Stream<Arguments> lineGeometry() {
        Segment[] segments = {
                new Segment("horizontal", 12, 24, 48, 24),
                new Segment("vertical", 24, 12, 24, 48),
                new Segment("diagonal", 12, 12, 48, 48),
                new Segment("reverse diagonal", 48, 12, 12, 48),
                new Segment("shallow diagonal", 12, 18, 48, 42),
                new Segment("coincident endpoints", 32, 32, 32, 32),
                new Segment("crossing clip", 2, 24, 62, 24)
        };
        Stream.Builder<Arguments> cases = Stream.builder();
        for (Segment segment : segments) {
            for (int scale : new int[]{1, 2}) {
                for (float width : new float[]{0.55f, 0.85f, 1f, 1.2f, 2f, 3.25f, 7f}) {
                    cases.add(Arguments.of(segment, scale, width));
                }
            }
        }
        return cases.build();
    }

    private static Graphics2D graphics(BufferedImage image, int scale, boolean antialias) {
        Graphics2D graphics = image.createGraphics();
        graphics.scale(scale, scale);
        graphics.setClip(8, 8, 48, 48);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                antialias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return graphics;
    }

    private static Path2D squareCapOutline(Segment segment, float width) {
        double dx = segment.x2 - segment.x1;
        double dy = segment.y2 - segment.y1;
        double length = Math.hypot(dx, dy);
        double ux = length == 0 ? 1 : dx / length;
        double uy = length == 0 ? 0 : dy / length;
        double half = width * 0.5;
        double startX = segment.x1 - half * ux;
        double startY = segment.y1 - half * uy;
        double endX = segment.x2 + half * ux;
        double endY = segment.y2 + half * uy;
        Path2D outline = new Path2D.Double();
        outline.moveTo(startX - half * uy, startY + half * ux);
        outline.lineTo(endX - half * uy, endY + half * ux);
        outline.lineTo(endX + half * uy, endY - half * ux);
        outline.lineTo(startX + half * uy, startY - half * ux);
        outline.closePath();
        return outline;
    }

    private record Segment(String name, float x1, float y1, float x2, float y2) {
        @Override
        public String toString() {
            return name;
        }
    }
}
