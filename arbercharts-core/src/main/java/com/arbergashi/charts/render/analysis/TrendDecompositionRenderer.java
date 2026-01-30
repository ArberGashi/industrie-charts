package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
/**
 * Trend decomposition renderer.
 *
 * <p>Draws a trend line computed via a long-window moving average.
 * (Seasonal + residuals are outside the scope of this lightweight renderer.)</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class TrendDecompositionRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];

    public TrendDecompositionRenderer() {
        super("trendDecomposition");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 5) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        int window = Math.max(5, Math.min(401, (int) Math.round(Math.sqrt(count) * 4)));
        int half = window / 2;

        float[] xs = RendererAllocationCache.getFloatArray(this, "trend.line.x", count);
        float[] ys = RendererAllocationCache.getFloatArray(this, "trend.line.y", count);
        int outCount = 0;

        // O(N) Sliding Window Algorithm
        double sum = 0.0;
        int left = 0;
        int right = -1;

        for (int i = 0; i < count; i++) {
            int targetLeft = Math.max(0, i - half);
            int targetRight = Math.min(count - 1, i + half);

            // Expand right
            while (right < targetRight) {
                right++;
                sum += yData[right];
            }
            // Shrink left
            while (left < targetLeft) {
                sum -= yData[left];
                left++;
            }

            int windowCount = right - left + 1;
            double y = sum / windowCount;

            context.mapToPixel(xData[i], y, pBuffer);
            xs[outCount] = (float) pBuffer[0];
            ys[outCount] = (float) pBuffer[1];
            outCount++;
        }

        ArberColor base = seriesOrBase(model, context, 0);
        ArberColor accent = isMultiColor() ? themeSeries(context, 1) : base;
        if (accent == null) accent = base;
        canvas.setStroke((float) ChartScale.scale(2.0));
        if (outCount > 1 && isMultiColor() && accent != base) {
            canvas.setColor(accent);
            canvas.drawPolyline(xs, ys, outCount);
        }
        canvas.setColor(base);
        if (outCount > 1) canvas.drawPolyline(xs, ys, outCount);
    }
}
