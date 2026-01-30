package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
/**
 * Change-point detection overlay renderer.
 *
 * <p>Detects abrupt changes based on a simple derivative threshold and draws vertical markers.
 * Intended as a lightweight, no-dependency visualization aid.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class ChangePointRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];
    private double[] diffBuffer = new double[256];

    public ChangePointRenderer() {
        super("changePoint");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 4) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        // Estimate typical delta (median of abs diff) to scale threshold.
        int n = count - 1;
        if (diffBuffer.length < n) diffBuffer = RendererAllocationCache.getDoubleArray(this, "diffBuffer", Math.max(n, diffBuffer.length * 2));

        for (int i = 1; i < count; i++) diffBuffer[i - 1] = Math.abs(yData[i] - yData[i - 1]);

        // Partial sort or full sort (using Arrays.sort for simplicity on primitive array is efficient enough here)
        java.util.Arrays.sort(diffBuffer, 0, n);
        double med = diffBuffer[n / 2];
        if (med < 1e-12) med = 1e-12;

        double threshold = med * 6.0;

        ArberColor base = seriesOrBase(model, context, 0);
        canvas.setColor(base);
        canvas.setStroke((float) ChartScale.scale(1.0));

        ArberRect bounds = context.getPlotBounds();
        double yTop = bounds.y();
        double yBottom = bounds.maxY();

        for (int i = 1; i < count; i++) {
            double d = yData[i] - yData[i - 1];
            if (Math.abs(d) < threshold) continue;

            context.mapToPixel(xData[i], yData[i], pBuffer);
            double x = pBuffer[0];
            if (isMultiColor()) {
                ArberColor marker = themeSeries(context, i);
                if (marker == null) marker = base;
                canvas.setColor(marker);
            }
            float[] xs = RendererAllocationCache.getFloatArray(this, "cp.line.x", 2);
            float[] ys = RendererAllocationCache.getFloatArray(this, "cp.line.y", 2);
            xs[0] = (float) x;
            ys[0] = (float) yTop;
            xs[1] = (float) x;
            ys[1] = (float) yBottom;
            canvas.drawPolyline(xs, ys, 2);
        }
    }
}
