package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Shape-preserving piecewise cubic Hermite interpolation (PCHIP).
 *
 * <p>The curve passes through the supplied points without overshooting monotone
 * intervals. Nonfinite points, repeated X values and changes in X direction
 * start a new path. Both increasing and decreasing X runs are supported.
 * Curves are flattened to reusable canvas polylines at the current display scale.</p>
 *
 * @author Arber Gashi
 * @since 2025-06-01
 * @see BaseRenderer
 */
public final class SplineRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    public SplineRenderer() {
        super("spline");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < 2) return;
        double[] xData = RendererAllocationCache.getDoubleArray(this, "spline.input.x", n);
        double[] yData = RendererAllocationCache.getDoubleArray(this, "spline.input.y", n);
        for (int i = 0; i < n; i++) {
            xData[i] = model.getX(i);
            yData[i] = model.getY(i);
        }

        canvas.setStroke(getSeriesStrokeWidth());
        canvas.setColor(getSeriesColor(model));

        float[] xs = RendererAllocationCache.getFloatArray(this, "spline.x", 65);
        float[] ys = RendererAllocationCache.getFloatArray(this, "spline.y", 65);
        int start = 0;
        while (start < n) {
            if (!finitePoint(xData, yData, start)) {
                start++;
                continue;
            }
            int end = start;
            double direction = 0.0;
            while (end + 1 < n && finitePoint(xData, yData, end + 1)) {
                double dx = xData[end + 1] - xData[end];
                if (dx == 0.0 || !Double.isFinite(dx)) break;
                if (direction == 0.0) direction = Math.signum(dx);
                if (Math.signum(dx) != direction) break;
                end++;
            }
            for (int i = start; i < end; i++) {
                double dx = xData[i + 1] - xData[i];
                double m0 = tangent(xData, yData, i, start, end);
                double m1 = tangent(xData, yData, i + 1, start, end);
                context.mapToPixel(xData[i], yData[i], p0);
                double pixelX = p0[0];
                context.mapToPixel(xData[i + 1], yData[i + 1], p0);
                int steps = (int) Math.clamp(Math.ceil(Math.abs(p0[0] - pixelX) / 4.0), 8, 64);
                int out = 0;
                for (int j = 0; j <= steps; j++) {
                    double t = j / (double) steps;
                    double t2 = t * t;
                    double t3 = t2 * t;
                    double y = (2 * t3 - 3 * t2 + 1) * yData[i]
                            + (t3 - 2 * t2 + t) * dx * m0
                            + (-2 * t3 + 3 * t2) * yData[i + 1]
                            + (t3 - t2) * dx * m1;
                    if (!Double.isFinite(y)) y = (1.0 - t) * yData[i] + t * yData[i + 1];
                    context.mapToPixel(xData[i] + t * dx, y, p0);
                    if (!Float.isFinite((float) p0[0]) || !Float.isFinite((float) p0[1])) {
                        if (out > 1) canvas.drawPolyline(xs, ys, out);
                        out = 0;
                        continue;
                    }
                    xs[out] = (float) p0[0];
                    ys[out++] = (float) p0[1];
                }
                if (out > 1) canvas.drawPolyline(xs, ys, out);
            }
            start = end + 1;
        }
    }

    private static boolean finitePoint(double[] x, double[] y, int i) {
        return Double.isFinite(x[i]) && Double.isFinite(y[i]);
    }

    private static double tangent(double[] x, double[] y, int i, int start, int end) {
        if (end - start == 1) return slope(x, y, start);
        if (i == start) {
            return endpoint(Math.abs(x[i + 1] - x[i]), Math.abs(x[i + 2] - x[i + 1]),
                    slope(x, y, i), slope(x, y, i + 1));
        }
        if (i == end) {
            return endpoint(Math.abs(x[i] - x[i - 1]), Math.abs(x[i - 1] - x[i - 2]),
                    slope(x, y, i - 1), slope(x, y, i - 2));
        }
        double previous = slope(x, y, i - 1);
        double next = slope(x, y, i);
        if (previous == 0.0 || next == 0.0 || Math.signum(previous) != Math.signum(next)) return 0.0;
        double h0 = Math.abs(x[i] - x[i - 1]);
        double h1 = Math.abs(x[i + 1] - x[i]);
        double w0 = 2 * h1 + h0;
        double w1 = h1 + 2 * h0;
        return (w0 + w1) / (w0 / previous + w1 / next);
    }

    private static double slope(double[] x, double[] y, int i) {
        return (y[i + 1] - y[i]) / (x[i + 1] - x[i]);
    }

    private static double endpoint(double h0, double h1, double d0, double d1) {
        double value = ((2 * h0 + h1) * d0 - h0 * d1) / (h0 + h1);
        if (Math.signum(value) != Math.signum(d0)) return 0.0;
        if (Math.signum(d0) != Math.signum(d1) && Math.abs(value) > 3 * Math.abs(d0)) return 3 * d0;
        return value;
    }
}
