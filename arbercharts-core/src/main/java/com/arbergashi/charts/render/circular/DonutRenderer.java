package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Optional;
import javax.swing.*;

/**
 * <h1>Modern Donut Renderer</h1>
 * <p>
 * Draws a professional, interactive donut chart with a customizable center content area,
 * adhering to strict zero-allocation guidelines.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Center Content:</b> API to display text (e.g., total, title) in the center hole.</li>
 *     <li><b>Smart Labels:</b> Inherits intelligent external labeling from {@link PieRenderer}.</li>
 *     <li><b>Hover Effect:</b> Inherits segment highlighting from {@link PieRenderer}.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class DonutRenderer extends PieRenderer {

    private static final double INNER_FACTOR = 0.55;

    private double lastCx, lastCy, lastOuterR;
    private String centerText;
    private String centerSubText;
    
    private final Font centerTextFont;
    private final Font centerSubTextFont;

    public DonutRenderer() {
        super("donut");
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = new Font("SansSerif", Font.PLAIN, 12);
        this.centerTextFont = base.deriveFont(Font.BOLD, ChartScale.uiFontSize(base, 18f));
        this.centerSubTextFont = base.deriveFont(Font.PLAIN, ChartScale.uiFontSize(base, 12f));
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        setupQualityHints(g2);
        super.drawData(g2, model, context);

        Rectangle2D plot = context.plotBounds();
        if (plot == null || plot.getWidth() <= 1 || plot.getHeight() <= 1) return;

        double diameter = Math.min(plot.getWidth(), plot.getHeight()) * 0.7;
        if (diameter <= 1) return;

        double cx = plot.getCenterX();
        double cy = plot.getCenterY();

        lastCx = cx;
        lastCy = cy;
        lastOuterR = diameter * 0.5;

        double innerDiameter = diameter * INNER_FACTOR;
        if (innerDiameter <= 0) return;

        ChartTheme t = resolveTheme(context);
        g2.setColor(t.getBackground());
        g2.fill(getEllipse(cx - innerDiameter / 2.0, cy - innerDiameter / 2.0, innerDiameter, innerDiameter));

        drawCenterText(g2, cx, cy, t);
    }

    private void drawCenterText(Graphics2D g2, double cx, double cy, ChartTheme t) {
        if (centerText == null && centerSubText == null) return;

        FontMetrics fm;
        int yOffset = 0;

        if (centerText != null) {
            g2.setFont(centerTextFont);
            fm = g2.getFontMetrics();
            g2.setColor(t.getForeground());
            float tx = (float) (cx - fm.stringWidth(centerText) / 2.0);
            if (centerSubText != null) {
                yOffset = -fm.getHeight() / 2;
            }
            float ty = (float) (cy + fm.getAscent() / 2.0) + yOffset;
            g2.drawString(centerText, tx, ty);
        }

        if (centerSubText != null) {
            g2.setFont(centerSubTextFont);
            fm = g2.getFontMetrics();
            g2.setColor(t.getAxisLabelColor());
            float tx = (float) (cx - fm.stringWidth(centerSubText) / 2.0);
            if (centerText != null) {
                yOffset = fm.getHeight();
            }
            float ty = (float) (cy + fm.getAscent() / 2.0) + yOffset;
            g2.drawString(centerSubText, tx, ty);
        }
    }


    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        if (lastOuterR <= 0) return Optional.empty();

        double dx = pixel.getX() - lastCx;
        double dy = pixel.getY() - lastCy;
        double d2 = dx * dx + dy * dy;

        double innerR = lastOuterR * INNER_FACTOR;
        if (d2 < innerR * innerR) {
            setHoverIndex(-1);
            return Optional.empty();
        }

        return super.getPointAt(pixel, model, context);
    }

    // --- Public API for Center Content ---

    public DonutRenderer setCenterText(String text) {
        this.centerText = text;
        return this;
    }

    public DonutRenderer setCenterSubText(String text) {
        this.centerSubText = text;
        return this;
    }
}
