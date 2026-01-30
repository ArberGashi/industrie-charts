package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.MultiDimensionalChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorUtils;
import com.arbergashi.charts.tools.RendererAllocationCache;

import java.util.List;

/**
 * Streamgraph renderer: stacked, flowing area plots with optional smoothing.
 * Avoids allocations in the main draw loop by reusing buffers.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class StreamgraphRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("streamgraph", new RendererDescriptor("streamgraph", "renderer.streamgraph", "/icons/streamgraph.svg"), StreamgraphRenderer::new);
    }

    private final double[] pBuffer = new double[2];
    private transient double[] xs;
    private transient double[] ys;

    public StreamgraphRenderer() {
        super("streamgraph");
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        if (model instanceof MultiDimensionalChartModel multi) {
            List<double[]> data = multi.getMultiDimensionalData();
            if (data == null || data.isEmpty()) return null;
            int n = data.size();
            int series = data.get(0).length;
            double maxTotal = 0.0;
            for (int i = 0; i < n; i++) {
                double[] row = data.get(i);
                if (row == null) continue;
                double sum = 0.0;
                for (int s = 0; s < series && s < row.length; s++) {
                    double v = row[s];
                    if (Double.isFinite(v)) sum += v;
                }
                if (sum > maxTotal) maxTotal = sum;
            }
            if (!(maxTotal > 0.0)) return null;
            double half = maxTotal * 0.5;
            return new double[]{-half * 1.05, half * 1.05};
        }
        return null;
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (model instanceof MultiDimensionalChartModel multi) {
            drawStackedStream(canvas, multi, context);
            return;
        }

        final int n0 = model.getPointCount();
        if (n0 < 2) return;

        final double[] xData = model.getXData();
        final double[] yData = model.getYData();

        final int n = Math.min(n0, Math.min(xData.length, yData.length));
        if (n < 2) return;

        if (xs == null || xs.length < n) {
            xs = RendererAllocationCache.getDoubleArray(this, "xs", n);
            ys = RendererAllocationCache.getDoubleArray(this, "ys", n);
        }

        // Map to pixel space, skipping non-finite values.
        int out = 0;
        for (int i = 0; i < n; i++) {
            final double x = xData[i];
            final double y = yData[i];
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
            context.mapToPixel(x, y, pBuffer);
            final double px = pBuffer[0];
            final double py = pBuffer[1];
            if (!Double.isFinite(px) || !Double.isFinite(py)) continue;
            xs[out] = px;
            ys[out] = py;
            out++;
        }

        if (out < 2) return;

        // Optional smoothing: single pass 3-point moving average on ys (in-place).
        for (int i = 1; i < out - 1; i++) {
            ys[i] = (ys[i - 1] + ys[i] + ys[i + 1]) / 3.0;
        }

        ArberRect bounds = context.getPlotBounds();
        float baseY = (float) (bounds.y() + bounds.height());

        int polyCount = out * 2 + 2;
        float[] polyX = RendererAllocationCache.getFloatArray(this, "stream.poly.x", polyCount);
        float[] polyY = RendererAllocationCache.getFloatArray(this, "stream.poly.y", polyCount);
        int idx = 0;
        polyX[idx] = (float) xs[0];
        polyY[idx] = baseY;
        idx++;
        for (int i = 0; i < out; i++) {
            polyX[idx] = (float) xs[i];
            polyY[idx] = (float) ys[i];
            idx++;
        }
        polyX[idx] = (float) xs[out - 1];
        polyY[idx] = baseY;
        idx++;

        ArberColor base = seriesOrBase(model, context, getLayerIndex());
        canvas.setColor(ColorUtils.applyAlpha(base, 0.65f));
        canvas.fillPolygon(polyX, polyY, idx);
    }

    private void drawStackedStream(ArberCanvas canvas, MultiDimensionalChartModel model, PlotContext context) {
        List<double[]> data = model.getMultiDimensionalData();
        if (data == null || data.size() < 2) return;
        int n = data.size();
        int series = data.get(0).length;
        if (series == 0) return;

        double[] totals = RendererAllocationCache.getDoubleArray(this, "stream.totals", n);
        double[] cum = RendererAllocationCache.getDoubleArray(this, "stream.cum", n);
        double[] xVals = RendererAllocationCache.getDoubleArray(this, "stream.x", n);
        for (int i = 0; i < n; i++) {
            totals[i] = 0.0;
            cum[i] = 0.0;
            xVals[i] = i;
        }

        for (int i = 0; i < n; i++) {
            double[] row = data.get(i);
            if (row == null) continue;
            double sum = 0.0;
            for (int s = 0; s < series && s < row.length; s++) {
                double v = row[s];
                if (Double.isFinite(v)) sum += v;
            }
            totals[i] = sum;
        }

        float[] polyX = RendererAllocationCache.getFloatArray(this, "stream.stack.x", n * 2 + 2);
        float[] polyY = RendererAllocationCache.getFloatArray(this, "stream.stack.y", n * 2 + 2);
        double[] buf = pBuffer;
        ArberColor baseColor = seriesOrBase(model, context, 0);
        for (int s = 0; s < series; s++) {
            int idx = 0;
            for (int i = 0; i < n; i++) {
                double[] row = data.get(i);
                double v = (row != null && s < row.length && Double.isFinite(row[s])) ? row[s] : 0.0;
                double base = -totals[i] * 0.5;
                double upper = base + cum[i] + v;
                context.mapToPixel(xVals[i], upper, buf);
                polyX[idx] = (float) buf[0];
                polyY[idx] = (float) buf[1];
                idx++;
            }
            for (int i = n - 1; i >= 0; i--) {
                double base = -totals[i] * 0.5;
                double lower = base + cum[i];
                context.mapToPixel(xVals[i], lower, buf);
                polyX[idx] = (float) buf[0];
                polyY[idx] = (float) buf[1];
                idx++;
            }

            ArberColor fill = isMultiColor() ? themeSeries(context, s) : baseColor;
            if (fill == null) fill = baseColor;
            canvas.setColor(ColorUtils.applyAlpha(fill, 0.65f));
            canvas.fillPolygon(polyX, polyY, idx);

            // update cumulative
            for (int i = 0; i < n; i++) {
                double[] row = data.get(i);
                double v = (row != null && s < row.length && Double.isFinite(row[s])) ? row[s] : 0.0;
                cum[i] += v;
            }
        }
    }
}
