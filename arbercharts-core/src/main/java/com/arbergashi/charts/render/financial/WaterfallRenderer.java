package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Optional;

/**
 * Waterfall renderer.
 *
 * <p>Performance policy:</p>
 * <ul>
 *   <li>No allocations in the hot drawing loop.</li>
 *   <li>No usage of {@link java.awt.geom.Area}.</li>
 *   <li>Hit testing uses a reusable rectangle buffer.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class WaterfallRenderer extends BaseRenderer {

    // cached connector stroke to avoid per-frame BasicStroke/dash allocations
    private final Stroke connectorStroke = getCachedStroke(ChartScale.scale(1.2f), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
    private transient Rectangle2D.Double[] rectBuffer = new Rectangle2D.Double[0];
    private transient int rectCount;

    public WaterfallRenderer() {
        super("waterfall");
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        final ChartTheme theme = resolveTheme(context);
        final Color colorUp = theme.getBullishColor();
        final Color colorDown = theme.getBearishColor();
        final Color colorTotal = theme.getAccentColor();

        ensureRectCapacity(n);
        rectCount = 0;

        final double barWidth = (context.plotBounds().getWidth() / n) * 0.75;
        final float arc = ChartScale.scale(4.0f);

        Font baseFont = g.getFont();
        if (baseFont == null) baseFont = com.arbergashi.charts.tools.RendererAllocationCache.getFont(this, "waterfallBaseFont", Font.SANS_SERIF, Font.PLAIN, 12);
        final Font labelFont = baseFont.deriveFont(Font.BOLD, ChartScale.uiFontSize(baseFont, 10.0f));
        final FontMetrics fm = g.getFontMetrics(labelFont);

        double runningTotal = 0;
        double lastConnectorX2 = Double.NaN;
        double lastConnectorY = Double.NaN;


        // clip optimization for x
        final Rectangle clip = g.getClipBounds();
        final double clipLeft = (clip != null) ? clip.getX() : Double.NEGATIVE_INFINITY;
        final double clipRight = (clip != null) ? clip.getMaxX() : Double.POSITIVE_INFINITY;

        final double[] pix0 = pBuffer();
        final double[] pix1 = pBuffer4();
        for (int i = 0; i < n; i++) {
            final double delta = model.getY(i);
            final double nextTotal = runningTotal + delta;

            context.mapToPixel(model.getX(i), runningTotal, pix0);
            context.mapToPixel(model.getX(i), nextTotal, pix1);

            final double startX = pix0[0];
            final double startY = pix0[1];
            final double endY = pix1[1];

            final double x = startX - barWidth / 2.0;
            if (x + barWidth < clipLeft || x > clipRight) {
                runningTotal = nextTotal;
                lastConnectorX2 = x + barWidth;
                lastConnectorY = endY;
                continue;
            }

            final double y = Math.min(startY, endY);
            final double h = Math.max(Math.abs(startY - endY), ChartScale.scale(2.0));

            final Shape bar = getRoundRectangle(x, y, barWidth, h, arc, arc);

            // connectors (single cached stroke)
            if (!Double.isNaN(lastConnectorX2)) {
                g.setColor(theme.getGridColor());
                g.setStroke(connectorStroke);
                g.draw(getLine(lastConnectorX2, lastConnectorY, x, startY));
            }

            // paint
            Color baseColor = (delta >= 0) ? colorUp : colorDown;
            if (model.getWeight(i) > 0) baseColor = colorTotal;

            g.setPaint(getCachedGradient(baseColor, (float) h));
            g.fill(bar);

            // label
            final String valueText = (delta >= 0 ? "+" : "") + String.format("%.1f", delta);
            final float labelY = (float) (delta >= 0
                    ? y - ChartScale.scale(5.0)
                    : y + h + fm.getAscent() + ChartScale.scale(2.0));
            drawLabel(g, valueText, labelFont, theme.getForeground(),
                    (float) (x + (barWidth - fm.stringWidth(valueText)) / 2.0),
                    labelY);

            // store hit box in reusable buffer
            final Rectangle2D.Double r = rectBuffer[rectCount++];
            r.x = x;
            r.y = y;
            r.width = barWidth;
            r.height = h;

            lastConnectorX2 = x + barWidth;
            lastConnectorY = endY;
            runningTotal = nextTotal;
        }
    }

    public Shape getRenderedShape(ChartModel model, PlotContext context) {
        return context.plotBounds();
    }

    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        final int n = rectCount;
        if (n == 0) return Optional.empty();

        for (int i = 0; i < n; i++) {
            if (rectBuffer[i].contains(pixel)) return Optional.of(i);
        }
        return Optional.empty();
    }

    private void ensureRectCapacity(int n) {
        if (rectBuffer.length >= n) return;
        final Rectangle2D.Double[] next = new Rectangle2D.Double[n];
        for (int i = 0; i < n; i++) {
            next[i] = new Rectangle2D.Double();
        }
        rectBuffer = next;
    }
}