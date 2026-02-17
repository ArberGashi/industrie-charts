package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;
import com.arbergashi.charts.util.ChartAssets;
/**
 * Outlier detection overlay.
 *
 * <p>Marks points considered outliers based on a robust z-score using median absolute deviation (MAD).
 * This is intended as an overlay/highlighter rather than a standalone chart.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class OutlierDetectionRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];
    private double[] valBuffer = new double[256];

    public OutlierDetectionRenderer() {
        super("outlierDetection");
    }

    private static double median(double[] a, int n) {
        // Quickselect would be ideal; for small N this is fine.
        java.util.Arrays.sort(a, 0, n);
        if ((n & 1) == 1) return a[n / 2];
        return 0.5 * (a[n / 2 - 1] + a[n / 2]);
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 5) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();
        if (xData == null || yData == null) return;
        int limit = Math.min(count, Math.min(xData.length, yData.length));
        if (limit < 5) return;

        // Copy y-values (allocations kept out of render loop? This renderer is analysis-only.
        if (valBuffer.length < limit) valBuffer = RendererAllocationCache.getDoubleArray(this, "valBuffer", Math.max(limit, valBuffer.length * 2));

        System.arraycopy(yData, 0, valBuffer, 0, limit);

        // Note: median modifies the buffer (sorts it), so we need to be careful if we needed original order.
        // Here we just need the median value.
        double median = median(valBuffer, limit);
        for (int i = 0; i < limit; i++) valBuffer[i] = Math.abs(yData[i] - median); // Recalculate diffs into buffer
        double mad = median(valBuffer, limit);
        if (mad < 1e-12) {
            // fallback: tiny MAD often happens on smooth demo data; keep renderer visible and useful
            mad = 1e-6;
        }

        double threshold = ChartAssets.getFloat("chart.render.outlier.threshold", 2.8f); // robust z-score threshold

        ArberColor base = seriesOrBase(model, context, 0);
        ArberColor mark = base != null ? base : themeAccent(context);
        ArberColor halo = ColorUtils.applyAlpha(mark, 0.28f);
        ArberColor guide = ColorUtils.applyAlpha(themeForeground(context), 0.42f);

        double r = ChartScale.scale(5.0);

        // Draw an analysis baseline path so renderer is visible even with few outliers.
        float[] xs = RendererAllocationCache.getFloatArray(this, "outlier.path.x", limit);
        float[] ys = RendererAllocationCache.getFloatArray(this, "outlier.path.y", limit);
        for (int i = 0; i < limit; i++) {
            context.mapToPixel(xData[i], yData[i], pBuffer);
            xs[i] = (float) pBuffer[0];
            ys[i] = (float) pBuffer[1];
        }
        canvas.setColor(guide);
        canvas.setStroke((float) ChartScale.scale(1.4));
        canvas.drawPolyline(xs, ys, limit);

        canvas.setStroke((float) ChartScale.scale(1.8));

        for (int i = 0; i < limit; i++) {
            double z = 0.6745 * (yData[i] - median) / mad;
            if (Math.abs(z) < threshold) continue;

            context.mapToPixel(xData[i], yData[i], pBuffer);
            double x = pBuffer[0];
            double y = pBuffer[1];

            if (isMultiColor()) {
                ArberColor point = themeSeries(context, i);
                if (point == null) point = base;
                halo = point;
                mark = point;
            }
            canvas.setColor(halo);
            canvas.fillRect((float) (x - r * 2), (float) (y - r * 2), (float) (r * 4), (float) (r * 4));

            canvas.setColor(mark);
            canvas.drawRect((float) (x - r), (float) (y - r), (float) (r * 2), (float) (r * 2));
        }
    }
}
