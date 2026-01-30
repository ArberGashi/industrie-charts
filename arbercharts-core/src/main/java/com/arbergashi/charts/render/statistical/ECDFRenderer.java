package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.util.Arrays;
/**
 * Professional, zero-allocation ECDF (Empirical CDF) Renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class ECDFRenderer extends BaseRenderer {

    public ECDFRenderer() {
        super("ecdf");
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        return new double[]{0.0, 1.02};
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < 2) return;

        double[] values = com.arbergashi.charts.tools.RendererAllocationCache.getDoubleArray(this, "ecdf.values", n);
        for (int i = 0; i < n; i++) {
            values[i] = model.getY(i);
        }
        java.util.Arrays.sort(values, 0, n);

        double[] buf = pBuffer();
        ArberColor c = seriesOrBase(model, context, 0);
        float alpha = ChartAssets.getFloat("chart.render.ecdf.alpha", 0.9f);
        float w = ChartAssets.getFloat("chart.render.ecdf.width", 2.0f);
        canvas.setStroke(ChartScale.scale(w));
        if (!isMultiColor()) {
            float[] xs = RendererAllocationCache.getFloatArray(this, "ecdf.xs", n);
            float[] ys = RendererAllocationCache.getFloatArray(this, "ecdf.ys", n);
            int count = 0;
            for (int i = 0; i < n; i++) {
                double x = values[i];
                double y = (double) (i + 1) / n;

                context.mapToPixel(x, y, buf);
                xs[count] = (float) buf[0];
                ys[count] = (float) buf[1];
                count++;
            }
            canvas.setColor(ColorUtils.applyAlpha(c, alpha));
            canvas.drawPolyline(xs, ys, count);
            return;
        }

        double prevX = Double.NaN;
        double prevY = Double.NaN;
        for (int i = 0; i < n; i++) {
            double x = values[i];
            double y = (double) (i + 1) / n;
            context.mapToPixel(x, y, buf);
            if (i > 0) {
                ArberColor segColor = themeSeries(context, i);
                if (segColor == null) segColor = c;
                canvas.setColor(ColorUtils.applyAlpha(segColor, alpha));
                drawLine(canvas, prevX, prevY, buf[0], buf[1]);
            }
            prevX = buf[0];
            prevY = buf[1];
        }
    }

    private void drawLine(ArberCanvas canvas, double x0, double y0, double x1, double y1) {
        float[] xs = RendererAllocationCache.getFloatArray(this, "ecdf.lineX", 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "ecdf.lineY", 2);
        xs[0] = (float) x0;
        ys[0] = (float) y0;
        xs[1] = (float) x1;
        ys[1] = (float) y1;
        canvas.drawPolyline(xs, ys, 2);
    }
}
