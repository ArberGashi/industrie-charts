package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;
import com.arbergashi.charts.util.MathUtils;

import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;

/**
 * <h1>Modern Gauge Renderer</h1>
 * <p>
 * Draws a professional, highly configurable radial gauge, adhering to strict zero-allocation guidelines.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Configurable Bands:</b> Define colored ranges (e.g., green, yellow, red).</li>
 *     <li><b>Custom Range:</b> Explicitly set the min/max values for the gauge scale.</li>
 *     <li><b>Modern Needle:</b> A sleek, animated needle design.</li>
 *     <li><b>Center Value Display:</b> Prominently displays the current value and unit.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class GaugeRenderer extends BaseRenderer implements ActionListener, HierarchyListener {

    /**
     * Defines a colored value band on the gauge scale.
     *
     * @param from start value (inclusive)
     * @param to end value (inclusive)
     * @param color band color
     */
    public record Band(double from, double to, Color color) {}

    private double minValue = 0.0;
    private double maxValue = 100.0;
    private double value = 0.0;
    private String unit = "";
    private List<Band> bands = List.of();

    private final Path2D.Double needleShape = new Path2D.Double();
    private final Arc2D.Double arc = new Arc2D.Double();
    private final NumberFormat valueFormat = NumberFormat.getInstance();
    
    private final Font valueFont;
    private final Font unitFont;
    private final Font tickFont;

    private double animatedValue = 0.0;
    private final Timer animationTimer;
    private Component repaintTarget;

    public GaugeRenderer() {
        super("gauge");
        double needleWidth = ChartScale.scale(4);
        double needleLength = ChartScale.scale(60);
        needleShape.moveTo(0, -needleWidth);
        needleShape.lineTo(needleLength, 0);
        needleShape.lineTo(0, needleWidth);
        needleShape.closePath();
        
        valueFormat.setMaximumFractionDigits(1);
        
        this.valueFont = getCachedFont(24f, Font.BOLD);
        this.unitFont = getCachedFont(14f, Font.PLAIN);
        this.tickFont = getCachedFont(10f, Font.PLAIN);
        
        animationTimer = new Timer(16, this);
        animationTimer.setRepeats(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        double diff = value - animatedValue;
        if (Math.abs(diff) < 0.1) {
            animatedValue = value;
            animationTimer.stop();
        } else {
            animatedValue += diff * 0.1; // Ease-out
        }
        if (repaintTarget != null) {
            repaintTarget.repaint();
        }
    }
    
    @Override
    public void hierarchyChanged(HierarchyEvent e) {
        if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
            if (e.getComponent().getParent() == null) {
                if (animationTimer != null && animationTimer.isRunning()) {
                    animationTimer.stop();
                }
                if (repaintTarget != null) {
                    repaintTarget.removeHierarchyListener(this);
                    repaintTarget = null;
                }
            }
        }
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    private void drawBands(Graphics2D g2, double cx, double cy, double radius, ChartTheme theme) {
        double startAngle = 225;
        double sweepAngle = 270;
        float bandWidth = (float) (radius * 0.2);
        g2.setStroke(getCachedStroke(bandWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));

        if (bands == null || bands.isEmpty()) {
            double lowT = 0.0;
            double midT = 0.60;
            double highT = 0.85;

            double lowAngle = startAngle - sweepAngle * lowT;
            double midAngle = startAngle - sweepAngle * midT;
            double highAngle = startAngle - sweepAngle * highT;

            g2.setColor(theme.getBearishColor());
            arc.setArc(cx - radius, cy - radius, radius * 2, radius * 2, lowAngle, -(midT - lowT) * sweepAngle, Arc2D.OPEN);
            g2.draw(arc);

            g2.setColor(theme.getAccentColor());
            arc.setArc(cx - radius, cy - radius, radius * 2, radius * 2, midAngle, -(highT - midT) * sweepAngle, Arc2D.OPEN);
            g2.draw(arc);

            g2.setColor(theme.getBullishColor());
            arc.setArc(cx - radius, cy - radius, radius * 2, radius * 2, highAngle, -(1.0 - highT) * sweepAngle, Arc2D.OPEN);
            g2.draw(arc);
            return;
        }

        for (Band band : bands) {
            double bandStartT = MathUtils.clamp((band.from - minValue) / (maxValue - minValue), 0, 1);
            double bandEndT = MathUtils.clamp((band.to - minValue) / (maxValue - minValue), 0, 1);

            double bandStartAngle = startAngle - sweepAngle * bandStartT;
            double bandSweep = -sweepAngle * (bandEndT - bandStartT);

            g2.setColor(band.color);
            arc.setArc(cx - radius, cy - radius, radius * 2, radius * 2, bandStartAngle, bandSweep, Arc2D.OPEN);
            g2.draw(arc);
        }
    }

    private void drawTicksAndLabels(Graphics2D g2, double cx, double cy, double radius, ChartTheme theme) {
        g2.setColor(theme.getAxisLabelColor());
        g2.setFont(tickFont);
        FontMetrics fm = g2.getFontMetrics();

        int numTicks = 11;
        for (int i = 0; i < numTicks; i++) {
            double t = (double) i / (numTicks - 1);
            double angleRad = Math.toRadians(225 - 270 * t);
            
            double x1 = cx + Math.cos(angleRad) * radius;
            double y1 = cy - Math.sin(angleRad) * radius;
            double x2 = cx + Math.cos(angleRad) * (radius - ChartScale.scale(5));
            double y2 = cy - Math.sin(angleRad) * (radius - ChartScale.scale(5));
            
            g2.draw(getLine(x1, y1, x2, y2));
            
            double val = minValue + t * (maxValue - minValue);
            String label = valueFormat.format(val);
            double labelRadius = radius + ChartScale.scale(15);
            double lx = cx + Math.cos(angleRad) * labelRadius;
            double ly = cy - Math.sin(angleRad) * labelRadius;
            
            g2.drawString(label, (float)(lx - fm.stringWidth(label)/2.0), (float)(ly + fm.getAscent()/2.0));
        }
    }

    private void drawNeedle(Graphics2D g2, double cx, double cy, double radius, ChartTheme theme) {
        double range = maxValue - minValue;
        if (range <= 0) return;
        
        double t = (animatedValue - minValue) / range;
        t = MathUtils.clamp(t, 0, 1);
        
        double angleDeg = 225 - 270 * t;
        
        AffineTransform old = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(Math.toRadians(-angleDeg));
        
        g2.setColor(theme.getAccentColor());
        Path2D transformedNeedle = new Path2D.Double(needleShape, AffineTransform.getScaleInstance(radius / 60, radius / 60));
        g2.fill(transformedNeedle);
        
        g2.setTransform(old);
        
        double hubRadius = ChartScale.scale(8);
        g2.setColor(ColorUtils.withAlpha(theme.getForeground(), 0.2f));
        g2.fill(getEllipse(cx - hubRadius, cy - hubRadius, hubRadius * 2, hubRadius * 2));
        g2.setColor(theme.getBackground());
        g2.fill(getEllipse(cx - hubRadius * 0.6, cy - hubRadius * 0.6, hubRadius * 1.2, hubRadius * 1.2));
    }

    private void drawCenterText(Graphics2D g2, double cx, double cy, ChartTheme theme) {
        String valueStr = valueFormat.format(this.value);
        
        g2.setFont(valueFont);
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(theme.getForeground());
        float y = (float) (cy + ChartScale.scale(40));
        g2.drawString(valueStr, (float)(cx - fm.stringWidth(valueStr)/2.0), y);
        
        if (unit != null && !unit.isEmpty()) {
            g2.setFont(unitFont);
            fm = g2.getFontMetrics();
            g2.setColor(theme.getAxisLabelColor());
            g2.drawString(unit, (float)(cx - fm.stringWidth(unit)/2.0), y + fm.getHeight());
        }
    }


    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        if (context instanceof Component newTarget && this.repaintTarget != newTarget) {
            if (this.repaintTarget != null) this.repaintTarget.removeHierarchyListener(this);
            this.repaintTarget = newTarget;
            this.repaintTarget.addHierarchyListener(this);
        }

        if (model != null && model.getPointCount() > 0) {
            updateValue(model.getY(0));
        }

        Rectangle2D b = context.plotBounds();
        if (b == null || b.getWidth() <= 1 || b.getHeight() <= 1) return;

        double size = Math.min(b.getWidth(), b.getHeight());
        double cx = b.getCenterX();
        double cy = b.getCenterY() + size * 0.15;
        double radius = size * 0.4;

        ChartTheme theme = resolveTheme(context);

        drawBands(g2, cx, cy, radius, theme);
        drawTicksAndLabels(g2, cx, cy, radius, theme);
        drawNeedle(g2, cx, cy, radius, theme);
        drawCenterText(g2, cx, cy, theme);
    }

    private void updateValue(double value) {
        if (Math.abs(this.value - value) > 1e-6) {
            this.value = value;
            if (animationTimer != null && !animationTimer.isRunning()) {
                animationTimer.start();
            }
        }
    }
    
    // --- Public API ---

    public GaugeRenderer setRange(double min, double max) {
        this.minValue = min;
        this.maxValue = max;
        return this;
    }
    
    public GaugeRenderer setValue(double value) {
        if (Math.abs(this.value - value) > 1e-6) {
            this.value = value;
            if (animationTimer != null && !animationTimer.isRunning()) {
                animationTimer.start();
            }
        }
        return this;
    }
    
    public GaugeRenderer setUnit(String unit) {
        this.unit = unit;
        return this;
    }
    
    public GaugeRenderer setBands(List<Band> bands) {
        this.bands = bands;
        return this;
    }
}
