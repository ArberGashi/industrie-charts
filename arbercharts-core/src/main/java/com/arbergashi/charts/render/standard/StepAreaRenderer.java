package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;
import com.arbergashi.charts.util.MathUtils;

/**
 * Step-area renderer: like an area chart, but with step transitions (horizontal then vertical).
 * Very fast and well-suited for discrete measurements.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-15
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class StepAreaRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];
    private final double[] p1 = new double[2];

    public StepAreaRenderer() {
        super("stepArea");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n0 = model.getPointCount();
        if (n0 < 2) return;

        final double[] xData = model.getXData();
        final double[] yData = model.getYData();
        final int n = Math.min(n0, Math.min(xData.length, yData.length));
        if (n < 2) return;

        ArberColor c = getSeriesColor(model);

        context.mapToPixel(0, 0.0, p0);
        double zeroY = p0[1];
        ArberRect bounds = context.getPlotBounds();
        double baselineY = MathUtils.clamp(zeroY, bounds.minY(), bounds.maxY());

        int first = -1;
        for (int i = 0; i < n; i++) {
            if (Double.isFinite(xData[i]) && Double.isFinite(yData[i])) {
                first = i;
                break;
            }
        }
        if (first < 0 || first + 1 >= n) return;

        int stepCount = 1 + (n - first - 1) * 2;
        float[] xs = RendererAllocationCache.getFloatArray(this, "stepArea.x", stepCount);
        float[] ys = RendererAllocationCache.getFloatArray(this, "stepArea.y", stepCount);

        context.mapToPixel(xData[first], yData[first], p0);
        if (!Double.isFinite(p0[0]) || !Double.isFinite(p0[1])) return;
        xs[0] = (float) p0[0];
        ys[0] = (float) p0[1];

        double lastX = xData[first];
        double lastY = yData[first];
        int lastFinite = first;
        int out = 1;

        for (int i = first + 1; i < n; i++) {
            double x = xData[i];
            double y = yData[i];
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;

            context.mapToPixel(lastX, lastY, p0);
            context.mapToPixel(x, y, p1);
            if (!Double.isFinite(p0[0]) || !Double.isFinite(p0[1]) || !Double.isFinite(p1[0]) || !Double.isFinite(p1[1])) {
                lastX = x;
                lastY = y;
                continue;
            }

            xs[out] = (float) p1[0];
            ys[out] = (float) p0[1];
            out++;
            xs[out] = (float) p1[0];
            ys[out] = (float) p1[1];
            out++;

            lastX = x;
            lastY = y;
            lastFinite = i;
        }

        if (lastFinite == first || out < 2) return;

        // Build polygon with baseline
        float[] polyX = RendererAllocationCache.getFloatArray(this, "stepArea.poly.x", out + 2);
        float[] polyY = RendererAllocationCache.getFloatArray(this, "stepArea.poly.y", out + 2);
        int p = 0;
        polyX[p] = xs[0];
        polyY[p] = (float) baselineY;
        p++;
        for (int i = 0; i < out; i++) {
            polyX[p] = xs[i];
            polyY[p] = ys[i];
            p++;
        }
        polyX[p] = xs[out - 1];
        polyY[p] = (float) baselineY;
        p++;

        canvas.setColor(ColorUtils.applyAlpha(c, 0.22f));
        canvas.fillPolygon(polyX, polyY, p);

        canvas.setStroke(ChartScale.scale(2.0f));
        canvas.setColor(c);
        canvas.drawPolyline(xs, ys, out);
    }
}
