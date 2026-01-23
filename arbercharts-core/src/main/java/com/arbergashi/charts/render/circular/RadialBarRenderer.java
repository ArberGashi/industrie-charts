package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.MathUtils;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.Optional;

/**
 * <h1>Modern Radial Bar Renderer</h1>
 * <p>
 * Draws a professional, interactive radial bar chart, adhering to strict zero-allocation guidelines.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Concentric Grid:</b> A clean, circular grid for easy value comparison.</li>
 *     <li><b>Styled Bars:</b> Bars have rounded caps for a modern look.</li>
 *     <li><b>Hover Effect:</b> Bars are highlighted on mouse hover with a tooltip.</li>
 *     <li><b>Smart Labels:</b> Category labels are drawn at the end of each bar.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
 */
public final class RadialBarRenderer extends BaseRenderer {

    private int hoverIndex = -1;
    private final Arc2D.Double arc = new Arc2D.Double();
    private final NumberFormat labelFormat = NumberFormat.getInstance();
    private final Font labelFont;

    public RadialBarRenderer() {
        super("radialBar");
        this.labelFont = getCachedFont(10f, Font.PLAIN);
        this.labelFormat.setMaximumFractionDigits(1);
    }

    private void drawRadialGrid(Graphics2D g2, double cx, double cy, double maxRadius, double maxValue, PlotContext context) {
        ChartTheme t = resolveTheme(context);
        g2.setColor(t.getGridColor());
        g2.setStroke(getCachedStroke(1.0f));
        g2.setFont(getCachedFont(9f, Font.PLAIN));

        int ringCount = 4;
        for (int i = 1; i <= ringCount; i++) {
            double r = maxRadius * ((double) i / ringCount);
            g2.draw(getEllipse(cx - r, cy - r, r * 2, r * 2));

            String label = labelFormat.format(maxValue * ((double) i / ringCount));
            g2.drawString(label, (float) (cx + 5), (float) (cy - r - 5));
        }
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return;

        Rectangle2D b = context.plotBounds();
        double cx = b.getCenterX();
        double cy = b.getCenterY();
        double maxRadius = Math.min(b.getWidth(), b.getHeight()) / 2.0 * 0.85;

        drawRadialGrid(g2, cx, cy, maxRadius, context.maxY(), context);
        drawBars(g2, model, context, cx, cy, maxRadius);
    }

    private void drawBars(Graphics2D g2, ChartModel model, PlotContext context, double cx, double cy, double maxRadius) {
        int n = model.getPointCount();
        double barWidth = (maxRadius * 0.8) / n;
        double gap = barWidth * 0.4;

        for (int i = 0; i < n; i++) {
            double value = model.getY(i);
            double t = MathUtils.clamp(value / context.maxY(), 0, 1);
            double angleSweep = t * 360.0;

            double innerRadius = maxRadius - (i + 1) * barWidth + gap;
            double outerRadius = maxRadius - i * barWidth;
            double diameter = (innerRadius + outerRadius);

            float strokeWidth = (float) (outerRadius - innerRadius);
            g2.setStroke(getCachedStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Color color = getSeriesColor(model, i, context);
            if (i == hoverIndex) {
                color = color.brighter();
            }
            g2.setColor(color);

            arc.setArc(cx - diameter / 2, cy - diameter / 2, diameter, diameter, 90, -angleSweep, Arc2D.OPEN);
            g2.draw(arc);

            drawBarLabel(g2, model.getLabel(i), cx, cy, innerRadius, angleSweep, resolveTheme(context));
        }
    }

    private void drawBarLabel(Graphics2D g2, String label, double cx, double cy, double radius, double angleSweep, ChartTheme t) {
        if (label == null || label.isEmpty() || angleSweep < 10) return;

        g2.setFont(labelFont);

        double angleRad = Math.toRadians(90 - angleSweep);
        double x = cx + Math.cos(angleRad) * (radius + ChartScale.scale(8));
        double y = cy - Math.sin(angleRad) * (radius + ChartScale.scale(8));

        g2.setColor(t.getAxisLabelColor());
        g2.drawString(label, (float)x, (float)y);
    }


    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return Optional.empty();

        Rectangle2D b = context.plotBounds();
        double cx = b.getCenterX();
        double cy = b.getCenterY();
        double maxRadius = Math.min(b.getWidth(), b.getHeight()) / 2.0 * 0.85;

        double dx = pixel.getX() - cx;
        double dy = pixel.getY() - cy;
        double distSq = dx * dx + dy * dy;

        if (distSq > maxRadius * maxRadius) {
            hoverIndex = -1;
            return Optional.empty();
        }

        double barWidth = (maxRadius * 0.8) / n;
        double dist = Math.sqrt(distSq);

        for (int i = 0; i < n; i++) {
            double innerRadius = maxRadius - (i + 1) * barWidth;
            double outerRadius = maxRadius - i * barWidth;

            if (dist >= innerRadius && dist <= outerRadius) {
                hoverIndex = i;
                return Optional.of(i);
            }
        }

        hoverIndex = -1;
        return Optional.empty();
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
