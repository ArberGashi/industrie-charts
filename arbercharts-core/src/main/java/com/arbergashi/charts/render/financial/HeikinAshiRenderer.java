package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;

/**
 * Heikin-Ashi Renderer - modified candlestick visualization for trend readability.
 *
 * <p>Data requirements: OHLC data in ChartPoint (min=low, max=high, weight=open, y=close).</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class HeikinAshiRenderer extends BaseRenderer {

    private final double[] pxHigh = new double[2];
    private final double[] pxLow = new double[2];
    private final double[] pxOpen = new double[2];
    private final double[] pxClose = new double[2];

    // Cached computed series
    private transient ChartModel cachedModel;
    private transient int cachedPointCount;

    private transient double[] xValues;
    private transient double[] haOpen;
    private transient double[] haClose;
    private transient double[] haHigh;
    private transient double[] haLow;

    public HeikinAshiRenderer() {
        super("heikinashi");
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        ensureCache(model);
        if (cachedPointCount <= 0 || xValues == null) return;

        final Rectangle clip = g.getClipBounds();
        final Rectangle viewBounds = (clip != null) ? clip : context.plotBounds().getBounds();

        final double w = context.plotBounds().getWidth();
        final double leftX = viewBounds.getX();
        final double rightX = viewBounds.getMaxX();
        final double step = (cachedPointCount > 1) ? (w / (double) (cachedPointCount - 1)) : w;

        int start = 0;
        int endExclusive = cachedPointCount;
        if (step > 0.0) {
            start = (int) Math.floor((leftX - context.plotBounds().getX()) / step) - 2;
            endExclusive = (int) Math.ceil((rightX - context.plotBounds().getX()) / step) + 2;
            if (start < 0) start = 0;
            if (endExclusive > cachedPointCount) endExclusive = cachedPointCount;
        }
        if (endExclusive - start <= 0) return;

        final double barWidth = (w / (double) cachedPointCount) * 0.75;

        final ChartTheme theme = resolveTheme(context);
        final Color colorBullish = theme.getBullishColor();
        final Color colorBearish = theme.getBearishColor();

        final Stroke wickStroke = getCachedStroke(ChartScale.scale(1.0f));
        final Stroke bodyStroke = getCachedStroke(ChartScale.scale(0.85f));

        for (int i = start; i < endExclusive; i++) {
            // map OHLC
            context.mapToPixel(xValues[i], haHigh[i], pxHigh);
            context.mapToPixel(xValues[i], haLow[i], pxLow);
            context.mapToPixel(xValues[i], haOpen[i], pxOpen);
            context.mapToPixel(xValues[i], haClose[i], pxClose);

            final double x = pxOpen[0];

            final boolean bullish = haClose[i] >= haOpen[i];
            final Color candleColor = bullish ? colorBullish : colorBearish;

            // wick
            g.setColor(theme.getForeground());
            g.setStroke(wickStroke);
            g.draw(getLine(pxHigh[0], pxHigh[1], pxLow[0], pxLow[1]));

            // body
            final double bodyY = Math.min(pxOpen[1], pxClose[1]);
            final double bodyH = Math.max(Math.abs(pxOpen[1] - pxClose[1]), ChartScale.scale(1.5f));
            final double bx = x - barWidth / 2.0;

            // Fast clip test
            if (bx > rightX || (bx + barWidth) < leftX) continue;
            if (bodyY > viewBounds.getMaxY() || (bodyY + bodyH) < viewBounds.getY()) continue;

            final Shape body = getRect(bx, bodyY, barWidth, bodyH);
            g.setColor(candleColor);
            g.fill(body);
            g.setColor(ColorUtils.adjustBrightness(candleColor, 0.7f));
            g.setStroke(bodyStroke);
            g.draw(body);
        }
    }

    private void ensureCache(ChartModel model) {
        final int n = model.getPointCount();
        if (n == 0) {
            cachedModel = model;
            cachedPointCount = 0;
            return;
        }

        if (cachedModel == model && cachedPointCount == n && xValues != null) {
            return;
        }

        cachedModel = model;
        cachedPointCount = n;

        ensureCapacity(n);

        // Seed with first candle
        xValues[0] = model.getX(0);

        // heikin close uses OHLC average
        double prevHaClose = (model.getWeight(0) + model.getMax(0) + model.getMin(0) + model.getY(0)) * 0.25;
        double prevHaOpen = (model.getWeight(0) + model.getY(0)) * 0.5;

        haOpen[0] = prevHaOpen;
        haClose[0] = prevHaClose;
        haHigh[0] = Math.max(model.getMax(0), Math.max(prevHaOpen, prevHaClose));
        haLow[0] = Math.min(model.getMin(0), Math.min(prevHaOpen, prevHaClose));

        for (int i = 1; i < n; i++) {
            xValues[i] = model.getX(i);

            final double curHaClose = (model.getWeight(i) + model.getMax(i) + model.getMin(i) + model.getY(i)) * 0.25;
            final double curHaOpen = 0.5 * (prevHaOpen + prevHaClose);

            final double curHaHigh = Math.max(model.getMax(i), Math.max(curHaOpen, curHaClose));
            final double curHaLow = Math.min(model.getMin(i), Math.min(curHaOpen, curHaClose));

            haOpen[i] = curHaOpen;
            haClose[i] = curHaClose;
            haHigh[i] = curHaHigh;
            haLow[i] = curHaLow;

            prevHaOpen = curHaOpen;
            prevHaClose = curHaClose;
        }
    }

    private void ensureCapacity(int n) {
        if (xValues == null || xValues.length < n) {
            xValues = new double[n];
            haOpen = new double[n];
            haClose = new double[n];
            haHigh = new double[n];
            haLow = new double[n];
        }
    }
}
