package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.engine.spatial.SpatialBuffer;
import com.arbergashi.charts.engine.spatial.SpatialChunkConsumer;
import com.arbergashi.charts.engine.spatial.SpatialStyleDescriptor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.TernaryChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.SpatialChunkRenderer;
import com.arbergashi.charts.render.grid.TernaryGeometry;
import com.arbergashi.charts.tools.RendererAllocationCache;

import java.util.List;

/**
 * Renders a Ternary Plot for 3-component data.
 * It expects a TernaryChartModel.
 *
 * @author Arber Gashi
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class TernaryPlotRenderer extends BaseRenderer implements SpatialChunkRenderer {
    private final SpatialBuffer spatialBuffer = new SpatialBuffer(1024);
    private final TernaryGeometry triangle = new TernaryGeometry();
    private final ArberPoint point = new ArberPoint();

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

        triangle.setBounds(context.getPlotBounds());

        ArberColor base = themeSeries(context, isMultiColor() ? 0 : getLayerIndex());
        float[] px = RendererAllocationCache.getFloatArray(this, "ternary.point.x", 8);
        float[] py = RendererAllocationCache.getFloatArray(this, "ternary.point.y", 8);

        // Draw data points
        for (int i = 0; i < data.size(); i++) {
            TernaryChartModel.TernaryPoint p = data.get(i);
            if (!triangle.map(p.getA(), p.getB(), p.getC(), point)) continue;
            ArberColor pointColor = isMultiColor() ? themeSeries(context, i) : base;
            if (pointColor == null) pointColor = base;
            canvas.setColor(pointColor);
            buildOctagon((float) point.x(), (float) point.y(), 2.5f, px, py);
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

        triangle.setBounds(context.getPlotBounds());
        ArberColor base = themeSeries(context, isMultiColor() ? 0 : getLayerIndex());
        double[] coords = spatialBuffer.getInputCoords();
        int capacity = spatialBuffer.getPointCapacity();
        int count = 0;
        for (int i = 0; i < data.size(); i++) {
            TernaryChartModel.TernaryPoint p = data.get(i);
            if (!triangle.map(p.getA(), p.getB(), p.getC(), point)) continue;
            ArberColor color = isMultiColor() ? themeSeries(context, i) : base;
            if (color == null) color = base;
            int offset = count * 3;
            coords[offset] = point.x();
            coords[offset + 1] = point.y();
            coords[offset + 2] = 1.0;
            spatialBuffer.setPointMetadata(count,
                    SpatialStyleDescriptor.pack(color.argb(), 5.0f, 0, SpatialStyleDescriptor.MARKER_OCTAGON), true);
            if (++count == capacity) {
                consumer.accept(spatialBuffer, count);
                count = 0;
            }
        }
        if (count > 0) consumer.accept(spatialBuffer, count);
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
