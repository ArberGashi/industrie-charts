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
 * Renders bubbles whose area is proportional to each positive model weight.
 * Nonfinite coordinates and nonpositive or nonfinite weights are omitted.
 * The largest bubble has a device-scaled diameter of 50 pixels.
 *
 * @author Arber Gashi
 * @since 2025-06-01
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
        double[] xData = model.getXData();
        double[] yData = model.getYData();
        double[] weightData = model.getWeightData();
        if (xData == null || yData == null || weightData == null) return;
        final int n = Math.min(model.getPointCount(),
                Math.min(weightData.length, Math.min(xData.length, yData.length)));
        if (n <= 0) return;

        ArberColor color = getSeriesColor(model);

        double maxWeight = 0.0;
        for (int i = 0; i < n; i++) {
            double w = weightData[i];
            if (Double.isFinite(xData[i]) && Double.isFinite(yData[i])
                    && Double.isFinite(w) && w > maxWeight) maxWeight = w;
        }
        if (maxWeight <= 0.0) return;

        ArberRect viewBounds = context.getPlotBounds();
        double maxBubbleSize = ChartScale.scale(50.0);

        float[] px = RendererAllocationCache.getFloatArray(this, "bubble.x", 8);
        float[] py = RendererAllocationCache.getFloatArray(this, "bubble.y", 8);

        canvas.setColor(color);
        for (int i = 0; i < n; i++) {
            double weight = weightData[i];
            if (!Double.isFinite(weight) || weight <= 0.0
                    || !Double.isFinite(xData[i]) || !Double.isFinite(yData[i])) continue;
            double size = Math.sqrt(weight / maxWeight) * maxBubbleSize;
            double halfSize = size / 2.0;

            context.mapToPixel(xData[i], yData[i], p0);
            if (!Double.isFinite(p0[0]) || !Double.isFinite(p0[1])) continue;

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
