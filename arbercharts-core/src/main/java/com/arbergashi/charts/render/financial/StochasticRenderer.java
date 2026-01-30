package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * <h1>StochasticRenderer - Stochastic Oscillator (%K and %D)</h1>
 *
 * <p>Professional Stochastic oscillator renderer for momentum analysis.
 * Displays %K (fast) and %D (slow) lines with overbought/oversold zones.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>%K Line:</b> Fast stochastic (blue)</li>
 *   <li><b>%D Line:</b> Slow stochastic - 3-period SMA of %K (red)</li>
 *   <li><b>Overbought:</b> Above 80 zone</li>
 *   <li><b>Oversold:</b> Below 20 zone</li>
 *   <li><b>Range:</b> 0-100 scale</li>
 * </ul>
 *
 * <h2>Data Mapping:</h2>
 * <pre>
 * ChartPoint fields:
 *   x       → Time/Index
 *   y       → %K value (fast stochastic)
 *   weight  → %D value (slow stochastic)
 * </pre>
 *
 * <h2>Calculation:</h2>
 * <pre>
 * %K = ((Close - Low14) / (High14 - Low14)) * 100
 * %D = 3-period SMA of %K
 * </pre>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class StochasticRenderer extends BaseRenderer {

    private static final int DEFAULT_K_PERIOD = 14;
    private static final int DEFAULT_D_PERIOD = 3;
    private static final double OVERBOUGHT = 80.0;
    private static final double OVERSOLD = 20.0;
    private static final double[] RANGE_0_100 = {0.0, 100.0};

    private final double[] pxA = new double[2];
    private final double[] pxB = new double[2];
    private final double[] pxC = new double[2];
    private final double[] pxD = new double[2];

    private final float[] zoneX = new float[4];
    private final float[] zoneY = new float[4];
    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];
    private transient float[] pathX;
    private transient float[] pathY;

    // Cached indicator arrays
    private transient ChartModel cachedModel;
    private transient int cachedPointCount;
    private transient int cachedKPeriod = DEFAULT_K_PERIOD;
    private transient int cachedDPeriod = DEFAULT_D_PERIOD;
    private double[] xValues = new double[0];
    private double[] kValues = new double[0];
    private double[] dValues = new double[0];

    // Sliding-window indices for O(n) high/low
    private transient int[] hiDeque = new int[0];
    private transient int[] loDeque = new int[0];

    public StochasticRenderer() {
        super("stochastic");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        final boolean dataLooksReady = looksLikeStochastic(model);
        if (!dataLooksReady) {
            ensureCache(model, cachedKPeriod, cachedDPeriod);
            if (cachedPointCount <= 0) return;
        }

        // Use the correct point count (may differ if cache decides to short-circuit).
        final int pointCount = dataLooksReady ? n : cachedPointCount;
        final IndicatorRendererSupport.IndexRange range = IndicatorRendererSupport.visibleRange(context, pointCount, 2);
        final int start = range.getStart();
        final int endExclusive = range.getEndExclusive();
        if (endExclusive <= start) return;

        final ChartTheme theme = getResolvedTheme(context);
        final ArberColor kColor = theme.getAccentColor();
        final ArberColor dColor = ColorRegistry.adjustBrightness(kColor, 1.3f);
        final ArberColor overboughtColor = ColorRegistry.applyAlpha(theme.getBearishColor(), 0.12f);
        final ArberColor oversoldColor = ColorRegistry.applyAlpha(theme.getBullishColor(), 0.12f);

        final double firstX = (dataLooksReady ? model.getX(0) : xValues[start]);
        final double lastX = (dataLooksReady ? model.getX(n - 1) : xValues[endExclusive - 1]);

        // Zones
        context.mapToPixel(firstX, 100, pxA);
        context.mapToPixel(lastX, 100, pxB);
        context.mapToPixel(firstX, OVERBOUGHT, pxC);
        context.mapToPixel(lastX, OVERBOUGHT, pxD);

        zoneX[0] = (float) pxA[0];
        zoneY[0] = (float) pxA[1];
        zoneX[1] = (float) pxB[0];
        zoneY[1] = (float) pxB[1];
        zoneX[2] = (float) pxD[0];
        zoneY[2] = (float) pxD[1];
        zoneX[3] = (float) pxC[0];
        zoneY[3] = (float) pxC[1];
        canvas.setColor(overboughtColor);
        canvas.fillPolygon(zoneX, zoneY, 4);

        context.mapToPixel(firstX, 0, pxA);
        context.mapToPixel(lastX, 0, pxB);
        context.mapToPixel(firstX, OVERSOLD, pxC);
        context.mapToPixel(lastX, OVERSOLD, pxD);

        zoneX[0] = (float) pxA[0];
        zoneY[0] = (float) pxA[1];
        zoneX[1] = (float) pxB[0];
        zoneY[1] = (float) pxB[1];
        zoneX[2] = (float) pxD[0];
        zoneY[2] = (float) pxD[1];
        zoneX[3] = (float) pxC[0];
        zoneY[3] = (float) pxC[1];
        canvas.setColor(oversoldColor);
        canvas.fillPolygon(zoneX, zoneY, 4);

        // Reference lines (20/80)
        canvas.setColor(theme.getGridColor());
        canvas.setStroke(ChartScale.scale(1.0f));

        context.mapToPixel(firstX, OVERSOLD, pxA);
        context.mapToPixel(lastX, OVERSOLD, pxB);
        lineX[0] = (float) pxA[0];
        lineY[0] = (float) pxA[1];
        lineX[1] = (float) pxB[0];
        lineY[1] = (float) pxB[1];
        canvas.drawPolyline(lineX, lineY, 2);

        context.mapToPixel(firstX, OVERBOUGHT, pxA);
        context.mapToPixel(lastX, OVERBOUGHT, pxB);
        lineX[0] = (float) pxA[0];
        lineY[0] = (float) pxA[1];
        lineX[1] = (float) pxB[0];
        lineY[1] = (float) pxB[1];
        canvas.drawPolyline(lineX, lineY, 2);

        // %D line (visible slice)
        int dCount = 0;
        ensurePathCapacity(endExclusive - start);
        if (dataLooksReady) {
            for (int i = start; i < endExclusive; i++) {
                double x = model.getX(i);
                context.mapToPixel(x, model.getWeight(i), pxA);
                pathX[dCount] = (float) pxA[0];
                pathY[dCount] = (float) pxA[1];
                dCount++;
            }
        } else {
            for (int i = start; i < endExclusive; i++) {
                context.mapToPixel(xValues[i], dValues[i], pxA);
                pathX[dCount] = (float) pxA[0];
                pathY[dCount] = (float) pxA[1];
                dCount++;
            }
        }
        if (dCount >= 2) {
            canvas.setColor(dColor);
            canvas.setStroke(ChartScale.scale(2.0f));
            canvas.drawPolyline(pathX, pathY, dCount);
        }

        // %K line (visible slice)
        int kCount = 0;
        ensurePathCapacity(endExclusive - start);
        if (dataLooksReady) {
            for (int i = start; i < endExclusive; i++) {
                double x = model.getX(i);
                context.mapToPixel(x, model.getY(i), pxA);
                pathX[kCount] = (float) pxA[0];
                pathY[kCount] = (float) pxA[1];
                kCount++;
            }
        } else {
            for (int i = start; i < endExclusive; i++) {
                context.mapToPixel(xValues[i], kValues[i], pxA);
                pathX[kCount] = (float) pxA[0];
                pathY[kCount] = (float) pxA[1];
                kCount++;
            }
        }
        if (kCount >= 2) {
            canvas.setColor(kColor);
            canvas.setStroke(ChartScale.scale(2.5f));
            canvas.drawPolyline(pathX, pathY, kCount);
        }
    }

    private void ensurePathCapacity(int count) {
        if (pathX == null || pathX.length < count) {
            pathX = new float[count];
            pathY = new float[count];
        }
    }

    private boolean looksLikeStochastic(ChartModel model) {
        final int n = model.getPointCount();
        // Heuristic: If y and weight are within 0..100, assume data is already stochastic.
        for (int i = 0; i < n; i++) {
            double y = model.getY(i);
            double w = model.getWeight(i);
            if (!(y >= 0.0 && y <= 100.0 && w >= 0.0 && w <= 100.0)) return false;
        }
        return true;
    }

    private void ensureCache(ChartModel model, int kPeriod, int dPeriod) {
        final int n = model.getPointCount();
        // Need at least one full %K window and one full %D window.
        if (n <= kPeriod) {
            cachedModel = model;
            cachedPointCount = 0;
            return;
        }

        if (cachedModel == model && cachedPointCount == n && cachedKPeriod == kPeriod && cachedDPeriod == dPeriod) {
            return;
        }

        cachedModel = model;
        cachedPointCount = n;
        cachedKPeriod = kPeriod;
        cachedDPeriod = dPeriod;

        if (xValues.length < n) {
            xValues = new double[n];
            kValues = new double[n];
            dValues = new double[n];
        }
        if (hiDeque.length < n) {
            hiDeque = new int[n];
            loDeque = new int[n];
        }

        for (int i = 0; i < n; i++) {
            xValues[i] = model.getX(i);
        }

        // Compute rolling high/low in O(n) with index deques.
        int hiHead = 0, hiTail = 0;
        int loHead = 0, loTail = 0;

        // Warmup to first window
        for (int i = 0; i < kPeriod - 1; i++) {
            kValues[i] = Double.NaN;

            final double hi = model.getMax(i);
            while (hiTail > hiHead && model.getMax(hiDeque[hiTail - 1]) <= hi) hiTail--;
            hiDeque[hiTail++] = i;

            final double lo = model.getMin(i);
            while (loTail > loHead && model.getMin(loDeque[loTail - 1]) >= lo) loTail--;
            loDeque[loTail++] = i;
        }

        for (int i = kPeriod - 1; i < n; i++) {
            final double hi = model.getMax(i);
            while (hiTail > hiHead && model.getMax(hiDeque[hiTail - 1]) <= hi) hiTail--;
            hiDeque[hiTail++] = i;

            final double lo = model.getMin(i);
            while (loTail > loHead && model.getMin(loDeque[loTail - 1]) >= lo) loTail--;
            loDeque[loTail++] = i;

            final int windowStart = i - kPeriod + 1;

            while (hiTail > hiHead && hiDeque[hiHead] < windowStart) hiHead++;
            while (loTail > loHead && loDeque[loHead] < windowStart) loHead++;

            final double windowHigh = model.getMax(hiDeque[hiHead]);
            final double windowLow = model.getMin(loDeque[loHead]);

            final double denom = windowHigh - windowLow;
            final double close = model.getY(i);

            double k;
            if (!(denom > 0) || !Double.isFinite(denom)) {
                k = Double.NaN;
            } else {
                k = ((close - windowLow) / denom) * 100.0;
            }

            // Clamp for numerical stability
            if (Double.isFinite(k)) {
                if (k < 0) k = 0;
                else if (k > 100) k = 100;
            }

            kValues[i] = k;
        }

        // Build %D as SMA of %K
        for (int i = 0; i < n; i++) {
            dValues[i] = Double.NaN;
        }

        double sum = 0.0;
        int valid = 0;
        for (int i = 0; i < n; i++) {
            double kv = kValues[i];
            if (Double.isFinite(kv)) {
                sum += kv;
                valid++;
            }

            int outIdx = i - dPeriod;
            if (outIdx >= 0) {
                double out = kValues[outIdx];
                if (Double.isFinite(out)) {
                    sum -= out;
                    valid--;
                }
            }

            if (i >= dPeriod - 1 && valid > 0) {
                dValues[i] = sum / valid;
            }
        }

        // Stabilize initial NaNs for nicer visuals
        double firstK = Double.NaN;
        double firstD = Double.NaN;
        for (int i = 0; i < n; i++) {
            if (!Double.isFinite(firstK) && Double.isFinite(kValues[i])) firstK = kValues[i];
            if (!Double.isFinite(firstD) && Double.isFinite(dValues[i])) firstD = dValues[i];
            if (Double.isFinite(firstK) && Double.isFinite(firstD)) break;
        }
        if (!Double.isFinite(firstK)) firstK = 50.0;
        if (!Double.isFinite(firstD)) firstD = firstK;

        for (int i = 0; i < n; i++) {
            if (!Double.isFinite(kValues[i])) kValues[i] = firstK;
            if (!Double.isFinite(dValues[i])) dValues[i] = firstD;
        }
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        return RANGE_0_100;
    }
}
