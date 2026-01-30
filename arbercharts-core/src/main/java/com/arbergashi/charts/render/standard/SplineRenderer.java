package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Professional, zero-allocation, high-precision spline chart renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * @see BaseRenderer
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
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

        double[] xData = model.getXData();
        double[] yData = model.getYData();

        canvas.setStroke(getSeriesStrokeWidth());
        canvas.setColor(getSeriesColor(model));

        float[] xs = RendererAllocationCache.getFloatArray(this, "spline.x", n);
        float[] ys = RendererAllocationCache.getFloatArray(this, "spline.y", n);
        int out = 0;
        for (int i = 0; i < n; i++) {
            context.mapToPixel(xData[i], yData[i], p0);
            if (!Double.isFinite(p0[0]) || !Double.isFinite(p0[1])) continue;
            xs[out] = (float) p0[0];
            ys[out] = (float) p0[1];
            out++;
        }
        if (out > 1) {
            canvas.drawPolyline(xs, ys, out);
        }
    }
}
