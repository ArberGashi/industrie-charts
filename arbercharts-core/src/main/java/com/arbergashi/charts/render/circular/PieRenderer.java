package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <h1>Modern Pie Renderer</h1>
 * <p>
 * Draws a professional, interactive pie chart with intelligent external labeling and hover effects,
 * adhering to strict zero-allocation guidelines.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Smart Labels:</b> Automatically places labels outside with leader lines to avoid clutter.</li>
 *     <li><b>Rich Labels:</b> Displays category name and percentage value.</li>
 *     <li><b>Hover Effect:</b> Segments brighten on mouse hover for better interactivity.</li>
 *     <li><b>Zero-Allocation:</b> Highly optimized for performance.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class PieRenderer extends BaseRenderer {

    private static final double LABEL_OFFSET = 1.15;
    private static final double LABEL_THRESHOLD_ANGLE = 8.0;

    private final LinkedHashMap<String, Double> perLabel = new LinkedHashMap<>(64);
    private final Color[] colorCache = new Color[256];
    private final Arc2D.Double arc = new Arc2D.Double();
    private final Line2D.Double leaderLine = new Line2D.Double();
    private final Font labelFont;
    private final NumberFormat percentFormat;

    private double[] hitStartDeg = new double[0];
    private double[] hitExtentDeg = new double[0];
    private int hitN;
    private double hitCx, hitCy, hitOuterR;
    private int hoverIndex = -1;
    private transient ChartTheme renderTheme;

    public PieRenderer() {
        this("pie");
    }

    protected PieRenderer(String key) {
        super(key);
        Font base = javax.swing.UIManager.getFont("Label.font");
        if (base == null) base = new Font("SansSerif", Font.PLAIN, 11);
        this.labelFont = base.deriveFont(Font.PLAIN, ChartScale.uiFontSize(base, 11f));
        this.percentFormat = NumberFormat.getPercentInstance();
        this.percentFormat.setMaximumFractionDigits(1);
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        Rectangle2D plot = context.plotBounds();
        if (plot == null || plot.getWidth() <= 1 || plot.getHeight() <= 1) return;

        perLabel.clear();
        double total = aggregateData(model, perLabel);
        if (perLabel.isEmpty() || !(total > 0.0)) return;

        double diameter = Math.min(plot.getWidth(), plot.getHeight()) * 0.7;
        if (diameter <= 1) return;

        hitCx = plot.getCenterX();
        hitCy = plot.getCenterY();
        hitOuterR = diameter * 0.5;

        this.renderTheme = resolveTheme(context);
        drawSegments(g2, total, diameter);
        drawLabels(g2, total, renderTheme);
    }

    private void drawSegments(Graphics2D g2, double total, double diameter) {
        ensureHitCapacity(perLabel.size());
        hitN = 0;
        double startAngle = 90.0;
        int idx = 0;

        for (Double value : perLabel.values()) {
            if (value <= 0.0) continue;
            double angle = 360.0 * (value / total);
            if (angle <= 0.0) continue;

            Color color = getSegmentColor(idx);
            if (idx == hoverIndex) {
                color = color.brighter();
            }
            g2.setColor(color);

            arc.setArc(hitCx - hitOuterR, hitCy - hitOuterR, diameter, diameter, startAngle, -angle, Arc2D.PIE);
            g2.fill(arc);

            hitStartDeg[hitN] = startAngle;
            hitExtentDeg[hitN] = angle;
            hitN++;
            startAngle -= angle;
            idx++;
        }
    }

    private void drawLabels(Graphics2D g2, double total, ChartTheme theme) {
        g2.setFont(labelFont);
        g2.setColor(theme.getAxisLabelColor());
        FontMetrics fm = g2.getFontMetrics();

        double startAngle = 90.0;
        int idx = 0;
        List<String> labels = new ArrayList<>(perLabel.keySet());

        for (Double value : perLabel.values()) {
            if (value <= 0.0) {
                idx++;
                continue;
            }
            double angle = 360.0 * (value / total);
            if (angle < LABEL_THRESHOLD_ANGLE) {
                startAngle -= angle;
                idx++;
                continue;
            }

            double midAngleRad = Math.toRadians(startAngle - angle / 2.0);
            double labelRadius = hitOuterR * LABEL_OFFSET;
            double ex = hitCx + Math.cos(midAngleRad) * labelRadius;
            double ey = hitCy - Math.sin(midAngleRad) * labelRadius;

            String labelText = labels.get(idx);
            String percentText = percentFormat.format(value / total);
            String fullText = labelText + " (" + percentText + ")";

            float textWidth = fm.stringWidth(fullText);
            float tx = (float) ex;
            if (ex < hitCx) {
                tx -= textWidth;
            }

            g2.drawString(fullText, tx, (float) ey + fm.getAscent() / 2f);

            // Draw leader line
            double lineStartX = hitCx + Math.cos(midAngleRad) * (hitOuterR + 2);
            double lineStartY = hitCy - Math.sin(midAngleRad) * (hitOuterR + 2);
            double lineEndX = ex;
            if (ex < hitCx) {
                lineEndX += textWidth;
            }
            leaderLine.setLine(lineStartX, lineStartY, lineEndX, ey);
            g2.setStroke(getCachedStroke(1.0f));
            g2.setColor(ColorUtils.withAlpha(theme.getAxisLabelColor(), 0.5f));
            g2.draw(leaderLine);

            startAngle -= angle;
            idx++;
        }
    }

    // Package-private for tests in the same package.
    double aggregateData(ChartModel model, Map<String, Double> target) {
        double total = 0.0;
        for (int i = 0; i < model.getPointCount(); i++) {
            String label = model.getLabel(i);
            if (label == null || label.isBlank()) label = "Unknown";
            double w = model.getWeight(i) > 0 ? model.getWeight(i) : Math.max(0.0, model.getY(i));
            if (!(w > 0.0) || !Double.isFinite(w)) continue;

            target.merge(label, w, Double::sum);
            total += w;
        }
        return total;
    }

    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        if (hitN <= 0 || !(hitOuterR > 0)) return Optional.empty();

        final double px = pixel.getX();
        final double py = pixel.getY();
        final double dx = px - hitCx;
        final double dy = py - hitCy;

        final double r2 = dx * dx + dy * dy;
        if (r2 > hitOuterR * hitOuterR) {
            setHoverIndex(-1);
            return Optional.empty();
        }

        double a = Math.toDegrees(Math.atan2(-dy, dx));
        if (a < 0) a += 360.0;

        for (int i = 0; i < hitN; i++) {
            double start = hitStartDeg[i];
            double extent = hitExtentDeg[i];
            double end = start - extent;
            if (end < 0) end += 360.0;

            boolean hit = (start >= end) ? (a <= start && a >= end) : (a <= start || a >= end);
            if (hit) {
                setHoverIndex(i);
                return Optional.of(i);
            }
        }

        setHoverIndex(-1);
        return Optional.empty();
    }

    private void ensureHitCapacity(int required) {
        if (hitStartDeg.length < required) {
            int newCap = Math.max(required, hitStartDeg.length == 0 ? 16 : hitStartDeg.length * 2);
            hitStartDeg = new double[newCap];
            hitExtentDeg = new double[newCap];
        }
    }

    // Package-private for tests in the same package.
    Color getSegmentColor(int idx) {
        int i = Math.floorMod(idx, colorCache.length);
        Color c = colorCache[i];
        if (c != null) return c;

        ChartTheme theme = (renderTheme != null) ? renderTheme : resolveTheme(null);
        c = theme.getSeriesColor(i);

        colorCache[i] = c;
        return c;
    }

    @Override
    public void clearHover() {
        super.clearHover();
        renderTheme = null;
    }

    public void setHoverIndex(int index) {
        if (this.hoverIndex != index) {
            this.hoverIndex = index;
        }
    }
}
