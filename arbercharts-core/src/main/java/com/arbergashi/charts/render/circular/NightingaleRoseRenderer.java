package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Optional;

/**
 * <h1>Modern Nightingale Rose Renderer</h1>
 * <p>
 * A modern, interactive polar area chart where segment radii represent values.
 * Adheres to strict zero-allocation guidelines.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Polar Grid:</b> A clean, circular grid for easy value comparison.</li>
 *     <li><b>Hover Effect:</b> Segments are highlighted on mouse hover.</li>
 *     <li><b>Smart Labels:</b> Category labels are placed around the circle.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class NightingaleRoseRenderer extends BaseRenderer {

    private int hoverIndex = -1;

    public NightingaleRoseRenderer() {
        super("nightingaleRose");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;

        Rectangle2D b = context.plotBounds();
        if (b == null || b.getWidth() <= 1 || b.getHeight() <= 1) return;

        double cx = b.getCenterX();
        double cy = b.getCenterY();
        double maxRadius = Math.min(b.getWidth(), b.getHeight()) / 2.0 * 0.8;
        
        drawPolarGrid(g2, context, n, model);

        double angleStep = 360.0 / n;
        double maxValue = getMaxValue(model);

        for (int i = 0; i < n; i++) {
            double valueRatio = model.getY(i) / maxValue;
            if (!Double.isFinite(valueRatio) || valueRatio < 0) valueRatio = 0.0;

            double r = valueRatio * maxRadius;
            if (r <= 0.0) continue;

            double startAngle = i * angleStep - 90.0;

            Shape arc = getArc(cx - r, cy - r, r * 2, r * 2, -startAngle, -angleStep, Arc2D.PIE);

            Color color = getSeriesColor(model, i, context);
            if (i == hoverIndex) {
                color = color.brighter();
            }
            
            g2.setColor(ColorUtils.withAlpha(color, 0.75f));
            g2.fill(arc);
            g2.setStroke(getCachedStroke(1.5f));
            g2.setColor(color);
            g2.draw(arc);
        }
    }
    
    private void drawPolarGrid(Graphics2D g, PlotContext context, int segments, ChartModel model) {
        ChartTheme t = resolveTheme(context);
        Rectangle2D b = context.plotBounds();
        double cx = b.getCenterX();
        double cy = b.getCenterY();
        double maxRadius = Math.min(b.getWidth(), b.getHeight()) / 2.0 * 0.8;

        g.setColor(t.getGridColor());
        g.setStroke(getCachedStroke(1.0f));

        // Concentric circles
        int ringCount = 4;
        for (int i = 1; i <= ringCount; i++) {
            double r = maxRadius * ((double) i / ringCount);
            g.draw(getEllipse(cx - r, cy - r, r * 2, r * 2));
        }

        // Radial lines and labels
        double angleStep = 360.0 / segments;
        g.setFont(getCachedFont(10f, Font.PLAIN));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < segments; i++) {
            double angleRad = Math.toRadians(i * angleStep - 90);
            double x = cx + Math.cos(angleRad) * maxRadius;
            double y = cy + Math.sin(angleRad) * maxRadius;
            g.draw(getLine(cx, cy, x, y));
            
            String label = model.getLabel(i);
            if (label != null && !label.isEmpty()) {
                double labelRadius = maxRadius + ChartScale.scale(10);
                float tx = (float) (cx + Math.cos(angleRad) * labelRadius);
                float ty = (float) (cy + Math.sin(angleRad) * labelRadius);
                
                if (Math.abs(tx - cx) < 1) tx -= fm.stringWidth(label) / 2.0f;
                else if (tx < cx) tx -= fm.stringWidth(label);
                if (Math.abs(ty - cy) > 1 && ty > cy) ty += fm.getAscent() / 2f;

                g.setColor(t.getAxisLabelColor());
                g.drawString(label, tx, ty);
            }
        }
    }


    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return Optional.empty();

        Rectangle2D b = context.plotBounds();
        double cx = b.getCenterX();
        double cy = b.getCenterY();
        double maxRadius = Math.min(b.getWidth(), b.getHeight()) / 2.0 * 0.8;
        
        double dx = pixel.getX() - cx;
        double dy = pixel.getY() - cy;
        double distSq = dx * dx + dy * dy;

        if (distSq > maxRadius * maxRadius) {
            hoverIndex = -1;
            return Optional.empty();
        }

        double angleStep = 360.0 / n;
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        if (angle < 0.0) angle += 360.0;

        int index = (int) (angle / angleStep);
        if (index < 0 || index >= n) {
            hoverIndex = -1;
            return Optional.empty();
        }
        
        double maxValue = getMaxValue(model);
        double valueRatio = model.getY(index) / maxValue;
        double radius = valueRatio * maxRadius;
        
        if (Math.sqrt(distSq) <= radius) {
            hoverIndex = index;
            return Optional.of(index);
        }

        hoverIndex = -1;
        return Optional.empty();
    }
    
    private double getMaxValue(ChartModel model) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < model.getPointCount(); i++) {
            if (model.getY(i) > max) {
                max = model.getY(i);
            }
        }
        return (Double.isFinite(max) && max > 0) ? max : 1.0;
    }
    
    private Color getSeriesColor(ChartModel model, int index, PlotContext context) {
        if (model.getColor() != null) return model.getColor();
        return resolveTheme(context).getSeriesColor(index);
    }
    
    @Override
    public void clearHover() {
        this.hoverIndex = -1;
    }
}
