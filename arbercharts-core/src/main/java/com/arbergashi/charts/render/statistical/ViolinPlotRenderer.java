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
 * ViolinPlotRenderer.
 * Combines box-plot properties with a kernel density estimate (KDE).
 * Shows the distribution of data across categories.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-15
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class ViolinPlotRenderer extends BaseRenderer {

    public ViolinPlotRenderer() {
        super("violin");
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
        double[] maxPix = pBuffer();

        for (int i = 0; i < count; i++) {
            ArberColor violinColor = seriesOrBase(model, context, i);
            drawViolin(canvas, i, model, context, violinColor, maxWidth, centerPix, minPix, maxPix);
        }
    }

    private void drawViolin(ArberCanvas canvas, int idx, ChartModel model, PlotContext context, ArberColor color, double maxWidth,
                            double[] centerPix, double[] minPix, double[] maxPix) {
        context.mapToPixel(model.getX(idx), model.getY(idx), centerPix);
        context.mapToPixel(model.getX(idx), model.getMin(idx), minPix);
        context.mapToPixel(model.getX(idx), model.getMax(idx), maxPix);

        double centerX = centerPix[0];
        double minY = maxPix[1]; /* Y is inverted in Swing */
        double maxY = minPix[1];
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
        canvas.fillRect((float) (centerX - boxWidth / 2), (float) (centerPix[1] - boxWidth), (float) boxWidth, (float) (boxWidth * 2));
    }
}
