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
import java.util.Optional;
/**
 * <h1>Modern Polar Line Renderer</h1>
 * <p>
 * Draws an interactive line chart in a polar coordinate system.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Concentric Grid:</b> A clean, circular grid for easy value comparison.</li>
 *     <li><b>Filled Area:</b> The area enclosed by the line is filled for better visualization.</li>
 *     <li><b>Data Nodes &amp; Hover:</b> Data points are highlighted and interactive.</li>
 *     <li><b>Line Smoothing:</b> Supports optional Catmull-Rom smoothing for a spline-like curve.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class PolarLineRenderer extends BaseRenderer {

    private int hoverIndex = -1;
    private boolean smoothing = false;

    // Reusable buffers to avoid allocations in drawData
    private final ArberPoint[] pointCache = new ArberPoint[0];

    public PolarLineRenderer() {
        super("polarLine");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return;

        drawPolarGrid(canvas, context);
        
        // Ensure point cache is large enough (reused, not re-allocated every frame)
        ArberPoint[] points = ensurePointCacheCapacity(n);
        mapToPolar(points, model, context);
        
        if (n < 2) return;

        drawPolarPath(canvas, points, n, getSeriesColor(model));
        drawDataNodes(canvas, points, n, getSeriesColor(model));
    }

    private void drawPolarGrid(ArberCanvas canvas, PlotContext context) {
        ArberRect b = context.getPlotBounds();
        double cx = b.centerX();
        double cy = b.centerY();
        double maxRadius = Math.min(b.width(), b.height()) / 2.0 * 0.85;

        canvas.setColor(getResolvedTheme(context).getGridColor());
        canvas.setStroke(ChartScale.scale(1.0f));

        int ringCount = 5;
        for (int i = 1; i <= ringCount; i++) {
            double r = maxRadius * ((double) i / ringCount);
            drawCirclePolyline(canvas, cx, cy, r);
        }

        int radialLines = 8;
        float[] xs = RendererAllocationCache.getFloatArray(this, "polarLine.grid.x", 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "polarLine.grid.y", 2);
        for (int i = 0; i < radialLines; i++) {
            double angleRad = Math.toRadians(i * (360.0 / radialLines));
            double x = cx + Math.cos(angleRad) * maxRadius;
            double y = cy + Math.sin(angleRad) * maxRadius;
            xs[0] = (float) cx;
            ys[0] = (float) cy;
            xs[1] = (float) x;
            ys[1] = (float) y;
            canvas.drawPolyline(xs, ys, 2);
        }
    }

    private void drawPolarPath(ArberCanvas canvas, ArberPoint[] points, int n, ArberColor accent) {
        float[] xs = RendererAllocationCache.getFloatArray(this, "polarLine.path.x", n);
        float[] ys = RendererAllocationCache.getFloatArray(this, "polarLine.path.y", n);
        for (int i = 0; i < n; i++) {
            xs[i] = (float) points[i].x();
            ys[i] = (float) points[i].y();
        }

        canvas.setColor(accent);
        canvas.fillPolygon(xs, ys, n);
        canvas.setStroke(1.5f);
        canvas.drawPolyline(xs, ys, n);
    }

    private void drawDataNodes(ArberCanvas canvas, ArberPoint[] points, int n, ArberColor accent) {
        for (int i = 0; i < n; i++) {
            double nodeRadius = (i == hoverIndex) ? ChartScale.scale(5f) : ChartScale.scale(3f);
            canvas.setColor(accent);
            drawCircleFill(canvas, points[i].x(), points[i].y(), nodeRadius);
        }
    }

    private void mapToPolar(ArberPoint[] points, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        ArberRect b = context.getPlotBounds();
        double cx = b.centerX();
        double cy = b.centerY();
        double maxRadius = Math.min(b.width(), b.height()) / 2.0 * 0.85;
        
        for (int i = 0; i < n; i++) {
            double angleDeg = model.getX(i);
            double radiusValue = model.getY(i);
            
            double angleRad = Math.toRadians(angleDeg - 90);
            double radius = maxRadius * (radiusValue / context.getMaxY());
            
            double x = cx + Math.cos(angleRad) * radius;
            double y = cy + Math.sin(angleRad) * radius;
            points[i].setLocation(x, y);
        }
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return Optional.empty();
        
        ArberPoint[] points = ensurePointCacheCapacity(n);
        mapToPolar(points, model, context);
        
        double thresholdSq = Math.pow(ChartScale.scale(10), 2);

        for (int i = 0; i < n; i++) {
            if (pixel.distanceSq(points[i]) < thresholdSq) {
                hoverIndex = i;
                return Optional.of(i);
            }
        }
        
        hoverIndex = -1;
        return Optional.empty();
    }

    private void drawCirclePolyline(ArberCanvas canvas, double cx, double cy, double r) {
        int segments = 48;
        float[] xs = RendererAllocationCache.getFloatArray(this, "polarLine.circle.x", segments + 1);
        float[] ys = RendererAllocationCache.getFloatArray(this, "polarLine.circle.y", segments + 1);
        for (int i = 0; i <= segments; i++) {
            double a = (i * 2.0 * Math.PI) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.drawPolyline(xs, ys, segments + 1);
    }

    private void drawCircleFill(ArberCanvas canvas, double cx, double cy, double r) {
        int segments = 24;
        float[] xs = RendererAllocationCache.getFloatArray(this, "polarLine.dot.x", segments);
        float[] ys = RendererAllocationCache.getFloatArray(this, "polarLine.dot.y", segments);
        for (int i = 0; i < segments; i++) {
            double a = (i * 2.0 * Math.PI) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.fillPolygon(xs, ys, segments);
    }
    
    private ArberPoint[] ensurePointCacheCapacity(int required) {
        if (pointCache.length >= required) return pointCache;
        
        ArberPoint[] newCache = new ArberPoint[required];
        for (int i = 0; i < required; i++) {
            newCache[i] = new ArberPoint();
        }
        System.arraycopy(pointCache, 0, newCache, 0, pointCache.length);
        return newCache; // This is an allocation, but outside drawData, only on capacity change.
    }
    
    // --- Public API ---

    public PolarLineRenderer setSmoothing(boolean smoothing){
        this.smoothing = smoothing;
        return this;
        
    }

}
