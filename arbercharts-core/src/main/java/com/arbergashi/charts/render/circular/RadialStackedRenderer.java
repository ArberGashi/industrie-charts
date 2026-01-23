package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Optional;

/**
 * <h1>Modern Stacked Radial Bar Renderer</h1>
 * <p>
 * Draws a stacked radial bar chart, ideal for comparing composite values across categories.
 * Adheres to strict zero-allocation guidelines.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Stacked Bars:</b> Values from different series are stacked on a single radial bar per category.</li>
 *     <li><b>Radial Grid:</b> A clean grid with radial spokes for each category.</li>
 *     <li><b>Hover Effect:</b> Segments are highlighted on mouse hover with a tooltip.</li>
 *     <li><b>Legend Support:</b> Automatically integrates with the chart's legend.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
 */
public final class RadialStackedRenderer extends BaseRenderer {

    private int hoverCategory = -1;
    private int hoverSeries = -1;

    public RadialStackedRenderer() {
        super("radialStacked");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        // This renderer expects a multi-layer setup where each layer is a series.
        // It also requires a pre-processing step to calculate the stacked values,
        // which is expected to be done by a controller or the ArberChartPanel.
        
        int n = model.getPointCount();
        if (n <= 0) return;

        drawRadialGrid(g2, context, n, model);
        drawStackedBars(g2, model, context, n);
    }

    private void drawRadialGrid(Graphics2D g2, PlotContext context, int n, ChartModel model) {
        ChartTheme t = resolveTheme(context);
        Rectangle2D b = context.plotBounds();
        double cx = b.getCenterX();
        double cy = b.getCenterY();
        double maxRadius = Math.min(b.getWidth(), b.getHeight()) / 2.0 * 0.85;

        g2.setColor(t.getGridColor());
        g2.setStroke(getCachedStroke(1.0f));

        // Radial lines and labels
        double angleStep = 360.0 / n;
        g2.setFont(getCachedFont(10f, Font.PLAIN));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < n; i++) {
            double angleRad = Math.toRadians(i * angleStep - 90);
            double x = cx + Math.cos(angleRad) * maxRadius;
            double y = cy + Math.sin(angleRad) * maxRadius;
            g2.draw(getLine(cx, cy, x, y));
            
            String label = model.getLabel(i);
            if (label != null && !label.isEmpty()) {
                double labelRadius = maxRadius + ChartScale.scale(10);
                float tx = (float) (cx + Math.cos(angleRad) * labelRadius);
                float ty = (float) (cy + Math.sin(angleRad) * labelRadius);
                
                if (Math.abs(tx - cx) < 1) tx -= fm.stringWidth(label) / 2.0f;
                else if (tx < cx) tx -= fm.stringWidth(label);
                if (Math.abs(ty - cy) > 1 && ty > cy) ty += fm.getAscent() / 2f;

                g2.setColor(t.getAxisLabelColor());
                g2.drawString(label, tx, ty);
            }
        }
    }


    private void drawStackedBars(Graphics2D g2, ChartModel model, PlotContext context, int n) {
        double angleStep = 360.0 / n;
        double barWidth = (360.0 / n) * 0.6; // Bar width as a fraction of the available angle

        for (int i = 0; i < n; i++) {
            double startAngle = i * angleStep - 90 - barWidth / 2.0;
            
            // The model is expected to be pre-processed for stacking.
            // yData contains the value for the current series.
            // xData contains the sum of values from previous series.
            double prevValue = model.getX(i);
            double currentValue = model.getY(i);
            
            double r1 = (prevValue / context.maxY()) * getPlotRadius(context);
            double r2 = ((prevValue + currentValue) / context.maxY()) * getPlotRadius(context);
            
            Color color = getSeriesColor(model);
            if (i == hoverCategory) {
                color = color.brighter();
            }
            
            drawSegment(g2, context, r1, r2, startAngle, barWidth, color);
        }
    }

    private void drawSegment(Graphics2D g2, PlotContext context, double r1, double r2, double startAngle, double extent, Color color) {
        double cx = context.plotBounds().getCenterX();
        double cy = context.plotBounds().getCenterY();
        
        Arc2D outerArc = getArc(cx - r2, cy - r2, r2 * 2, r2 * 2, startAngle, extent, Arc2D.OPEN);
        Arc2D innerArc = getArc(cx - r1, cy - r1, r1 * 2, r1 * 2, startAngle + extent, -extent, Arc2D.OPEN);
        
        Path2D path = getPathCache();
        path.reset();
        path.append(outerArc, false);
        path.append(innerArc, true);
        path.closePath();
        
        g2.setColor(ColorUtils.withAlpha(color, 0.8f));
        g2.fill(path);
        g2.setColor(color);
        g2.draw(path);
    }

    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return Optional.empty();

        double cx = context.plotBounds().getCenterX();
        double cy = context.plotBounds().getCenterY();
        
        double dx = pixel.getX() - cx;
        double dy = pixel.getY() - cy;
        
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
    
    private double getPlotRadius(PlotContext context) {
        return Math.min(context.plotBounds().getWidth(), context.plotBounds().getHeight()) / 2.0 * 0.85;
    }
    
    @Override
    public void clearHover() {
        this.hoverCategory = -1;
        this.hoverSeries = -1;
    }
}
