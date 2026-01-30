package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
/**
 * <h1>OBVRenderer - On-Balance Volume</h1>
 *
 * <p>Professional OBV indicator renderer for volume-based momentum analysis.
 * Measures buying and selling pressure as a cumulative indicator.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>OBV Line:</b> Cumulative volume indicator</li>
 *   <li><b>Trend Identification:</b> Rising OBV = bullish, falling OBV = bearish</li>
 *   <li><b>Divergence Detection:</b> Visual comparison with price</li>
 *   <li><b>Zero Line:</b> Reference baseline</li>
 * </ul>
 *
 * <h2>Data Requirements:</h2>
 * <p>Requires price (close) and volume data.</p>
 *
 * <h2>Calculation:</h2>
 * <pre>
 * If Close &gt; PrevClose: OBV = PrevOBV + Volume
 * If Close &lt; PrevClose: OBV = PrevOBV - Volume
 * If Close = PrevClose: OBV = PrevOBV
 * </pre>
 *
 * <h2>Interpretation:</h2>
 * <ul>
 *   <li>Rising OBV = Accumulation (bullish)</li>
 *   <li>Falling OBV = Distribution (bearish)</li>
 *   <li>OBV confirms trend if it moves with price</li>
 *   <li>Divergence = potential reversal signal</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class OBVRenderer extends BaseRenderer {

    private final double[] pxA = new double[2];
    private final double[] pxB = new double[2];

    private transient float[] pathX;
    private transient float[] pathY;
    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];
    // Cached indicator arrays
    private transient ChartModel cachedModel;
    private transient int cachedPointCount;
    private transient double[] xValues;
    private transient double[] obvValues;
    private final double[] preferredRange = new double[2];
    private boolean preferredRangeValid;

    public OBVRenderer() {
        super("obv");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < 2) return;

        ensureCache(model);
        if (cachedPointCount <= 0 || xValues == null || obvValues == null) return;

        final IndicatorRendererSupport.IndexRange range = IndicatorRendererSupport.visibleRange(context, cachedPointCount, 2);
        final int start = range.getStart();
        final int endExclusive = range.getEndExclusive();
        if (endExclusive <= start) return;

        final ChartTheme theme = getResolvedTheme(context);
        final ArberColor obvColor = theme.getAccentColor();

        // map baseline y once
        context.mapToPixel(xValues[start], 0, pxA);
        final double baseY = pxA[1];

        // Draw zero line (visible slice)
        context.mapToPixel(xValues[start], 0, pxA);
        context.mapToPixel(xValues[endExclusive - 1], 0, pxB);
        canvas.setColor(getResolvedTheme(context).getGridColor());
        canvas.setStroke(ChartScale.scale(1.0f));
        lineX[0] = (float) pxA[0];
        lineY[0] = (float) pxA[1];
        lineX[1] = (float) pxB[0];
        lineY[1] = (float) pxB[1];
        canvas.drawPolyline(lineX, lineY, 2);

        // Build line + area path (area closed to baseline) for visible slice
        int count = endExclusive - start;
        ensurePathCapacity(count);
        for (int i = start, p = 0; i < endExclusive; i++, p++) {
            context.mapToPixel(xValues[i], obvValues[i], pxB);
            pathX[p] = (float) pxB[0];
            pathY[p] = (float) pxB[1];
        }

        // Draw line (area fills removed in headless core)
        canvas.setColor(obvColor);
        canvas.setStroke(ChartScale.scale(2.5f));
        canvas.drawPolyline(pathX, pathY, count);
    }

    private void ensurePathCapacity(int count) {
        if (pathX == null || pathX.length < count) {
            pathX = new float[count];
            pathY = new float[count];
        }
    }

    /**
     * Calculates OBV from price (y) and volume (weight) data.
     * OBV values are cached in primitive arrays for performance.
     */
    private void ensureCache(ChartModel model) {
        final int n = model.getPointCount();
        if (n < 2) {
            cachedModel = model;
            cachedPointCount = 0;
            preferredRangeValid = false;
            return;
        }

        if (cachedModel == model && cachedPointCount == n && xValues != null && obvValues != null) {
            return;
        }

        cachedModel = model;
        cachedPointCount = n;
        preferredRangeValid = false;

        if (xValues == null || xValues.length < n || obvValues == null || obvValues.length < n) {
            xValues = new double[n];
            obvValues = new double[n];
        }

        // help static analysis; arrays are guaranteed after the allocation block
        final double[] xs = xValues;
        final double[] obvs = obvValues;

        for (int i = 0; i < n; i++) {
            xs[i] = model.getX(i);
        }

        double obv = 0.0;
        obvs[0] = obv;

        for (int i = 1; i < n; i++) {
            double volume = model.getWeight(i);
            double close = model.getY(i);
            double prevClose = model.getY(i - 1);

            if (close > prevClose) obv += volume;
            else if (close < prevClose) obv -= volume;

            obvs[i] = obv;
        }

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            double v = obvValues[i];
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
