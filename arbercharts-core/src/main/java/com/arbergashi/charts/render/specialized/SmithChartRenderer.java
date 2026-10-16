package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.CoordinateTransformProvider;
import com.arbergashi.charts.api.CoordinateTransformer;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.engine.spatial.SpatialBuffer;
import com.arbergashi.charts.engine.spatial.SpatialChunkConsumer;
import com.arbergashi.charts.engine.spatial.SpatialStyleDescriptor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.SpatialChunkRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ColorUtils;

/**
 * Minimal Smith chart renderer for complex impedance samples.
 * Expects normalized reflection coefficients in model X/Y.
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 * @since 1.5.0
 * @author Arber Gashi
 */
public final class SmithChartRenderer extends BaseRenderer implements CoordinateTransformProvider, SpatialChunkRenderer {

    private final SmithChartTransform transformer = new SmithChartTransform();
    private final double[] pixelBuf = new double[2];
    private final SpatialBuffer spatialBuffer = new SpatialBuffer(1024);

    public SmithChartRenderer() {
        super("smith_chart");
    }

    @Override
    public CoordinateTransformer getCoordinateTransformer() {
        return transformer;
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        double[] x = model.getXData();
        double[] y = model.getYData();
        if (x == null || y == null) return;
        int n = Math.min(model.getPointCount(), Math.min(x.length, y.length));
        if (n == 0) return;

        ArberColor base = themeSeries(context, isMultiColor() ? 0 : getLayerIndex());
        if (base == null) base = themeSeries(context, 0);
        float[] xs = RendererAllocationCache.getFloatArray(this, "smith.path.x", n);
        float[] ys = RendererAllocationCache.getFloatArray(this, "smith.path.y", n);
        for (int i = 0; i < n; i++) {
            transformer.mapToPixel(context, x[i], y[i], pixelBuf);
            xs[i] = (float) pixelBuf[0];
            ys[i] = (float) pixelBuf[1];
        }

        float stroke = ChartAssets.getFloat("chart.render.smith.strokeWidth", 2.2f);
        canvas.setStroke(stroke);
        canvas.setColor(ColorUtils.applyAlpha(base, 0.9f));
        if (n > 1) {
            canvas.drawPolyline(xs, ys, n);
        }

        int markerStep = Math.max(1, n / 18);
        float markerSize = ChartAssets.getFloat("chart.render.smith.markerSize", 3.2f);
        for (int i = 0; i < n; i += markerStep) {
            canvas.fillRect(xs[i] - markerSize * 0.5f, ys[i] - markerSize * 0.5f, markerSize, markerSize);
        }
        if (n > 0) {
            float endpointSize = markerSize + 1.6f;
            canvas.setColor(themeForeground(context));
            canvas.fillRect(xs[0] - endpointSize * 0.5f, ys[0] - endpointSize * 0.5f, endpointSize, endpointSize);
            canvas.fillRect(xs[n - 1] - endpointSize * 0.5f, ys[n - 1] - endpointSize * 0.5f, endpointSize, endpointSize);
        }
    }

    @Override
    public void renderSpatial(ChartModel model, PlotContext context, SpatialChunkConsumer consumer) {
        if (consumer == null || context == null) {
            return;
        }
        double[] x = model.getXData();
        double[] y = model.getYData();
        if (x == null || y == null) return;
        int n = Math.min(model.getPointCount(), Math.min(x.length, y.length));
        if (n == 0) return;

        ArberColor base = themeSeries(context, isMultiColor() ? 0 : getLayerIndex());
        if (base == null) base = themeSeries(context, 0);
        ArberColor traceColor = ColorUtils.applyAlpha(base, 0.9f);
        float stroke = ChartAssets.getFloat("chart.render.smith.strokeWidth", 2.2f);
        long traceStyle = SpatialStyleDescriptor.pack(traceColor.argb(), stroke, 0, 0);
        int capacity = spatialBuffer.getPointCapacity();
        int index = 0;
        while (index < n) {
            int count = Math.min(capacity, n - index);
            for (int i = 0; i < count; i++) {
                writeSpatialPoint(i, x[index + i], y[index + i], context, traceStyle, index + i == 0);
            }
            consumer.accept(spatialBuffer, count);
            index += count;
        }

        float markerSize = ChartAssets.getFloat("chart.render.smith.markerSize", 3.2f);
        long markerStyle = SpatialStyleDescriptor.pack(traceColor.argb(), markerSize, 0,
                SpatialStyleDescriptor.MARKER_SQUARE);
        int markerStep = Math.max(1, n / 18);
        int count = 0;
        for (int i = 0; i < n; i += markerStep) {
            writeSpatialPoint(count++, x[i], y[i], context, markerStyle, true);
            if (count == capacity) {
                consumer.accept(spatialBuffer, count);
                count = 0;
            }
        }
        if (count > 0) consumer.accept(spatialBuffer, count);

        long endpointStyle = SpatialStyleDescriptor.pack(themeForeground(context).argb(), markerSize + 1.6f, 0,
                SpatialStyleDescriptor.MARKER_SQUARE);
        writeSpatialPoint(0, x[0], y[0], context, endpointStyle, true);
        writeSpatialPoint(1, x[n - 1], y[n - 1], context, endpointStyle, true);
        consumer.accept(spatialBuffer, 2);
    }

    private void writeSpatialPoint(int index, double x, double y, PlotContext context, long style, boolean pathStart) {
        transformer.mapToPixel(context, x, y, pixelBuf);
        double[] coords = spatialBuffer.getInputCoords();
        int offset = index * 3;
        coords[offset] = pixelBuf[0];
        coords[offset + 1] = pixelBuf[1];
        coords[offset + 2] = 1.0;
        spatialBuffer.setPointMetadata(index, style, pathStart);
    }

    @Override
    public void accept(SpatialBuffer buffer, int count) {
    }
}
