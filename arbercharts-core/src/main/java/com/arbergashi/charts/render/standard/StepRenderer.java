package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Professional, zero-allocation, high-precision step chart renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * @see BaseRenderer
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class StepRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];
    private final double[] p1 = new double[2];

    public StepRenderer() {
        super("step");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();

        canvas.setStroke(getSeriesStrokeWidth());
        canvas.setColor(getSeriesColor(model));

        int outCount = 1 + (n - 1) * 2;
        float[] xs = RendererAllocationCache.getFloatArray(this, "step.x", outCount);
        float[] ys = RendererAllocationCache.getFloatArray(this, "step.y", outCount);

        context.mapToPixel(xData[0], yData[0], p0);
        xs[0] = (float) p0[0];
        ys[0] = (float) p0[1];

        int out = 1;
        for (int i = 1; i < n; i++) {
            context.mapToPixel(xData[i - 1], yData[i - 1], p0);
            context.mapToPixel(xData[i], yData[i], p1);
            xs[out] = (float) p1[0];
            ys[out] = (float) p0[1];
            out++;
            xs[out] = (float) p1[0];
            ys[out] = (float) p1[1];
            out++;
        }
        canvas.drawPolyline(xs, ys, out);
    }
}
