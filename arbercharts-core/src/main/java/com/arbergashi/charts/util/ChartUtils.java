package com.arbergashi.charts.util;

import com.arbergashi.charts.api.PlotContext;

/**
 * Helper methods for coordinate transformations.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class ChartUtils {

    // Thread-local buffer for temporary transformations to avoid allocations
    // when mapToPixel cannot be used directly.
    private static final ThreadLocal<double[]> TEMP_BUFFER = ThreadLocal.withInitial(() -> new double[2]);

    private ChartUtils() {
    }

    /**
     * Transforms X coordinate to pixels.
     * Uses PlotContext internally.
     */
    public static double transformX(double x, PlotContext ctx) {
        double[] buf = TEMP_BUFFER.get();
        ctx.mapToPixel(x, 0, buf); // Y is irrelevant for X transformation
        return buf[0];
    }

    /**
     * Transforms Y coordinate to pixels.
     */
    public static double transformY(double y, PlotContext ctx) {
        double[] buf = TEMP_BUFFER.get();
        ctx.mapToPixel(0, y, buf); // X is irrelevant for Y transformation
        return buf[1];
    }

    /**
     * Map polar coordinates (angle in degrees, radius in data units) to pixels.
     * Writes result to supplied Point2D (allocation-free).
     */
    public static void mapToPolar(double angleDeg, double rValue, PlotContext ctx, java.awt.geom.Point2D dest) {
        java.awt.geom.Rectangle2D b = ctx.plotBounds();
        if (b == null) {
            dest.setLocation(0.0, 0.0);
            return;
        }
        double cx = b.getCenterX();
        double cy = b.getCenterY();
        double maxR = Math.min(b.getWidth(), b.getHeight()) / 2.0 - ChartScale.scale(20.0);
        if (!(maxR > 0)) {
            dest.setLocation(cx, cy);
            return;
        }
        double denom = ctx.maxY();
        if (!Double.isFinite(denom) || denom == 0.0) denom = 1.0;
        double radius = (rValue / denom) * maxR;
        double rad = Math.toRadians(angleDeg - 90.0);
        double px = Math.cos(rad) * radius + cx;
        double py = Math.sin(rad) * radius + cy;
        dest.setLocation(px, py);
    }

    /**
     * Heuristic to determine an appropriate bar width for categorical/bar-like renderers.
     * Returns at least 1.0 to avoid zero-width bars.
     */
    public static double calculateBestBarWidth(int count, double availableWidth, float paddingFactor) {
        if (count <= 0) return 1.0;
        double base = availableWidth / (double) count;
        float pad = Math.max(0.0f, Math.min(0.9f, paddingFactor));
        double width = base * (1.0 - pad);
        return Math.max(1.0, width);
    }
}