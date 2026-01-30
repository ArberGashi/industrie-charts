package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.util.ChartAssets;
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
 * Area renderer relative to a configurable baseline.
 * Key: {@code chart.render.baseline.value} (default 0.0).
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-15
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class BaselineAreaRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    public BaselineAreaRenderer() {
        super("baselineArea");
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

        double baselineValue = 0.0;
        try {
            baselineValue = Double.parseDouble(ChartAssets.getString("chart.render.baseline.value", "0.0"));
        } catch (Exception ignored) {
        }
        context.mapToPixel(0, baselineValue, p0);
        ArberRect bounds = context.getPlotBounds();
        double baselineY = MathUtils.clamp(p0[1], bounds.minY(), bounds.maxY());

        ArberColor c = getSeriesColor(model);

        float[] xs = RendererAllocationCache.getFloatArray(this, "baseline.x", n);
        float[] ys = RendererAllocationCache.getFloatArray(this, "baseline.y", n);
        int out = 0;
        for (int i = 0; i < n; i++) {
            context.mapToPixel(xData[i], yData[i], p0);
            xs[out] = (float) p0[0];
            ys[out] = (float) p0[1];
            out++;
        }
        if (out < 2) return;

        float[] polyX = RendererAllocationCache.getFloatArray(this, "baseline.poly.x", out + 2);
        float[] polyY = RendererAllocationCache.getFloatArray(this, "baseline.poly.y", out + 2);
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

        canvas.setColor(ColorUtils.applyAlpha(c, 0.25f));
        canvas.fillPolygon(polyX, polyY, p);

        canvas.setStroke(getSeriesStrokeWidth());
        canvas.setColor(c);
        canvas.drawPolyline(xs, ys, out);
    }
}
