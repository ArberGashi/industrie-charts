package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;

/**
 * Professional, zero-allocation, high-precision bubble chart renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class BubbleRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    public BubbleRenderer() {
        super("bubble");
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
        double[] weightData = yData;

        ArberColor color = getSeriesColor(model);

        double maxWeight = 1.0;
        for (int i = 0; i < n; i++) {
            double w = weightData[i];
            if (w > maxWeight) maxWeight = w;
        }

        ArberRect viewBounds = context.getPlotBounds();
        double maxBubbleSize = ChartScale.scale(50.0);

        float[] px = RendererAllocationCache.getFloatArray(this, "bubble.x", 8);
        float[] py = RendererAllocationCache.getFloatArray(this, "bubble.y", 8);

        canvas.setColor(color);
        for (int i = 0; i < n; i++) {
            double weight = weightData[i];
            double size = (weight / maxWeight) * maxBubbleSize;
            if (size < 2) size = 2;
            double halfSize = size / 2.0;

            context.mapToPixel(xData[i], yData[i], p0);

            if (p0[0] < viewBounds.minX() - halfSize || p0[0] > viewBounds.maxX() + halfSize ||
                    p0[1] < viewBounds.minY() - halfSize || p0[1] > viewBounds.maxY() + halfSize) {
                continue;
            }

            buildOctagon((float) p0[0], (float) p0[1], (float) halfSize, px, py);
            canvas.fillPolygon(px, py, 8);
        }
    }

    private static void buildOctagon(float cx, float cy, float r, float[] xs, float[] ys) {
        for (int i = 0; i < 8; i++) {
            double a = i * (Math.PI * 2.0 / 8.0);
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
    }
}
