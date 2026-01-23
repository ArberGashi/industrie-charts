package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * <h1>ATRRenderer - Average True Range</h1>
 *
 * <p>Professional ATR indicator renderer for volatility measurement.
 * ATR measures market volatility by decomposing the entire range of an asset price.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>ATR Line:</b> 14-period average true range (default)</li>
 *   <li><b>Volatility Bands:</b> Optional bands at ±1 ATR</li>
 *   <li><b>Smooth Rendering:</b> Uses Wilder's smoothing method</li>
 *   <li><b>Zero-Allocation:</b> Optimized for performance</li>
 * </ul>
 *
 * <h2>Data Requirements:</h2>
 * <p>Requires OHLC data with min (low), max (high), and previous close.</p>
 *
 * <h2>Calculation:</h2>
 * <pre>
 * True Range = max(High - Low, |High - PrevClose|, |Low - PrevClose|)
 * ATR = Wilder's Smoothing of True Range over 14 periods
 * </pre>
 *
 * <h2>Interpretation:</h2>
 * <ul>
 *   <li>High ATR = High volatility (trending market)</li>
 *   <li>Low ATR = Low volatility (consolidation)</li>
 *   <li>ATR does not indicate direction, only volatility</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class ATRRenderer extends BaseRenderer {

    private static final int DEFAULT_PERIOD = 14;

    private final double[] pxA = new double[2];
    private final double[] pxB = new double[2];

    private final Path2D atrPath = new Path2D.Double();

    // Cached indicator arrays
    private transient ChartModel cachedModel;
    private transient int cachedPointCount;
    private double[] xValues = new double[0];
    private double[] atrValues = new double[0];
    private final double[] preferredRange = new double[2];
    private boolean preferredRangeValid;

    public ATRRenderer() {
        super("atr");
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n <= DEFAULT_PERIOD) return;

        ensureCache(model);
        if (cachedPointCount <= 0) return;

        final IndicatorRendererSupport.IndexRange range = IndicatorRendererSupport.visibleRange(g, context, cachedPointCount, 2);
        final int start = range.start();
        final int endExclusive = range.endExclusive();
        if (endExclusive <= start) return;

        final ChartTheme theme = resolveTheme(context);
        final Color atrColor = theme.getAccentColor();

        // Draw ATR line only for visible slice
        atrPath.reset();
        context.mapToPixel(xValues[start], atrValues[start], pxA);
        atrPath.moveTo(pxA[0], pxA[1]);
        for (int i = start + 1; i < endExclusive; i++) {
            context.mapToPixel(xValues[i], atrValues[i], pxA);
            atrPath.lineTo(pxA[0], pxA[1]);
        }

        g.setColor(atrColor);
        g.setStroke(getCachedStroke(ChartScale.scale(2.5f)));
        g.draw(atrPath);

        // Draw zero line for reference (clip-aware)
        context.mapToPixel(xValues[start], 0, pxA);
        context.mapToPixel(xValues[endExclusive - 1], 0, pxB);
        g.setColor(resolveTheme(context).getGridColor());
        g.setStroke(getCachedStroke(ChartScale.scale(1.0f)));
        g.draw(getLine(pxA[0], pxA[1], pxB[0], pxB[1]));
    }

    private void ensureCache(ChartModel model) {
        final int n = model.getPointCount();
        if (n <= DEFAULT_PERIOD) {
            cachedModel = model;
            cachedPointCount = 0;
            preferredRangeValid = false;
            return;
        }

        if (cachedModel == model && cachedPointCount == n) {
            return;
        }

        cachedModel = model;
        cachedPointCount = n;
        preferredRangeValid = false;

        if (xValues.length < n) {
            xValues = new double[n];
            atrValues = new double[n];
        }

        for (int i = 0; i < n; i++) {
            xValues[i] = model.getX(i);
        }

        // ATR calculation via Wilder smoothing.
        // ATR is defined from DEFAULT_PERIOD onward; fill early part with first valid result.
        double atr = 0.0;

        // Initial ATR: average of first DEFAULT_PERIOD true ranges (computed from i=1..DEFAULT_PERIOD)
        for (int i = 1; i <= DEFAULT_PERIOD; i++) {
            final double high = model.getMax(i);
            final double low = model.getMin(i);
            final double prevClose = model.getY(i - 1);

            final double tr1 = high - low;
            final double tr2 = Math.abs(high - prevClose);
            final double tr3 = Math.abs(low - prevClose);
            final double tr = Math.max(tr1, Math.max(tr2, tr3));
            atr += tr;
        }
        atr /= DEFAULT_PERIOD;

        // Fill until DEFAULT_PERIOD with first ATR to keep line stable.
        for (int i = 0; i <= DEFAULT_PERIOD; i++) {
            atrValues[i] = atr;
        }

        for (int i = DEFAULT_PERIOD + 1; i < n; i++) {
            final double high = model.getMax(i);
            final double low = model.getMin(i);
            final double prevClose = model.getY(i - 1);

            final double tr1 = high - low;
            final double tr2 = Math.abs(high - prevClose);
            final double tr3 = Math.abs(low - prevClose);
            final double tr = Math.max(tr1, Math.max(tr2, tr3));

            atr = ((atr * (DEFAULT_PERIOD - 1)) + tr) / DEFAULT_PERIOD;
            atrValues[i] = atr;
        }

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            double v = atrValues[i];
            if (!Double.isFinite(v)) continue;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        if (min != Double.POSITIVE_INFINITY && max != Double.NEGATIVE_INFINITY) {
            min = Math.min(0.0, min);
            if (min == max) {
                min -= 1.0;
                max += 1.0;
            }
            preferredRange[0] = min;
            preferredRange[1] = max;
            preferredRangeValid = true;
        }
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        ensureCache(model);
        return preferredRangeValid ? preferredRange : null;
    }
}
