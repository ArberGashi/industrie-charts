package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
import java.util.Optional;
/**
 * <h1>Modern Stacked Polar Renderer</h1>
 * <p>
 * Draws a stacked polar area chart, ideal for comparing multiple series across different categories.
 * Adheres to strict zero-allocation guidelines.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Stacked Segments:</b> Values from different series are stacked on top of each other.</li>
 *     <li><b>Polar Grid:</b> A clean, circular grid for easy value comparison.</li>
 *     <li><b>Hover Effect:</b> Segments are highlighted on mouse hover with a tooltip.</li>
 *     <li><b>Legend Support:</b> Automatically integrates with the chart's legend.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class PolarAdvancedRenderer extends BaseRenderer {

    private int hoverCategory = -1;
    private int hoverSeries = -1;

    public PolarAdvancedRenderer() {
        super("polarAdvanced");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return;

        ArberRect b = context.getPlotBounds();
        double cx = b.centerX();
        double cy = b.centerY();
        double maxRadius = Math.min(b.width(), b.height()) / 2.0 * 0.85;
        double angleStep = 360.0 / n;

        double[] yData = model.getYData();
        double[] xData = model.getXData();
        if (yData == null || xData == null) {
            return;
        }

        drawPolarGrid(canvas, context, cx, cy, maxRadius);
        double stackMax = resolveStackMax(xData, yData, n);
        if (!Double.isFinite(stackMax) || stackMax <= 0.0) {
            return;
        }

        ArberColor strokeColor = ColorRegistry.applyAlpha(getResolvedTheme(context).getBackground(), 0.72f);
        canvas.setStroke(ChartScale.scale(1.0f));

        for (int i = 0; i < n; i++) {
            double value = Math.max(0.0, yData[i]);
            double previousValue = Math.max(0.0, xData[i]);
            double startRadius = maxRadius * (previousValue / stackMax);
            double endRadius = maxRadius * ((previousValue + value) / stackMax);
            if (!Double.isFinite(startRadius) || !Double.isFinite(endRadius) || endRadius <= startRadius) {
                continue;
            }
            double startAngle = i * angleStep - 90;

            ArberColor color = getResolvedTheme(context).getSeriesColor(i);
            if (i == hoverCategory) {
                color = ColorRegistry.adjustBrightness(color, 1.12);
            }

            drawSegment(canvas, cx, cy, startRadius, endRadius, startAngle, angleStep, color);
            drawSegmentOutline(canvas, cx, cy, startRadius, endRadius, startAngle, angleStep, strokeColor);
        }
    }

    private void drawPolarGrid(ArberCanvas canvas, PlotContext context, double cx, double cy, double maxRadius) {
        ArberColor grid = ColorRegistry.applyAlpha(getResolvedTheme(context).getGridColor(), 0.6f);
        canvas.setColor(grid);
        canvas.setStroke(ChartScale.scale(1.0f));

        int rings = 5;
        for (int i = 1; i <= rings; i++) {
            drawCirclePolyline(canvas, cx, cy, maxRadius * (i / (double) rings));
        }

        int spokes = 12;
        float[] xs = RendererAllocationCache.getFloatArray(this, "polarAdv.grid.line.x", 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "polarAdv.grid.line.y", 2);
        for (int i = 0; i < spokes; i++) {
            double a = Math.toRadians(i * (360.0 / spokes) - 90.0);
            xs[0] = (float) cx;
            ys[0] = (float) cy;
            xs[1] = (float) (cx + Math.cos(a) * maxRadius);
            ys[1] = (float) (cy + Math.sin(a) * maxRadius);
            canvas.drawPolyline(xs, ys, 2);
        }
    }

    private double resolveStackMax(double[] xData, double[] yData, int n) {
        double m = 0.0;
        for (int i = 0; i < n; i++) {
            double base = (i < xData.length && Double.isFinite(xData[i])) ? Math.max(0.0, xData[i]) : 0.0;
            double val = (i < yData.length && Double.isFinite(yData[i])) ? Math.max(0.0, yData[i]) : 0.0;
            m = Math.max(m, base + val);
        }
        return m;
    }

    private void drawSegment(ArberCanvas canvas, double cx, double cy, double r1, double r2, double startAngle, double extent, ArberColor color) {
        int segs = 24;
        int total = (segs + 1) * 2;
        float[] xs = RendererAllocationCache.getFloatArray(this, "polarAdv.seg.x", total);
        float[] ys = RendererAllocationCache.getFloatArray(this, "polarAdv.seg.y", total);
        int idx = 0;
        double start = Math.toRadians(startAngle);
        double end = Math.toRadians(startAngle + extent);
        for (int i = 0; i <= segs; i++) {
            double t = start + (end - start) * (i / (double) segs);
            xs[idx] = (float) (cx + Math.cos(t) * r2);
            ys[idx] = (float) (cy + Math.sin(t) * r2);
            idx++;
        }
        for (int i = segs; i >= 0; i--) {
            double t = start + (end - start) * (i / (double) segs);
            xs[idx] = (float) (cx + Math.cos(t) * r1);
            ys[idx] = (float) (cy + Math.sin(t) * r1);
            idx++;
        }
        canvas.setColor(color);
        canvas.fillPolygon(xs, ys, idx);
    }

    private void drawSegmentOutline(ArberCanvas canvas, double cx, double cy, double r1, double r2, double startAngle, double extent, ArberColor color) {
        int segs = 24;
        int count = segs + 1;
        float[] outerX = RendererAllocationCache.getFloatArray(this, "polarAdv.outline.outer.x", count);
        float[] outerY = RendererAllocationCache.getFloatArray(this, "polarAdv.outline.outer.y", count);
        float[] innerX = RendererAllocationCache.getFloatArray(this, "polarAdv.outline.inner.x", count);
        float[] innerY = RendererAllocationCache.getFloatArray(this, "polarAdv.outline.inner.y", count);

        double start = Math.toRadians(startAngle);
        double end = Math.toRadians(startAngle + extent);
        for (int i = 0; i <= segs; i++) {
            double t = start + (end - start) * (i / (double) segs);
            outerX[i] = (float) (cx + Math.cos(t) * r2);
            outerY[i] = (float) (cy + Math.sin(t) * r2);
            innerX[i] = (float) (cx + Math.cos(t) * r1);
            innerY[i] = (float) (cy + Math.sin(t) * r1);
        }
        canvas.setColor(color);
        canvas.drawPolyline(outerX, outerY, count);
        canvas.drawPolyline(innerX, innerY, count);
    }

    private void drawCirclePolyline(ArberCanvas canvas, double cx, double cy, double r) {
        int segments = 48;
        float[] xs = RendererAllocationCache.getFloatArray(this, "polarAdv.circle.x", segments + 1);
        float[] ys = RendererAllocationCache.getFloatArray(this, "polarAdv.circle.y", segments + 1);
        for (int i = 0; i <= segments; i++) {
            double a = (i * 2.0 * Math.PI) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.drawPolyline(xs, ys, segments + 1);
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        // This renderer requires a more complex hit-testing logic that is aware of all layers.
        // For now, we provide a simplified version that finds the category.
        int n = model.getPointCount();
        if (n <= 0) return Optional.empty();

        ArberRect b = context.getPlotBounds();
        double cx = b.centerX();
        double cy = b.centerY();
        
        double dx = pixel.x() - cx;
        double dy = pixel.y() - cy;
        
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
        if (angle < 0) angle += 360;
        
        double angleStep = 360.0 / n;
        int index = (int) (angle / angleStep);
        
        if (index >= 0 && index < n) {
            hoverCategory = index;
            // A full implementation would also determine which series (stack level) was hit.
            return Optional.of(index);
        }

        hoverCategory = -1;
        return Optional.empty();
    }
    
    @Override
    public void clearHover() {
        this.hoverCategory = -1;
        this.hoverSeries = -1;
    }
}
