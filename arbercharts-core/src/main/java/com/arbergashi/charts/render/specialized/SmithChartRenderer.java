package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.CoordinateTransformProvider;
import com.arbergashi.charts.api.CoordinateTransformer;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.engine.spatial.SpatialBuffer;
import com.arbergashi.charts.engine.spatial.SpatialChunkConsumer;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;
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
  * @version 1.7.0
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
        int n = Math.min(x.length, y.length);
        if (n == 0) return;

        ArberColor base = seriesOrBase(model, context, 0);
        if (base == null) base = themeSeries(context, 0);
        float[] xs = RendererAllocationCache.getFloatArray(this, "smith.path.x", n);
        float[] ys = RendererAllocationCache.getFloatArray(this, "smith.path.y", n);
        for (int i = 0; i < n; i++) {
            context.mapToPixel(x[i], y[i], pixelBuf);
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
        int n = Math.min(x.length, y.length);
        if (n == 0) return;

        double[] coords = spatialBuffer.getInputCoords();
        int capacity = spatialBuffer.getPointCapacity();
        int index = 0;

        boolean markerEnabled = ChartAssets.getBoolean("Demo.smith.marker.enabled", true);
        SpatialPathBatchBuilder builder = (consumer instanceof SpatialPathBatchBuilder b) ? b : null;
        long baseStyle = (builder != null) ? builder.getStyleKey() : SpatialStyleDescriptor.getDefaultKey();
        long traceStyle = baseStyle;
        if (builder != null) {
            int argb = SpatialStyleDescriptor.unpackArgb(baseStyle);
            float width = Math.max(1.2f, ChartAssets.getFloat("chart.render.smith.strokeWidth", 2.2f));
            int dashId = SpatialStyleDescriptor.unpackDashId(baseStyle);
            traceStyle = SpatialStyleDescriptor.pack(argb, width, dashId, markerEnabled ? 2 : 0);
            builder.setStyleKey(traceStyle);
        }

        while (index < n) {
            int chunk = Math.min(capacity, n - index);
            int out = 0;
            for (int i = 0; i < chunk; i++) {
                context.mapToPixel(x[index + i], y[index + i], pixelBuf);
                coords[out++] = pixelBuf[0];
                coords[out++] = pixelBuf[1];
                // Spatial clipping keeps only z > zMin (zMin defaults to 0).
                // Use positive z to keep Smith trace visible in spatial batch pipeline.
                coords[out++] = 1.0;
            }
            consumer.accept(spatialBuffer, chunk);
            index += chunk;
        }
        if (builder != null && traceStyle != baseStyle) {
            builder.setStyleKey(baseStyle);
        }
    }

    @Override
    public void accept(SpatialBuffer buffer, int count) {
    }
}
