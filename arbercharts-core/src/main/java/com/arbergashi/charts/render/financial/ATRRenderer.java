package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
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
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class ATRRenderer extends BaseRenderer {

    private static final int DEFAULT_PERIOD = 14;
    private static final String KEY_PERIOD = "Chart.atr.period";

    private final double[] pxA = new double[2];
    private final double[] pxB = new double[2];

    private transient float[] pathX;
    private transient float[] pathY;
    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];

    // Cached indicator arrays
    private transient ChartModel cachedModel;
    private transient int cachedPointCount;
    private transient int cachedPeriod = -1;
    private double[] xValues = new double[0];
    private double[] atrValues = new double[0];
    private final double[] preferredRange = new double[2];
    private boolean preferredRangeValid;
    private int period = ChartAssets.getInt(KEY_PERIOD, DEFAULT_PERIOD);

    public ATRRenderer() {
        super("atr");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        final int p = getResolvedPeriod();
        if (n <= p) return;

        ensureCache(model, p);
        if (cachedPointCount <= 0) return;

        final IndicatorRendererSupport.IndexRange range = IndicatorRendererSupport.visibleRange(context, cachedPointCount, 2);
        final int start = range.getStart();
        final int endExclusive = range.getEndExclusive();
        if (endExclusive <= start) return;

        final ChartTheme theme = getResolvedTheme(context);
        final ArberColor atrColor = theme.getAccentColor();

        // Draw ATR line only for visible slice
        int count = endExclusive - start;
        ensurePathCapacity(count);
        for (int i = start, pIndex = 0; i < endExclusive; i++, pIndex++) {
            context.mapToPixel(xValues[i], atrValues[i], pxA);
            pathX[pIndex] = (float) pxA[0];
            pathY[pIndex] = (float) pxA[1];
        }

        canvas.setColor(atrColor);
        canvas.setStroke(ChartScale.scale(2.5f));
        canvas.drawPolyline(pathX, pathY, count);

        // Draw zero line for reference (clip-aware)
        context.mapToPixel(xValues[start], 0, pxA);
        context.mapToPixel(xValues[endExclusive - 1], 0, pxB);
        canvas.setColor(getResolvedTheme(context).getGridColor());
        canvas.setStroke(ChartScale.scale(1.0f));
        lineX[0] = (float) pxA[0];
        lineY[0] = (float) pxA[1];
        lineX[1] = (float) pxB[0];
        lineY[1] = (float) pxB[1];
        canvas.drawPolyline(lineX, lineY, 2);
    }

    private int getResolvedPeriod() {
        final int configured = Math.max(2, ChartAssets.getInt(KEY_PERIOD, period));
        if (configured != period) {
            period = configured;
            cachedModel = null;
            cachedPointCount = 0;
            cachedPeriod = -1;
            preferredRangeValid = false;
        }
        return period;
    }

    private void ensurePathCapacity(int count) {
        if (pathX == null || pathX.length < count) {
            pathX = new float[count];
            pathY = new float[count];
        }
    }

    private void ensureCache(ChartModel model, int period) {
        final int n = model.getPointCount();
        if (n <= period) {
            cachedModel = model;
            cachedPointCount = 0;
            cachedPeriod = period;
            preferredRangeValid = false;
            return;
        }

        if (cachedModel == model && cachedPointCount == n && cachedPeriod == period) {
            return;
        }

        cachedModel = model;
        cachedPointCount = n;
        cachedPeriod = period;
        preferredRangeValid = false;

        if (xValues.length < n) {
            xValues = new double[n];
            atrValues = new double[n];
        }

        for (int i = 0; i < n; i++) {
            xValues[i] = model.getX(i);
        }

        // ATR calculation via Wilder smoothing.
        // ATR is defined from `period` onward; fill early part with first valid result.
        double atr = 0.0;

        // Initial ATR: average of first `period` true ranges (computed from i=1..period)
        for (int i = 1; i <= period; i++) {
            final double high = model.getMax(i);
            final double low = model.getMin(i);
            final double prevClose = model.getY(i - 1);

            final double tr1 = high - low;
            final double tr2 = Math.abs(high - prevClose);
            final double tr3 = Math.abs(low - prevClose);
            final double tr = Math.max(tr1, Math.max(tr2, tr3));
            atr += tr;
        }
        atr /= period;

        // Fill until `period` with first ATR to keep line stable.
        for (int i = 0; i <= period; i++) {
            atrValues[i] = atr;
        }

        for (int i = period + 1; i < n; i++) {
            final double high = model.getMax(i);
            final double low = model.getMin(i);
            final double prevClose = model.getY(i - 1);

            final double tr1 = high - low;
            final double tr2 = Math.abs(high - prevClose);
            final double tr3 = Math.abs(low - prevClose);
            final double tr = Math.max(tr1, Math.max(tr2, tr3));

            atr = ((atr * (period - 1)) + tr) / period;
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
        ensureCache(model, getResolvedPeriod());
        return preferredRangeValid ? preferredRange : null;
    }
}
