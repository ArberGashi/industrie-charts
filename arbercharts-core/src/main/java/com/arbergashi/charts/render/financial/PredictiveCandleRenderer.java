package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.predictive.PredictiveShadowRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * Predictive ghost candle rendered as a pre-data layer for OHLC charts.
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class PredictiveCandleRenderer extends BaseRenderer {
    private static final String KEY_ENABLED = "Chart.financial.ghost.enabled";
    private static final String KEY_ALPHA = "Chart.financial.ghost.alpha";
    private static final String KEY_SIGMA_FACTOR = "Chart.financial.ghost.sigmaFactor";
    private static final String KEY_RANGE_FACTOR = "Chart.financial.ghost.rangeFactor";
    private static final String KEY_MIN_RANGE = "Chart.financial.ghost.minRange";
    private static final String KEY_STROKE = "Chart.financial.ghost.strokeWidth";

    private static final String KEY_ATR_PERIOD = "Chart.atr.period";

    private final PredictiveShadowRenderer shadow;

    private final double[] pxHigh = new double[2];
    private final double[] pxLow = new double[2];
    private final double[] pxOpen = new double[2];
    private final double[] pxClose = new double[2];

    public PredictiveCandleRenderer(PredictiveShadowRenderer shadow) {
        super("predictive_candle");
        this.shadow = shadow;
    }

    @Override
    public String getName() {
        return "Predictive Candle";
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (shadow == null) return;
        if (!ChartAssets.getBoolean(KEY_ENABLED, true)) return;

        final int last = lastFiniteIndex(model);
        if (last < 1) return;

        final double lastX = model.getX(last);
        final double prevX = model.getX(last - 1);
        final double lastClose = model.getY(last);
        if (!finite3(lastX, prevX, lastClose)) return;

        final double step = Math.abs(lastX - prevX) > 0.0 ? (lastX - prevX) : 1.0;
        final double nextX = lastX + step;

        shadow.ensurePredicted(model, context);
        final double predictedClose = shadow.predictedForX(nextX);
        if (!Double.isFinite(predictedClose)) return;

        final double open = lastClose;
        final double close = predictedClose;

        final double atr = getComputedAtr(model, Math.max(2, ChartAssets.getInt(KEY_ATR_PERIOD, 14)));
        final double sigma = shadow.residualStd();
        final double sigmaFactor = Math.max(0.1, ChartAssets.getFloat(KEY_SIGMA_FACTOR, 1.0f));
        final double rangeFactor = Math.max(0.1, ChartAssets.getFloat(KEY_RANGE_FACTOR, 1.0f));
        final double minRange = Math.max(1e-9, ChartAssets.getFloat(KEY_MIN_RANGE, 1e-6f));
        final double sigmaRange = (Double.isFinite(sigma) && sigma > 0.0) ? sigma * shadow.residualScale() * sigmaFactor : 0.0;
        final double range = Math.max(minRange, Math.max(atr, sigmaRange) * rangeFactor);

        final double high = Math.max(open, close) + range;
        final double low = Math.min(open, close) - range;

        if (!finite4(high, low, open, close)) return;

        context.mapToPixel(nextX, high, pxHigh);
        context.mapToPixel(nextX, low, pxLow);
        context.mapToPixel(nextX, open, pxOpen);
        context.mapToPixel(nextX, close, pxClose);

        if (!finite4(pxHigh[0], pxHigh[1], pxLow[0], pxLow[1])) return;

        final double candleX = context.snapPixel(pxOpen[0]);
        final double highY = context.snapPixel(pxHigh[1]);
        final double lowY = context.snapPixel(pxLow[1]);
        final double openY = context.snapPixel(pxOpen[1]);
        final double closeY = context.snapPixel(pxClose[1]);

        final double candleWidth = getResolvedCandleWidth(context, prevX, lastX, lastClose, last + 1);
        if (!(candleWidth > 0.0)) return;

        final ChartTheme theme = getResolvedTheme(context);
        final boolean bullish = close >= open;
        final float alpha = clamp01(ChartAssets.getFloat(KEY_ALPHA, 0.4f));
        final ArberColor base = bullish ? theme.getBullishColor() : theme.getBearishColor();
        final ArberColor fill = ColorRegistry.applyAlpha(base, alpha);
        final ArberColor strokeColor = ColorRegistry.applyAlpha(base, Math.min(1f, alpha + 0.2f));

        final float strokeWidth = ChartScale.scale(ChartAssets.getFloat(KEY_STROKE, 1.2f));

        // Wick
        canvas.setColor(strokeColor);
        canvas.setStroke(strokeWidth);
        float[] wx = { (float) candleX, (float) candleX };
        float[] wy = { (float) highY, (float) lowY };
        canvas.drawPolyline(wx, wy, 2);

        // Body
        final double bodyLeft = candleX - candleWidth * 0.5;
        final double bodyTop = Math.min(openY, closeY);
        final double bodyHeight = Math.max(1.0, Math.abs(closeY - openY));
        canvas.setColor(fill);
        canvas.fillRect((float) bodyLeft, (float) bodyTop, (float) candleWidth, (float) bodyHeight);
        canvas.setColor(strokeColor);
        canvas.drawRect((float) bodyLeft, (float) bodyTop, (float) candleWidth, (float) bodyHeight);
    }

    private double getResolvedCandleWidth(PlotContext context, double prevX, double lastX, double refY, int count) {
        final ArberRect plotBounds = context.getPlotBounds();
        double uniform = (plotBounds.width() / (double) Math.max(1, count)) * 0.7;

        double[] px = pBuffer4();
        context.mapToPixel(prevX, refY, px);
        double pxPrev = px[0];
        context.mapToPixel(lastX, refY, px);
        double stepPx = Math.abs(px[0] - pxPrev);
        if (Double.isFinite(stepPx) && stepPx > 0.0) {
            uniform = stepPx * 0.7;
        }
        return Math.max(1.0, uniform);
    }

    private double getComputedAtr(ChartModel model, int period) {
        final int n = model.getPointCount();
        if (n <= period) return 0.0;
        final int start = Math.max(1, n - period);
        double sum = 0.0;
        int count = 0;
        for (int i = start; i < n; i++) {
            final double high = model.getMax(i);
            final double low = model.getMin(i);
            final double prevClose = model.getY(i - 1);
            if (!finite3(high, low, prevClose)) continue;
            final double tr1 = high - low;
            final double tr2 = Math.abs(high - prevClose);
            final double tr3 = Math.abs(low - prevClose);
            sum += Math.max(tr1, Math.max(tr2, tr3));
            count++;
        }
        return (count > 0) ? sum / count : 0.0;
    }

    private int lastFiniteIndex(ChartModel model) {
        for (int i = model.getPointCount() - 1; i >= 0; i--) {
            double x = model.getX(i);
            double y = model.getY(i);
            if (Double.isFinite(x) && Double.isFinite(y)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean finite3(double a, double b, double c) {
        return Double.isFinite(a) && Double.isFinite(b) && Double.isFinite(c);
    }

    private static boolean finite4(double a, double b, double c, double d) {
        return Double.isFinite(a) && Double.isFinite(b) && Double.isFinite(c) && Double.isFinite(d);
    }

    private static float clamp01(float v) {
        if (!Float.isFinite(v)) return 0f;
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}
