package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ColorUtils;
import com.arbergashi.charts.util.MathUtils;

/**
 * StackedAreaRenderer.
 *
 * <p>Lightweight stacked area implementation.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class StackedAreaRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    public StackedAreaRenderer() {
        super("stackedArea");
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

        context.mapToPixel(0, 0.0, p0);
        ArberRect bounds = context.getPlotBounds();
        double yBase = MathUtils.clamp(p0[1], bounds.minY(), bounds.maxY());

        float[] xs = RendererAllocationCache.getFloatArray(this, "stackedArea.x", n);
        float[] ys = RendererAllocationCache.getFloatArray(this, "stackedArea.y", n);
        int out = 0;
        for (int i = 0; i < n; i++) {
            context.mapToPixel(xData[i], yData[i], p0);
            xs[out] = (float) p0[0];
            ys[out] = (float) p0[1];
            out++;
        }
        if (out < 2) return;

        float[] polyX = RendererAllocationCache.getFloatArray(this, "stackedArea.poly.x", out + 2);
        float[] polyY = RendererAllocationCache.getFloatArray(this, "stackedArea.poly.y", out + 2);
        int p = 0;
        polyX[p] = xs[0];
        polyY[p] = (float) yBase;
        p++;
        for (int i = 0; i < out; i++) {
            polyX[p] = xs[i];
            polyY[p] = ys[i];
            p++;
        }
        polyX[p] = xs[out - 1];
        polyY[p] = (float) yBase;
        p++;

        ArberColor base = getSeriesColor(model);
        canvas.setColor(ColorUtils.applyAlpha(base, 0.25f));
        canvas.fillPolygon(polyX, polyY, p);

        canvas.setColor(base);
        canvas.setStroke(getSeriesStrokeWidth());
        canvas.drawPolyline(xs, ys, out);
    }
}
