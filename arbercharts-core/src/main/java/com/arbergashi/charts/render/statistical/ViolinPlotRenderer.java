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
 * Draws a symmetric summary silhouette between each category's minimum and maximum,
 * with a marker at its supplied central Y value.
 * <p>The summary model does not supply per-category samples, so the silhouette
 * is not a kernel density estimate.</p>
 *
 * @author Arber Gashi
 * @since 2025-06-15
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class ViolinPlotRenderer extends BaseRenderer {

    public ViolinPlotRenderer() {
        super("violin");
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        int count = model.getPointCount();
        for (int i = 0; i < count; i++) {
            double center = model.getY(i);
            double min = model.getMin(i);
            double max = model.getMax(i);
            if (!Double.isFinite(model.getX(i)) || !Double.isFinite(center)
                    || !Double.isFinite(min) || !Double.isFinite(max)) continue;
            minY = Math.min(minY, Math.min(center, Math.min(min, max)));
            maxY = Math.max(maxY, Math.max(center, Math.max(min, max)));
        }
        return minY == Double.POSITIVE_INFINITY ? null : new double[]{minY, maxY};
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        double maxWidth = com.arbergashi.charts.util.ChartScale.scale(40.0);

        double[] centerPix = pBuffer();
        double[] minPix = pBuffer4();

        for (int i = 0; i < count; i++) {
            ArberColor violinColor = seriesOrBase(model, context, i);
            drawViolin(canvas, i, model, context, violinColor, maxWidth, centerPix, minPix);
        }
    }

    private void drawViolin(ArberCanvas canvas, int idx, ChartModel model, PlotContext context, ArberColor color, double maxWidth,
                            double[] centerPix, double[] minPix) {
        context.mapToPixel(model.getX(idx), model.getY(idx), centerPix);
        double centerX = centerPix[0];
        double centerY = centerPix[1];
        context.mapToPixel(model.getX(idx), model.getMin(idx), minPix);
        double lowerPixel = minPix[1];
        context.mapToPixel(model.getX(idx), model.getMax(idx), centerPix);

        double minY = Math.min(centerPix[1], lowerPixel);
        double maxY = Math.max(centerPix[1], lowerPixel);
        if (!Double.isFinite(centerX) || !Double.isFinite(centerY)
                || !Double.isFinite(minY) || !Double.isFinite(maxY)) return;
        double height = maxY - minY;

        int segments = 24;
        int points = segments * 2 + 1;
        float[] xs = RendererAllocationCache.getFloatArray(this, "violin.x", points);
        float[] ys = RendererAllocationCache.getFloatArray(this, "violin.y", points);

        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            double y = minY + height * t;
            double w = maxWidth * Math.sin(Math.PI * t);
            xs[i] = (float) (centerX + w);
            ys[i] = (float) y;
            xs[points - 1 - i] = (float) (centerX - w);
            ys[points - 1 - i] = (float) y;
        }

        canvas.setColor(ColorUtils.applyAlpha(color, 0.4f));
        canvas.fillPolygon(xs, ys, points);
        canvas.setColor(color);
        canvas.setStroke(ChartScale.scale(1.5f));
        canvas.drawPolyline(xs, ys, points);

        double boxWidth = ChartScale.scale(4.0);
        canvas.setColor(themeBackground(context));
        canvas.fillRect((float) (centerX - boxWidth / 2), (float) (centerY - boxWidth), (float) boxWidth, (float) (boxWidth * 2));
    }
}
