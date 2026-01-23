package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Ternary Phase Diagram Renderer.
 * A triangle plot for mixtures of three components (A, B, C). Uses barycentric coordinates.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class TernaryPhasediagramRenderer extends BaseRenderer {

    // Cached theme resources
    private transient int themeKey;
    private transient Color sepColor;
    private transient Color sepColor30;
    private transient Color labelColor;
    private transient Stroke gridStroke;

    public TernaryPhasediagramRenderer() {
        super("ternary_phase");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        Rectangle clip = g2.getClipBounds();
        Rectangle2D bounds = context.plotBounds();
        if (clip != null && !clip.intersects(bounds.getBounds())) return;

        ChartTheme theme = resolveTheme(context);
        ensureThemeCache(theme);

        double margin = ChartScale.scale(40);
        double xBase = bounds.getX() + margin;
        double yBase = bounds.getY() + bounds.getHeight() - margin;
        double side = Math.min(bounds.getWidth(), bounds.getHeight()) - 2 * margin;
        if (side <= 1) return;

        double topX = xBase + side / 2.0;
        double topY = yBase - side * Math.sqrt(3) / 2.0;
        double leftX = xBase;
        double leftY = yBase;
        double rightX = xBase + side;
        double rightY = yBase;

        Path2D pth = getPathCache();
        pth.moveTo(topX, topY);
        pth.lineTo(leftX, leftY);
        pth.lineTo(rightX, rightY);
        pth.closePath();
        g2.setColor(sepColor);
        g2.draw(pth);

        drawGrid(g2, topX, topY, leftX, leftY, rightX, rightY);

        // Points
        int count = model.getPointCount();
        if (count > 0) {
            Color baseColor = getSeriesColor(model);
            double r = ChartScale.scale(3.0);
            double d = r * 2.0;
            for (int i = 0; i < count; i++) {
                double a = model.getX(i);
                double b = model.getY(i);
                double c = model.getWeight(i);
                double total = a + b + c;
                if (total <= 0) continue;
                double inv = 1.0 / total;
                a *= inv;
                b *= inv;
                c *= inv;

                double px = Math.fma(a, topX, Math.fma(b, leftX, c * rightX));
                double py = Math.fma(a, topY, Math.fma(b, leftY, c * rightY));

                if (clip != null) {
                    if (px < clip.x - d || px > clip.x + clip.width + d || py < clip.y - d || py > clip.y + clip.height + d)
                        continue;
                }

                Color pointColor = isMultiColor() ? theme.getSeriesColor(i) : baseColor;
                if (pointColor == null) pointColor = baseColor;
                g2.setColor(pointColor);
                g2.fill(getEllipse(px - r, py - r, d, d));
            }
        }

        // Labels (cached in BaseRenderer)
        Font font = g2.getFont();
        g2.setColor(labelColor);
        drawLabel(g2, "Component A", font, labelColor, (float) topX - 30, (float) topY - 10);
        drawLabel(g2, "Component B", font, labelColor, (float) leftX - 60, (float) leftY + 20);
        drawLabel(g2, "Component C", font, labelColor, (float) rightX, (float) rightY + 20);
    }

    public TernaryPhasediagramRenderer setMultiColor(boolean enabled) {
        super.setMultiColor(enabled);
        return this;
    }

    private void ensureThemeCache(ChartTheme theme) {
        int k = System.identityHashCode(theme);
        if (k == themeKey && sepColor != null) return;
        themeKey = k;

        sepColor = theme.getGridColor();
        sepColor30 = com.arbergashi.charts.util.ColorUtils.withAlpha(sepColor, 0.3f);
        labelColor = theme.getAxisLabelColor();

        float dash = ChartScale.scale(5.0f);
        float w = ChartScale.scale(1.0f);
        gridStroke = new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{dash}, 0.0f);
    }

    private void drawGrid(Graphics2D g2, double topX, double topY, double leftX, double leftY, double rightX, double rightY) {
        Stroke prev = g2.getStroke();
        Color prevC = g2.getColor();

        g2.setStroke(gridStroke);
        g2.setColor(sepColor30);

        int steps = 10;
        for (int i = 1; i < steps; i++) {
            double f = (double) i / steps;

            double x1 = Math.fma(f, (leftX - topX), topX);
            double y1 = Math.fma(f, (leftY - topY), topY);
            double x2 = Math.fma(f, (rightX - topX), topX);
            double y2 = Math.fma(f, (rightY - topY), topY);
            g2.draw(getLine(x1, y1, x2, y2));

            double x3 = Math.fma(f, (topX - leftX), leftX);
            double y3 = Math.fma(f, (topY - leftY), leftY);
            double x4 = Math.fma(f, (rightX - leftX), leftX);
            double y4 = Math.fma(f, (rightY - leftY), leftY);
            g2.draw(getLine(x3, y3, x4, y4));

            double x5 = Math.fma(f, (topX - rightX), rightX);
            double y5 = Math.fma(f, (topY - rightY), rightY);
            double x6 = Math.fma(f, (leftX - rightX), rightX);
            double y6 = Math.fma(f, (leftY - rightY), rightY);
            g2.draw(getLine(x5, y5, x6, y6));
        }

        g2.setColor(prevC);
        g2.setStroke(prev);
    }

}
