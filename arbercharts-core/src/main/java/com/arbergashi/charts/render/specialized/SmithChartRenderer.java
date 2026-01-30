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
import com.arbergashi.charts.util.ChartAssets;

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
        canvas.setColor(base);

        for (int i = 0; i < n; i++) {
            context.mapToPixel(x[i], y[i], pixelBuf);
            canvas.fillRect((float) (pixelBuf[0] - 2), (float) (pixelBuf[1] - 2), 4f, 4f);
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

        boolean markerEnabled = ChartAssets.getBoolean("Demo.smith.marker.enabled", false);
        SpatialPathBatchBuilder builder = (consumer instanceof SpatialPathBatchBuilder b) ? b : null;
        long baseStyle = (builder != null) ? builder.getStyleKey() : SpatialStyleDescriptor.getDefaultKey();

        if (markerEnabled && builder != null && n > 0) {
            int argb = SpatialStyleDescriptor.unpackArgb(baseStyle);
            float width = SpatialStyleDescriptor.unpackStrokeWidth(baseStyle);
            int dashId = SpatialStyleDescriptor.unpackDashId(baseStyle);
            long markerStyle = SpatialStyleDescriptor.pack(argb, width, dashId, 1);
            builder.setStyleKey(markerStyle);
            context.mapToPixel(x[0], y[0], pixelBuf);
            coords[0] = pixelBuf[0];
            coords[1] = pixelBuf[1];
            coords[2] = 0.0;
            consumer.accept(spatialBuffer, 1);
            builder.setStyleKey(baseStyle);
            index = 1;
        }
        while (index < n) {
            int chunk = Math.min(capacity, n - index);
            int out = 0;
            for (int i = 0; i < chunk; i++) {
                context.mapToPixel(x[index + i], y[index + i], pixelBuf);
                coords[out++] = pixelBuf[0];
                coords[out++] = pixelBuf[1];
                coords[out++] = 0.0;
            }
            consumer.accept(spatialBuffer, chunk);
            index += chunk;
        }
    }

    @Override
    public void accept(SpatialBuffer buffer, int count) {
    }
}
