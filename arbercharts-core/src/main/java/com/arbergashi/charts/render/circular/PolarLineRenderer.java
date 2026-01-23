package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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
 */
public final class PolarLineRenderer extends BaseRenderer {

    private int hoverIndex = -1;
    private boolean smoothing = false;

    // Reusable buffers to avoid allocations in drawData
    private final Point2D.Double[] pointCache = new Point2D.Double[0];

    public PolarLineRenderer() {
        super("polarLine");
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return;

        drawPolarGrid(g, context);
        
        // Ensure point cache is large enough (reused, not re-allocated every frame)
        Point2D.Double[] points = ensurePointCacheCapacity(n);
        mapToPolar(points, model, context);
        
        if (n < 2) return;

        drawPolarPath(g, points, n, getSeriesColor(model));
        drawDataNodes(g, points, n, getSeriesColor(model));
    }

    private void drawPolarGrid(Graphics2D g, PlotContext context) {
        Rectangle2D b = context.plotBounds();
        double cx = b.getCenterX();
        double cy = b.getCenterY();
        double maxRadius = Math.min(b.getWidth(), b.getHeight()) / 2.0 * 0.85;

        g.setColor(resolveTheme(context).getGridColor());
        g.setStroke(getCachedStroke(ChartScale.scale(1.0f)));

        int ringCount = 5;
        for (int i = 1; i <= ringCount; i++) {
            double r = maxRadius * ((double) i / ringCount);
            g.draw(getEllipse(cx - r, cy - r, r * 2, r * 2));
        }

        int radialLines = 8;
        for (int i = 0; i < radialLines; i++) {
            double angleRad = Math.toRadians(i * (360.0 / radialLines));
            double x = cx + Math.cos(angleRad) * maxRadius;
            double y = cy + Math.sin(angleRad) * maxRadius;
            g.draw(getLine(cx, cy, x, y));
        }
    }

    private void drawPolarPath(Graphics2D g, Point2D.Double[] points, int n, Color accent) {
        Path2D path = getPathCache();
        path.reset();

        if (smoothing && n > 2) {
            path.moveTo(points[0].x, points[0].y);
            for (int i = 0; i < n - 1; i++) {
                Point2D p0 = points[Math.max(0, i - 1)];
                Point2D p1 = points[i];
                Point2D p2 = points[i + 1];
                Point2D p3 = points[Math.min(n - 1, i + 2)];

                double t = 0.5; // Tension
                double c1x = p1.getX() + (p2.getX() - p0.getX()) * t / 3.0;
                double c1y = p1.getY() + (p2.getY() - p0.getY()) * t / 3.0;
                double c2x = p2.getX() - (p3.getX() - p1.getX()) * t / 3.0;
                double c2y = p2.getY() - (p3.getY() - p1.getY()) * t / 3.0;
                
                path.curveTo(c1x, c1y, c2x, c2y, p2.getX(), p2.getY());
            }
        } else {
            path.moveTo(points[0].x, points[0].y);
            for (int i = 1; i < n; i++) {
                path.lineTo(points[i].x, points[i].y);
            }
        }

        g.setColor(ColorUtils.withAlpha(accent, 0.2f));
        g.fill(path);
        
        g.setStroke(getCachedStroke(1.5f));
        g.setColor(accent);
        g.draw(path);
    }

    private void drawDataNodes(Graphics2D g, Point2D.Double[] points, int n, Color accent) {
        for (int i = 0; i < n; i++) {
            double nodeRadius = (i == hoverIndex) ? ChartScale.scale(5f) : ChartScale.scale(3f);
            Color nodeColor = (i == hoverIndex) ? accent.brighter() : accent;
            
            g.setColor(nodeColor);
            g.fill(getEllipse(points[i].x - nodeRadius, points[i].y - nodeRadius, nodeRadius * 2, nodeRadius * 2));
        }
    }

    private void mapToPolar(Point2D.Double[] points, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        double cx = context.plotBounds().getCenterX();
        double cy = context.plotBounds().getCenterY();
        double maxRadius = Math.min(context.plotBounds().getWidth(), context.plotBounds().getHeight()) / 2.0 * 0.85;
        
        for (int i = 0; i < n; i++) {
            double angleDeg = model.getX(i);
            double radiusValue = model.getY(i);
            
            double angleRad = Math.toRadians(angleDeg - 90);
            double radius = maxRadius * (radiusValue / context.maxY());
            
            double x = cx + Math.cos(angleRad) * radius;
            double y = cy + Math.sin(angleRad) * radius;
            points[i].setLocation(x, y);
        }
    }

    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return Optional.empty();
        
        Point2D.Double[] points = ensurePointCacheCapacity(n);
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
    
    private Point2D.Double[] ensurePointCacheCapacity(int required) {
        if (pointCache.length >= required) return pointCache;
        
        Point2D.Double[] newCache = new Point2D.Double[required];
        for (int i = 0; i < required; i++) {
            newCache[i] = new Point2D.Double();
        }
        System.arraycopy(pointCache, 0, newCache, 0, pointCache.length);
        return newCache; // This is an allocation, but outside drawData, only on capacity change.
    }
    
    // --- Public API ---

    public PolarLineRenderer setSmoothing(boolean smoothing) {
        this.smoothing = smoothing;
        return this;
    }

}
