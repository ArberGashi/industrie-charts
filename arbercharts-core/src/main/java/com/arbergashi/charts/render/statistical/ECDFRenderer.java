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

/**
 * Draws the empirical cumulative distribution of finite Y observations.
 * The right-continuous step function advances only at an observed value;
 * repeated observations contribute to the same jump.
 *
 * @author Arber Gashi
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class ECDFRenderer extends BaseRenderer {

    public ECDFRenderer() {
        super("ecdf");
    }

    @Override
    public double[] getPreferredXRange(ChartModel model) {
        double[] values = RendererAllocationCache.getDoubleArray(this, "ecdf.values", model.getPointCount());
        int n = copySortedSamples(model, values);
        if (n == 0) return null;
        double min = values[0];
        double max = values[n - 1];
        double padding = min == max ? Math.max(0.5, Math.abs(min) * 0.05) : 0.0;
        return new double[]{min - padding, max + padding};
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        return new double[]{0.0, 1.02};
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        double[] values = RendererAllocationCache.getDoubleArray(this, "ecdf.values", model.getPointCount());
        int n = copySortedSamples(model, values);
        if (n == 0) return;
        float[] xs = RendererAllocationCache.getFloatArray(this, "ecdf.xs", 2 * n + 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "ecdf.ys", 2 * n + 2);
        double[] buf = pBuffer();
        context.mapToPixel(Math.min(context.getMinX(), values[0]), 0.0, buf);
        xs[0] = (float) buf[0];
        ys[0] = (float) buf[1];
        int count = 1;
        for (int i = 0; i < n;) {
            int next = i + 1;
            while (next < n && values[next] == values[i]) next++;
            context.mapToPixel(values[i], (double) i / n, buf);
            xs[count] = (float) buf[0];
            ys[count++] = (float) buf[1];
            context.mapToPixel(values[i], (double) next / n, buf);
            xs[count] = (float) buf[0];
            ys[count++] = (float) buf[1];
            i = next;
        }
        context.mapToPixel(Math.max(context.getMaxX(), values[n - 1]), 1.0, buf);
        xs[count] = (float) buf[0];
        ys[count++] = (float) buf[1];

        ArberColor c = seriesOrBase(model, context, 0);
        float alpha = ChartAssets.getFloat("chart.render.ecdf.alpha", 0.9f);
        float w = ChartAssets.getFloat("chart.render.ecdf.width", 2.0f);
        canvas.setStroke(ChartScale.scale(w));
        if (!isMultiColor()) {
            canvas.setColor(ColorUtils.applyAlpha(c, alpha));
            canvas.drawPolyline(xs, ys, count);
            return;
        }

        for (int i = 1; i < count; i++) {
            ArberColor segColor = themeSeries(context, (i - 1) / 2);
            if (segColor == null) segColor = c;
            canvas.setColor(ColorUtils.applyAlpha(segColor, alpha));
            drawLine(canvas, xs[i - 1], ys[i - 1], xs[i], ys[i]);
        }
    }

    private int copySortedSamples(ChartModel model, double[] values) {
        int n = 0;
        int count = Math.min(model.getPointCount(), values.length);
        for (int i = 0; i < count; i++) {
            double value = model.getY(i);
            if (Double.isFinite(value)) values[n++] = value;
        }
        java.util.Arrays.sort(values, 0, n);
        return n;
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
