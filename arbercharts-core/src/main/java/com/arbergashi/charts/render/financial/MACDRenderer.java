package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Professional MACD indicator renderer.
 * This implementation is zero-allocation and works directly on primitive data.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class MACDRenderer extends BaseRenderer {

    private final double[] pxA = new double[2];
    private final double[] pxB = new double[2];

    private final Path2D macdPath = new Path2D.Double();
    private final Path2D signalPath = new Path2D.Double();

    // Cached computed series
    private long lastModelStamp = -1;
    private int cachedPointCount;
    private double[] xValues;
    private double[] yValues; // Source y-values
    private double[] macdValues;
    private double[] signalValues;
    private double[] histValues;
    private double[] emaFast;
    private double[] emaSlow;
    private final double[] preferredRange = new double[2];
    private boolean preferredRangeValid;

    public MACDRenderer() {
        super("macd");
    }

    private static void computeEMA(double[] values, int period, double[] out) {
        final int n = values.length;
        if (n < period) return;
        final double alpha = 2.0 / (period + 1.0);

        double sum = 0.0;
        for (int i = 0; i < period; i++) sum += values[i];
        double ema = sum / period;

        for (int i = 0; i < period - 1; i++) out[i] = Double.NaN;
        out[period - 1] = ema;

        for (int i = period; i < n; i++) {
            ema = (values[i] - ema) * alpha + ema;
            out[i] = ema;
        }
    }

    private static void computeEMAOnArray(double[] src, int start, int end, int period, double[] out) {
        if (end - start < period) return;
        final double alpha = 2.0 / (period + 1.0);

        double sum = 0.0;
        for (int i = start; i < start + period; i++) sum += src[i];
        double ema = sum / period;

        for (int i = 0; i < start + period - 1; i++) out[i] = Double.NaN;
        out[start + period - 1] = ema;

        for (int i = start + period; i < end; i++) {
            ema = (src[i] - ema) * alpha + ema;
            out[i] = ema;
        }
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        ensureCache(model);
        if (cachedPointCount <= 0) return;

        final Rectangle2D viewBounds = g.getClipBounds() != null ? g.getClipBounds() : context.plotBounds();
        final double leftX = viewBounds.getMinX();
        final double rightX = viewBounds.getMaxX();

        final double w = context.plotBounds().getWidth();
        final double barWidth = (w / (double) cachedPointCount) * 0.6;

        final Color macdColor = themeAccent(context);
        final Color bullishColor = themeBullish(context);
        final Color bearishColor = themeBearish(context);
        final Color signalColor = ColorUtils.adjustBrightness(macdColor, 1.2f);
        final Color bullishHist = ColorUtils.withAlpha(bullishColor, 0.5f);
        final Color bearishHist = ColorUtils.withAlpha(bearishColor, 0.5f);

        // Zero line
        context.mapToPixel(xValues[0], 0, pxA);
        context.mapToPixel(xValues[cachedPointCount - 1], 0, pxB);
        g.setColor(themeGrid(context));
        g.setStroke(getCachedStroke(ChartScale.scale(1.0f)));
        g.draw(getLine(pxA[0], pxA[1], pxB[0], pxB[1]));

        // Histogram, MACD, and Signal lines
        drawHistogram(g, context, barWidth, leftX, rightX, bullishHist, bearishHist);
        drawLines(g, context, macdColor, signalColor);
    }

    private void drawHistogram(Graphics2D g, PlotContext context, double barWidth, double leftX, double rightX, Color bullish, Color bearish) {
        for (int i = 0; i < cachedPointCount; i++) {
            final double histogram = histValues[i];
            if (!Double.isFinite(histogram)) continue;

            context.mapToPixel(xValues[i], histogram, pxA);
            context.mapToPixel(xValues[i], 0, pxB);

            final double x = pxA[0] - barWidth / 2.0;
            if (x + barWidth < leftX || x > rightX) continue;

            final double topY = pxA[1];
            final double baseY = pxB[1];
            final double barHeight = Math.abs(baseY - topY);
            final double y = Math.min(topY, baseY);

            g.setColor(histogram >= 0 ? bullish : bearish);
            g.fill(getRect(x, y, barWidth, barHeight));
        }
    }

    private void drawLines(Graphics2D g, PlotContext context, Color macdColor, Color signalColor) {
        macdPath.reset();
        signalPath.reset();
        boolean macdMoved = false;
        boolean signalMoved = false;

        for (int i = 0; i < cachedPointCount; i++) {
            double x = xValues[i];

            if (Double.isFinite(macdValues[i])) {
                context.mapToPixel(x, macdValues[i], pxA);
                if (!macdMoved) {
                    macdPath.moveTo(pxA[0], pxA[1]);
                    macdMoved = true;
                } else {
                    macdPath.lineTo(pxA[0], pxA[1]);
                }
            }

            if (Double.isFinite(signalValues[i])) {
                context.mapToPixel(x, signalValues[i], pxB);
                if (!signalMoved) {
                    signalPath.moveTo(pxB[0], pxB[1]);
                    signalMoved = true;
                } else {
                    signalPath.lineTo(pxB[0], pxB[1]);
                }
            }
        }

        g.setStroke(getCachedStroke(ChartScale.scale(2.0f)));
        g.setColor(macdColor);
        g.draw(macdPath);

        g.setStroke(getCachedStroke(ChartScale.scale(1.5f)));
        g.setColor(signalColor);
        g.draw(signalPath);
    }

    private void ensureCache(ChartModel model) {
        final long stamp = model.getUpdateStamp();
        if (stamp == lastModelStamp && cachedPointCount == model.getPointCount()) {
            return;
        }
        lastModelStamp = stamp;
        cachedPointCount = model.getPointCount();
        final int n = cachedPointCount;
        preferredRangeValid = false;

        if (n < 35) { // EMA(26) + EMA(9)
            cachedPointCount = 0;
            return;
        }

        ensureCapacity(n);

        for (int i = 0; i < n; i++) {
            xValues[i] = model.getX(i);
            yValues[i] = model.getY(i);
        }

        computeEMA(yValues, 12, emaFast);
        computeEMA(yValues, 26, emaSlow);

        for (int i = 0; i < 25; i++) macdValues[i] = Double.NaN;
        for (int i = 25; i < n; i++) macdValues[i] = emaFast[i] - emaSlow[i];

        computeEMAOnArray(macdValues, 25, n, 9, signalValues);

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            histValues[i] = macdValues[i] - signalValues[i];
            if (Double.isFinite(macdValues[i])) {
                min = Math.min(min, macdValues[i]);
                max = Math.max(max, macdValues[i]);
            }
            if (Double.isFinite(signalValues[i])) {
                min = Math.min(min, signalValues[i]);
                max = Math.max(max, signalValues[i]);
            }
            if (Double.isFinite(histValues[i])) {
                min = Math.min(min, histValues[i]);
                max = Math.max(max, histValues[i]);
            }
        }
        if (min != Double.POSITIVE_INFINITY && max != Double.NEGATIVE_INFINITY) {
            min = Math.min(min, 0.0);
            max = Math.max(max, 0.0);
            if (min == max) {
                min -= 1.0;
                max += 1.0;
            }
            preferredRange[0] = min;
            preferredRange[1] = max;
            preferredRangeValid = true;
        }
    }

    private void ensureCapacity(int n) {
        if (xValues == null || xValues.length < n) {
            xValues = new double[n];
            yValues = new double[n];
            macdValues = new double[n];
            signalValues = new double[n];
            histValues = new double[n];
            emaFast = new double[n];
            emaSlow = new double[n];
        }
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        ensureCache(model);
        return preferredRangeValid ? preferredRange : null;
    }
}
