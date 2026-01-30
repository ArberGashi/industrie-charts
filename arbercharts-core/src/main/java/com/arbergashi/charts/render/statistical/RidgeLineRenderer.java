package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;
/**
 * RidgeLine Renderer for JDK 25.
 * Known as "Joyplot" - ideal for comparing distributions over time.
 * Utilizes a vertical offset based on the x-value to create overlapping ridges.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class RidgeLineRenderer extends BaseRenderer {

    private transient double[] xs;
    private transient double[] ys;

    public RidgeLineRenderer() {
        super("ridgeline");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;
        if (xs == null || xs.length < n) {
            xs = RendererAllocationCache.getDoubleArray(this, "ridgeline.xs", n);
            ys = RendererAllocationCache.getDoubleArray(this, "ridgeline.ys", n);
        }
        ArberRect bounds = context.getPlotBounds();
        final double baselineY = bounds.maxY();
        final double[] pix = pBuffer();
        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            double y = model.getY(i);
            context.mapToPixel(x, y, pix);
            xs[i] = pix[0];
            ys[i] = pix[1] - (x * 0.5);
        }
        float[] px = RendererAllocationCache.getFloatArray(this, "ridgeline.px", n + 2);
        float[] py = RendererAllocationCache.getFloatArray(this, "ridgeline.py", n + 2);
        int count = 0;
        px[count] = (float) xs[0];
        py[count] = (float) baselineY;
        count++;
        px[count] = (float) xs[0];
        py[count] = (float) ys[0];
        count++;
        for (int i = 1; i < n; i++) {
            px[count] = (float) xs[i];
            py[count] = (float) ys[i];
            count++;
        }
        px[count] = (float) xs[n - 1];
        py[count] = (float) baselineY;
        count++;
        ArberColor baseColor = seriesOrBase(model, context, 0);
        canvas.setColor(ColorUtils.applyAlpha(baseColor, 0.8f));
        canvas.fillPolygon(px, py, count);
        ArberColor multiStroke = themeSeries(context, 1);
        if (multiStroke == null) multiStroke = baseColor;
        ArberColor strokeColor = isMultiColor() ? ColorUtils.applyAlpha(multiStroke, 0.7f) : themeBackground(context);
        canvas.setColor(strokeColor);
        canvas.setStroke(ChartScale.scale(1.5f));
        canvas.drawPolyline(px, py, count);
    }
}
