package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
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

        // Copy y-values (allocations kept out of render loop? This renderer is analysis-only.
        if (valBuffer.length < count) valBuffer = RendererAllocationCache.getDoubleArray(this, "valBuffer", Math.max(count, valBuffer.length * 2));

        System.arraycopy(yData, 0, valBuffer, 0, count);

        // Note: median modifies the buffer (sorts it), so we need to be careful if we needed original order.
        // Here we just need the median value.
        double median = median(valBuffer, count);
        for (int i = 0; i < count; i++) valBuffer[i] = Math.abs(yData[i] - median); // Recalculate diffs into buffer
        double mad = median(valBuffer, count);
        if (mad < 1e-12) return;

        double threshold = 3.5; // robust z-score threshold

        ArberColor base = seriesOrBase(model, context, 0);
        ArberColor mark = base;
        ArberColor halo = base;

        double r = ChartScale.scale(4.0);

        canvas.setStroke((float) ChartScale.scale(1.5));

        for (int i = 0; i < count; i++) {
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
