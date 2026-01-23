package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * <h1>ADXRenderer - Average Directional Index</h1>
 *
 * <p>Professional ADX indicator renderer for trend strength measurement.
 * Displays ADX line with +DI and -DI directional indicators.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>ADX Line:</b> Measures trend strength (0-100 scale)</li>
 *   <li><b>+DI Line:</b> Positive directional indicator</li>
 *   <li><b>-DI Line:</b> Negative directional indicator</li>
 *   <li><b>Threshold Zones:</b> &lt; 25 (weak), &gt; 25 (strong), &gt; 50 (very strong)</li>
 * </ul>
 *
 * <h2>Data Requirements:</h2>
 * <p>Requires OHLC data with min (low) and max (high) fields.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class ADXRenderer extends BaseRenderer {

    private static final int DEFAULT_PERIOD = 14;
    private static final double WEAK_THRESHOLD = 25.0;
    private static final double STRONG_THRESHOLD = 50.0;
    private static final double[] RANGE_0_100 = {0.0, 100.0};

    private final double[] pxA = new double[2];
    private final double[] pxB = new double[2];

    private final Path2D adxPath = new Path2D.Double();
    private final Path2D plusPath = new Path2D.Double();
    private final Path2D minusPath = new Path2D.Double();

    // Cached computed series
    private transient ChartModel cachedModel;
    private transient int cachedPointCount;

    private transient double[] xValues;
    private transient double[] adxValues;
    private transient double[] plusDIValues;
    private transient double[] minusDIValues;

    // Scratch arrays
    private transient double[] tr;
    private transient double[] plusDM;
    private transient double[] minusDM;
    private transient double[] dx;

    public ADXRenderer() {
        super("adx");
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < DEFAULT_PERIOD * 2 + 2) return;

        ensureCache(model);
        if (cachedPointCount <= 0 || xValues == null || adxValues == null) return;

        final IndicatorRendererSupport.IndexRange range = IndicatorRendererSupport.visibleRange(g, context, cachedPointCount, 2);
        final int start = range.start();
        final int endExclusive = range.endExclusive();
        if (endExclusive - start < 2) return;

        final ChartTheme theme = resolveTheme(context);
        final Color adxColor = theme.getAccentColor();
        final Color plusDIColor = theme.getBullishColor();
        final Color minusDIColor = theme.getBearishColor();
        final Color weakZoneColor = ColorUtils.withAlpha(theme.getGridColor(), 0.10f);

        // Weak zone (0..25)
        context.mapToPixel(xValues[start], 0, pxA);
        context.mapToPixel(xValues[endExclusive - 1], 0, pxB);
        final double x0 = pxA[0];
        final double x1 = pxB[0];
        final double y0 = pxA[1];

        context.mapToPixel(xValues[start], WEAK_THRESHOLD, pxA);
        final double y25 = pxA[1];

        g.setColor(weakZoneColor);
        g.fill(getRect(Math.min(x0, x1), Math.min(y0, y25), Math.abs(x1 - x0), Math.abs(y25 - y0)));

        // Threshold lines
        g.setColor(resolveTheme(context).getGridColor());
        g.setStroke(getCachedStroke(ChartScale.scale(1.0f)));
        g.draw(getLine(x0, y25, x1, y25));
        context.mapToPixel(xValues[start], STRONG_THRESHOLD, pxA);
        context.mapToPixel(xValues[endExclusive - 1], STRONG_THRESHOLD, pxB);
        g.draw(getLine(pxA[0], pxA[1], pxB[0], pxB[1]));

        // -DI, +DI, ADX (foreground)
        drawSeriesPath(g, minusPath, xValues, minusDIValues, start, endExclusive, context, minusDIColor, 1.5f);
        drawSeriesPath(g, plusPath, xValues, plusDIValues, start, endExclusive, context, plusDIColor, 1.5f);
        drawSeriesPath(g, adxPath, xValues, adxValues, start, endExclusive, context, adxColor, 2.5f);
    }

    private void drawSeriesPath(Graphics2D g,
                                Path2D path,
                                double[] xs,
                                double[] ys,
                                int start,
                                int endExclusive,
                                PlotContext context,
                                Color color,
                                float width) {
        path.reset();
        context.mapToPixel(xs[start], ys[start], pxA);
        path.moveTo(pxA[0], pxA[1]);
        for (int i = start + 1; i < endExclusive; i++) {
            context.mapToPixel(xs[i], ys[i], pxA);
            path.lineTo(pxA[0], pxA[1]);
        }
        g.setColor(color);
        g.setStroke(getCachedStroke(ChartScale.scale(width)));
        g.draw(path);
    }

    private void ensureCache(ChartModel model) {
        final int n = model.getPointCount();
        if (n < DEFAULT_PERIOD * 2 + 2) {
            cachedModel = model;
            cachedPointCount = 0;
            return;
        }

        if (cachedModel == model && cachedPointCount == n && xValues != null && adxValues != null) {
            return;
        }

        cachedModel = model;
        cachedPointCount = n;

        ensureCapacity(n);

        for (int i = 0; i < n; i++) {
            xValues[i] = model.getX(i);
        }

        // Compute TR, +DM, -DM for i=1..n-1
        tr[0] = 0.0;
        plusDM[0] = 0.0;
        minusDM[0] = 0.0;
        for (int i = 1; i < n; i++) {
            final double high = model.getMax(i);
            final double low = model.getMin(i);
            final double prevHigh = model.getMax(i - 1);
            final double prevLow = model.getMin(i - 1);
            final double prevClose = model.getY(i - 1);

            final double tr1 = high - low;
            final double tr2 = Math.abs(high - prevClose);
            final double tr3 = Math.abs(low - prevClose);
            tr[i] = Math.max(tr1, Math.max(tr2, tr3));

            final double upMove = high - prevHigh;
            final double downMove = prevLow - low;

            plusDM[i] = (upMove > downMove && upMove > 0) ? upMove : 0.0;
            minusDM[i] = (downMove > upMove && downMove > 0) ? downMove : 0.0;
        }

        // Wilder smoothing for TR, +DM, -DM
        double smTR = 0.0;
        double smPlus = 0.0;
        double smMinus = 0.0;
        for (int i = 1; i <= DEFAULT_PERIOD; i++) {
            smTR += tr[i];
            smPlus += plusDM[i];
            smMinus += minusDM[i];
        }

        // For stability, prefill with zeros
        for (int i = 0; i < n; i++) {
            plusDIValues[i] = 0.0;
            minusDIValues[i] = 0.0;
            dx[i] = 0.0;
            adxValues[i] = 0.0;
        }

        // DX starts at index DEFAULT_PERIOD+1 (because for i we used current i, which depends on TR[i])
        // We'll compute DI/DX for i = DEFAULT_PERIOD+1 .. n-1
        for (int i = DEFAULT_PERIOD + 1; i < n; i++) {
            smTR = smTR - (smTR / DEFAULT_PERIOD) + tr[i];
            smPlus = smPlus - (smPlus / DEFAULT_PERIOD) + plusDM[i];
            smMinus = smMinus - (smMinus / DEFAULT_PERIOD) + minusDM[i];

            final double pdi = (smTR == 0.0) ? 0.0 : (100.0 * smPlus / smTR);
            final double mdi = (smTR == 0.0) ? 0.0 : (100.0 * smMinus / smTR);
            plusDIValues[i] = pdi;
            minusDIValues[i] = mdi;

            final double sum = pdi + mdi;
            final double dxVal = (sum == 0.0) ? 0.0 : (100.0 * Math.abs(pdi - mdi) / sum);
            dx[i] = dxVal;
        }

        // Initial ADX is average of first DEFAULT_PERIOD DX values (starting at index DEFAULT_PERIOD+1)
        final int adxStartIndex = (DEFAULT_PERIOD + 1) + (DEFAULT_PERIOD - 1);

        double adx = 0.0;
        for (int i = DEFAULT_PERIOD + 1; i <= adxStartIndex; i++) {
            adx += dx[i];
        }
        adx /= DEFAULT_PERIOD;

        // Fill early values for visual continuity
        for (int i = 0; i <= adxStartIndex; i++) {
            adxValues[i] = adx;
        }

        for (int i = adxStartIndex + 1; i < n; i++) {
            adx = ((adx * (DEFAULT_PERIOD - 1)) + dx[i]) / DEFAULT_PERIOD;
            adxValues[i] = adx;
        }
    }

    private void ensureCapacity(int n) {
        if (xValues == null || xValues.length < n) {
            xValues = new double[n];
            adxValues = new double[n];
            plusDIValues = new double[n];
            minusDIValues = new double[n];

            tr = new double[n];
            plusDM = new double[n];
            minusDM = new double[n];
            dx = new double[n];
        }
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        return RANGE_0_100;
    }
}
