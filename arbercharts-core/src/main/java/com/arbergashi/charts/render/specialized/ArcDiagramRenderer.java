package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.MathUtils;
/**
 * Arc diagram renderer: draws arcs between points laid out on a single axis.
 * Optimized to reuse Path2D and avoid allocations in the draw loop.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class ArcDiagramRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("arc_diagram", new RendererDescriptor("arc_diagram", "renderer.arc_diagram", "/icons/arc.svg"), ArcDiagramRenderer::new);
    }

    private final double[] pBuffer = new double[2];

    public ArcDiagramRenderer() {
        super("arc_diagram");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();
        int limit = Math.min(count, Math.min(xData.length, yData.length));
        if (limit == 0) return;

        ArberRect bounds = context.getPlotBounds();
        double baseY = bounds.y() + bounds.height() * 0.7;

        // Precompute pixel x positions
        double[] xs = RendererAllocationCache.getDoubleArray(this, "xs", limit);
        for (int i = 0; i < limit; i++) {
            context.mapToPixel(xData[i], yData[i], pBuffer);
            xs[i] = pBuffer[0];
        }

        canvas.setStroke(getSeriesStrokeWidth());
        ArberColor baseColor = getSeriesColor(model);
        if (!isMultiColor()) {
            canvas.setColor(baseColor);
        }
        // Draw arcs limited to a small neighbor window and clip visibility test
        int neighborLimit = Math.min(30, Math.max(1, limit / 15));
        // adapt neighbor window based on density: reduce work for very large n
        if (limit > 2000) neighborLimit = Math.min(neighborLimit, 5);
        if (limit > 8000) neighborLimit = 2;
        // decimation: skip points when extremely dense
        int decimation = 1;
        if (limit > 3000) decimation = (int) Math.ceil(limit / 2000.0);
        for (int i = 0; i < limit; i++) {
            if ((i % decimation) != 0) continue;
            int maxJ = Math.min(limit - 1, i + neighborLimit);
            for (int j = i + 1; j <= maxJ; j++) {
                double x1 = xs[i];
                double x2 = xs[j];
                double minX = Math.min(x1, x2);
                double maxX = Math.max(x1, x2);
                // skip very short arcs when many points
                if (limit > 3000 && Math.abs(maxX - minX) < 2.0) continue;
                double mid = (x1 + x2) / 2.0;
                // JDK 25: Use Math.clamp() for arc height calculation
                double height = MathUtils.clamp((maxX - minX) / 2.0, 6, bounds.height() / 2.0);

                if (isMultiColor()) {
                    ArberColor arcColor = themeSeries(context, i);
                    if (arcColor == null) arcColor = baseColor;
                    canvas.setColor(arcColor);
                }
                drawArcPolyline(canvas, x1, baseY, mid, baseY - height, x2, baseY);
            }
        }
    }

    private void drawArcPolyline(ArberCanvas canvas, double x1, double y1, double cx, double cy, double x2, double y2) {
        double span = Math.abs(x2 - x1);
        int steps = (int) MathUtils.clamp(Math.ceil(span / 8.0), 8, 60);
        int count = steps + 1;
        float[] xs = RendererAllocationCache.getFloatArray(this, "arc.poly.x", count);
        float[] ys = RendererAllocationCache.getFloatArray(this, "arc.poly.y", count);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / (double) steps;
            double inv = 1.0 - t;
            double x = inv * inv * x1 + 2.0 * inv * t * cx + t * t * x2;
            double y = inv * inv * y1 + 2.0 * inv * t * cy + t * t * y2;
            xs[i] = (float) x;
            ys[i] = (float) y;
        }
        canvas.drawPolyline(xs, ys, count);
    }
}
