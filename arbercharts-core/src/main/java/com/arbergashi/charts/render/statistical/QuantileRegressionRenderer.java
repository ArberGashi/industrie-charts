package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;
/**
 * Quantile regression renderer.
 * Draws multiple percentiles (e.g., 10%, 50%, 90%) as lines.
 * Expects multivariate data in min/max/weight slots for the quantiles.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class QuantileRegressionRenderer extends BaseRenderer {

    public QuantileRegressionRenderer() {
        super("quantileRegression");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count <= 0) return;

        ArberColor baseColor = getSeriesColor(model);
        ArberColor medianColor = isMultiColor() ? themeSeries(context, 0) : baseColor;
        ArberColor highColor = isMultiColor() ? themeSeries(context, 1) : baseColor;
        ArberColor lowColor = isMultiColor() ? themeSeries(context, 2) : baseColor;
        if (medianColor == null) medianColor = baseColor;
        if (highColor == null) highColor = baseColor;
        if (lowColor == null) lowColor = baseColor;

        // 50% Quantil (Median) - component 1
        drawQuantilePath(canvas, model, context, medianColor, 2.5f, 1.0f, 1);

        // 90% Quantil - max -> component 4
        drawQuantilePath(canvas, model, context, highColor, 1.5f, 0.6f, 4);

        // 10% Quantil - min -> component 3
        drawQuantilePath(canvas, model, context, lowColor, 1.5f, 0.6f, 3);
    }

    private void drawQuantilePath(ArberCanvas canvas, ChartModel model, PlotContext context,
                                  ArberColor color, float width, float alpha, int component) {
        int count = model.getPointCount();
        if (count <= 0) return;
        float[] xs = RendererAllocationCache.getFloatArray(this, "qr.xs", count);
        float[] ys = RendererAllocationCache.getFloatArray(this, "qr.ys", count);
        int used = 0;
        double[] buf = pBuffer();
        for (int i = 0; i < count; i++) {
            double x = model.getX(i);
            double y = model.getY(i);
            // If component is not x/y, use getValue fallback
            double val = (component == 0) ? x : (component == 1) ? y : model.getValue(i, component);
            context.mapToPixel(x, val, buf);
            xs[used] = (float) buf[0];
            ys[used] = (float) buf[1];
            used++;
        }

        canvas.setColor(ColorUtils.applyAlpha(color, alpha));
        canvas.setStroke(ChartScale.scale(width));
        canvas.drawPolyline(xs, ys, used);
    }
}
