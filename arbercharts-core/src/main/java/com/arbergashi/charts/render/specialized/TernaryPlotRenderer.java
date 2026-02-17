package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.engine.spatial.SpatialBuffer;
import com.arbergashi.charts.engine.spatial.SpatialChunkConsumer;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.TernaryChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.SpatialChunkRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;

import java.util.List;

/**
 * Renders a Ternary Plot for 3-component data.
 * It expects a TernaryChartModel.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class TernaryPlotRenderer extends BaseRenderer implements SpatialChunkRenderer {
    private final SpatialBuffer spatialBuffer = new SpatialBuffer(1024);

    public TernaryPlotRenderer() {
        super("ternary_basic");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!(model instanceof TernaryChartModel ternaryModel)) {
            return;
        }

        List<TernaryChartModel.TernaryPoint> data = ternaryModel.getTernaryData();
        if (data.isEmpty()) return;

        // Ternary plot geometry
        ArberRect bounds = context.getPlotBounds();
        double ax = bounds.centerX();
        double ay = bounds.y();
        double bx = bounds.x();
        double by = bounds.maxY();
        double cx = bounds.maxX();
        double cy = bounds.maxY();

        ArberColor base = seriesOrBase(model, context, 0);
        float[] px = RendererAllocationCache.getFloatArray(this, "ternary.point.x", 8);
        float[] py = RendererAllocationCache.getFloatArray(this, "ternary.point.y", 8);

        // Draw data points
        for (int i = 0; i < data.size(); i++) {
            TernaryChartModel.TernaryPoint p = data.get(i);
            double sum = p.getA() + p.getB() + p.getC();
            if (sum == 0) continue;

            double normA = p.getA() / sum;
            double normB = p.getB() / sum;
            double normC = p.getC() / sum;

            // Barycentric coordinates to Cartesian
            double x = ax * normA + bx * normB + cx * normC;
            double y = ay * normA + by * normB + cy * normC;

            ArberColor pointColor = isMultiColor() ? themeSeries(context, i) : base;
            if (pointColor == null) pointColor = base;
            canvas.setColor(pointColor);
            buildOctagon((float) x, (float) y, 2.5f, px, py);
            canvas.fillPolygon(px, py, 8);
        }
    }

    @Override
    public void renderSpatial(ChartModel model, PlotContext context, SpatialChunkConsumer consumer) {
        if (!(model instanceof TernaryChartModel ternaryModel) || consumer == null || context == null) {
            return;
        }
        List<TernaryChartModel.TernaryPoint> data = ternaryModel.getTernaryData();
        if (data.isEmpty()) return;

        ArberRect bounds = context.getPlotBounds();
        double ax = bounds.centerX();
        double ay = bounds.y();
        double bx = bounds.x();
        double by = bounds.maxY();
        double cx = bounds.maxX();
        double cy = bounds.maxY();

        double[] coords = spatialBuffer.getInputCoords();
        int capacity = spatialBuffer.getPointCapacity();
        int index = 0;
        int total = data.size();
        while (index < total) {
            int chunk = Math.min(capacity, total - index);
            int out = 0;
            for (int i = 0; i < chunk; i++) {
                TernaryChartModel.TernaryPoint p = data.get(index + i);
                double sum = p.getA() + p.getB() + p.getC();
                if (sum == 0) {
                    coords[out++] = ax;
                    coords[out++] = ay;
                    coords[out++] = 1.0;
                    continue;
                }
                double normA = p.getA() / sum;
                double normB = p.getB() / sum;
                double normC = p.getC() / sum;
                double x = ax * normA + bx * normB + cx * normC;
                double y = ay * normA + by * normB + cy * normC;
                coords[out++] = x;
                coords[out++] = y;
                // Spatial batch builder clips with z > zMin (default zMin=0).
                // Use positive z so ternary points are not discarded.
                coords[out++] = 1.0;
            }
            consumer.accept(spatialBuffer, chunk);
            index += chunk;
        }
    }

    @Override
    public void accept(SpatialBuffer buffer, int count) {
    }

    private static void buildOctagon(float cx, float cy, float r, float[] xs, float[] ys) {
        for (int i = 0; i < 8; i++) {
            double a = i * (Math.PI * 2.0 / 8.0);
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
    }
}
