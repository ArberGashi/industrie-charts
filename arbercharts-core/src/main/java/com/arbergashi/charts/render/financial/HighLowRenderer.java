package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;
import java.util.Optional;

/**
 * High-low renderer.
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
public final class HighLowRenderer extends BaseRenderer {

    private final double[] pixMid = new double[2];
    private transient Rectangle2D.Double[] hitBoxBuffer = new Rectangle2D.Double[0];
    private transient int hitBoxCount;

    public HighLowRenderer() {
        super("highlow");
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        ensureHitBoxCapacity(n);
        hitBoxCount = 0;

        final ChartTheme theme = resolveTheme(context);
        final Color bullish = theme.getBullishColor();
        final Color bearish = theme.getBearishColor();
        final Color tickBullish = ColorUtils.adjustBrightness(bullish, 0.7f);
        final Color tickBearish = ColorUtils.adjustBrightness(bearish, 0.7f);

        final float mainStrokeWidth = ChartScale.scale(2.0f);
        final float tickStrokeWidth = ChartScale.scale(1.2f);
        final double tickHalfWidth = ChartScale.scale(5.0);
        final double hitWidth = ChartScale.scale(12.0);

        final double[] pixHigh = pBuffer();
        final double[] pixLow = pBuffer4();

        final Stroke mainStroke = getCachedStroke(mainStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        final Stroke tickStroke = getCachedStroke(tickStrokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

        // clip optimization for x
        final Rectangle clip = g.getClipBounds();
        final double clipLeft = (clip != null) ? clip.getX() : Double.NEGATIVE_INFINITY;
        final double clipRight = (clip != null) ? clip.getMaxX() : Double.POSITIVE_INFINITY;

        for (int i = 0; i < n; i++) {
            final double xData = model.getX(i);

            context.mapToPixel(xData, model.getMax(i), pixHigh);
            context.mapToPixel(xData, model.getMin(i), pixLow);
            context.mapToPixel(xData, model.getY(i), pixMid);

            final double x = pixHigh[0];
            if (x < clipLeft - hitWidth || x > clipRight + hitWidth) continue;

            final double yHigh = pixHigh[1];
            final double yLow = pixLow[1];
            final double open = model.getWeight(i);
            final double close = model.getY(i);
            final boolean up = close >= open;
            final Color mainColor = up ? bullish : bearish;
            final Color tickColor = up ? tickBullish : tickBearish;

            // main wick
            g.setColor(mainColor);
            g.setStroke(mainStroke);
            g.draw(getLine(x, yHigh, x, yLow));

            // ticks
            g.setStroke(tickStroke);
            g.setColor(tickColor);
            g.draw(getLine(x - tickHalfWidth, yHigh, x + tickHalfWidth, yHigh));
            g.draw(getLine(x - tickHalfWidth, yLow, x + tickHalfWidth, yLow));

            // label
            String label = model.getLabel(i);
            if (label != null && !label.isEmpty()) {
                renderHighLowLabel(g, label, x, pixMid[1], tickHalfWidth);
            }

            // hit box in reusable buffer
            final Rectangle2D.Double hb = hitBoxBuffer[hitBoxCount++];
            hb.x = x - hitWidth / 2;
            hb.y = Math.min(yHigh, yLow);
            hb.width = hitWidth;
            hb.height = Math.max(Math.abs(yLow - yHigh), 1.0);
        }
    }

    private void renderHighLowLabel(Graphics2D g, String label, double x, double yMid, double tickOffset) {
        final Font currentFont = Objects.requireNonNullElseGet(g.getFont(),
                () -> new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        Font font = currentFont.deriveFont(Font.BOLD, ChartScale.uiFontSize(currentFont, 10.0f));
        Color color = getTheme().getForeground();

        final float textX = (float) (x + tickOffset + ChartScale.scale(6.0));
        final float textY = (float) (yMid);

        drawLabel(g, label, font, color, textX, textY);
    }

    public Shape getRenderedShape(ChartModel model, PlotContext context) {
        return context.plotBounds();
    }

    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        final int n = hitBoxCount;
        if (n == 0) return Optional.empty();

        for (int i = 0; i < n; i++) {
            if (hitBoxBuffer[i].contains(pixel)) return Optional.of(i);
        }
        return Optional.empty();
    }

    private void ensureHitBoxCapacity(int n) {
        if (hitBoxBuffer.length >= n) return;
        final Rectangle2D.Double[] next = new Rectangle2D.Double[n];
        for (int i = 0; i < n; i++) {
            next[i] = new Rectangle2D.Double();
        }
        hitBoxBuffer = next;
    }
}
