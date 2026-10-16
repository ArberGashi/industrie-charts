package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
/**
 * <h2>BeeswarmRenderer - Beeswarm Plot (Stripchart)</h2>
 *
 * <p>Professional beeswarm plot renderer for showing individual data points without overlap.
 * Points retain their mapped Y value and use the nearest free horizontal lane
 * around their mapped X position. Placement is deterministic in model order.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>No Overlap:</b> Horizontal lane placement avoids previously placed markers</li>
 *   <li><b>All Data Visible:</b> Every data point is shown individually</li>
 *   <li><b>Distribution Shape:</b> Reveals density and outliers</li>
 *   <li><b>Category Grouping:</b> Points grouped by category (x-value)</li>
 * </ul>
 *
 * <h2>Advantages over:</h2>
 * <ul>
 *   <li><b>Box Plot:</b> Shows all individual points, not just summary statistics</li>
 *   <li><b>Violin Plot:</b> More precise, shows exact values</li>
 *   <li><b>Scatter with Jitter:</b> Intelligent layout, no random overlap</li>
 * </ul>
 *
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li>Small to medium datasets ({@code &lt; 500} points per category)</li>
 *   <li>Comparing distributions across groups</li>
 *   <li>Identifying outliers and clusters</li>
 *   <li>Showing all data points transparently</li>
 * </ul>
 *
 * @author Arber Gashi
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class BeeswarmRenderer extends BaseRenderer {

    private static final double DOT_RADIUS = 4.0;
    private final double[] p0 = new double[2];

    public BeeswarmRenderer() {
        super("beeswarm");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 1) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();
        if (xData == null || yData == null) return;
        count = Math.min(count, Math.min(xData.length, yData.length));
        final ArberColor baseColor = getSeriesColor(model);
        final double scaledRadius = ChartScale.scale(DOT_RADIUS);
        final double spacing = 2.0 * scaledRadius + ChartScale.scale(0.5);
        double[] placedX = RendererAllocationCache.getDoubleArray(this, "beeswarm.placedX", count);
        double[] placedY = RendererAllocationCache.getDoubleArray(this, "beeswarm.placedY", count);
        int[] occupied = RendererAllocationCache.getIntArray(this, "beeswarm.occupied", 2 * count + 1);
        java.util.Arrays.fill(occupied, 0, 2 * count + 1, 0);
        int placed = 0;
        for (int i = 0; i < count; i++) {
            if (!Double.isFinite(xData[i]) || !Double.isFinite(yData[i])) continue;
            context.mapToPixel(xData[i], yData[i], p0);
            double originX = p0[0];
            double y = p0[1];
            if (!Double.isFinite(originX) || !Double.isFinite(y)) continue;
            int stamp = i + 1;
            for (int j = 0; j < placed; j++) {
                double dy = y - placedY[j];
                if (Math.abs(dy) >= spacing) continue;
                double clearance = Math.sqrt(spacing * spacing - dy * dy);
                // Each prior circle excludes at most two lanes. Mark them once
                // instead of repeatedly rescanning all points for each candidate.
                int first = (int) Math.max(-count, Math.floor((placedX[j] - clearance - originX) / spacing) + 1);
                int last = (int) Math.min(count, Math.ceil((placedX[j] + clearance - originX) / spacing) - 1);
                for (int lane = first; lane <= last; lane++) occupied[count + lane] = stamp;
            }
            int lane = 0;
            while (occupied[count + lane] == stamp) lane = lane > 0 ? -lane : 1 - lane;
            double x = originX + lane * spacing;
            placedX[placed] = x;
            placedY[placed++] = y;
            ArberColor pointColor = isMultiColor() ? themeSeries(context, i) : baseColor;
            if (pointColor == null) pointColor = baseColor;
            canvas.setColor(pointColor);
            fillCircle(canvas, x, y, scaledRadius);
            canvas.setColor(themeBackground(context));
            canvas.setStroke(ChartScale.scale(0.5f));
            strokeCircle(canvas, x, y, scaledRadius);
        }
    }

    private void fillCircle(ArberCanvas canvas, double cx, double cy, double r) {
        int segments = 10;
        float[] xs = RendererAllocationCache.getFloatArray(this, "beeswarm.cx", segments);
        float[] ys = RendererAllocationCache.getFloatArray(this, "beeswarm.cy", segments);
        for (int i = 0; i < segments; i++) {
            double a = (2.0 * Math.PI * i) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.fillPolygon(xs, ys, segments);
    }

    private void strokeCircle(ArberCanvas canvas, double cx, double cy, double r) {
        int segments = 10;
        float[] xs = RendererAllocationCache.getFloatArray(this, "beeswarm.sx", segments + 1);
        float[] ys = RendererAllocationCache.getFloatArray(this, "beeswarm.sy", segments + 1);
        for (int i = 0; i <= segments; i++) {
            double a = (2.0 * Math.PI * i) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.drawPolyline(xs, ys, segments + 1);
    }
}
