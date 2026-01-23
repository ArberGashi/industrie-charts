package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.MultiDimensionalChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;
import com.arbergashi.charts.tools.RendererAllocationCache;
import java.util.List;

/**
 * Streamgraph renderer: stacked, flowing area plots with optional smoothing.
 * Avoids allocations in the main draw loop by reusing buffers and Path2D.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class StreamgraphRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("streamgraph", new RendererDescriptor("streamgraph", "renderer.streamgraph", "/icons/streamgraph.svg"), StreamgraphRenderer::new);
    }

    private final double[] pBuffer = new double[2];
    private transient Path2D.Double path;
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

    private Path2D.Double getPath() {
        if (path == null) path = new Path2D.Double();
        return path;
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        if (model instanceof MultiDimensionalChartModel multi) {
            drawStackedStream(g2, multi, context);
            return;
        }

        final int n0 = model.getPointCount();
        if (n0 < 2) return;

        final double[] xData = model.getXData();
        final double[] yData = model.getYData();

        // Framework contract: arrays may be backing arrays (capacity > pointCount) or could be shorter.
        final int n = Math.min(n0, Math.min(xData.length, yData.length));
        if (n < 2) return;

        if (xs == null || xs.length < n) {
            xs = RendererAllocationCache.getDoubleArray(this, "xs", n);
            ys = RendererAllocationCache.getDoubleArray(this, "ys", n);
        }

        final Rectangle clip = g2.getClipBounds();
        final Rectangle bounds = context.plotBounds().getBounds();
        if (clip != null && !clip.intersects(bounds)) return;

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

        // Optional smoothing: single pass 3-point moving average on ys (in-place, allocation-free).
        // Only apply to interior points.
        for (int i = 1; i < out - 1; i++) {
            ys[i] = (ys[i - 1] + ys[i] + ys[i + 1]) / 3.0;
        }

        // Draw a single filled area.
        Path2D.Double pth = getPath();
        pth.reset();

        final double baseY = bounds.getY() + bounds.getHeight();
        pth.moveTo(xs[0], baseY);
        for (int i = 0; i < out; i++) {
            pth.lineTo(xs[i], ys[i]);
        }
        pth.lineTo(xs[out - 1], baseY);
        pth.closePath();

        Object prevAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color base = seriesOrBase(model, context, getLayerIndex());
        g2.setColor(ColorUtils.withAlpha(base, 0.65f));
        g2.fill(pth);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, prevAA);
    }

    private void drawStackedStream(Graphics2D g2, MultiDimensionalChartModel model, PlotContext context) {
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

        Object prevAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Path2D.Double pth = getPath();
        double[] buf = pBuffer;
        Color baseColor = seriesOrBase(model, context, 0);
        for (int s = 0; s < series; s++) {
            pth.reset();
            boolean moved = false;
            for (int i = 0; i < n; i++) {
                double[] row = data.get(i);
                double v = (row != null && s < row.length && Double.isFinite(row[s])) ? row[s] : 0.0;
                double base = -totals[i] * 0.5;
                double upper = base + cum[i] + v;
                context.mapToPixel(xVals[i], upper, buf);
                if (!moved) {
                    pth.moveTo(buf[0], buf[1]);
                    moved = true;
                } else {
                    pth.lineTo(buf[0], buf[1]);
                }
            }
            for (int i = n - 1; i >= 0; i--) {
                double base = -totals[i] * 0.5;
                double lower = base + cum[i];
                context.mapToPixel(xVals[i], lower, buf);
                pth.lineTo(buf[0], buf[1]);
            }
            pth.closePath();

            Color fill = isMultiColor() ? themeSeries(context, s) : baseColor;
            if (fill == null) fill = baseColor;
            g2.setColor(ColorUtils.withAlpha(fill, 0.65f));
            g2.fill(pth);

            for (int i = 0; i < n; i++) {
                double[] row = data.get(i);
                double v = (row != null && s < row.length && Double.isFinite(row[s])) ? row[s] : 0.0;
                cum[i] += v;
            }
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, prevAA);
    }
}
