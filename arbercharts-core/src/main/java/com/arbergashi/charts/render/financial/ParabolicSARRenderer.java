package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
/**
 * <h1>ParabolicSARRenderer - Parabolic Stop and Reverse</h1>
 *
 * <p>Professional Parabolic SAR renderer for stop-loss and trend reversal points.
 * Displays dots above (downtrend) or below (uptrend) price bars.</p>
 *
 * <p>This implementation is optimized for Swing painting:
 * all indicator series are cached in primitive arrays and reused.
 * No per-point allocations occur inside {@link #drawData(Graphics2D, ChartModel, PlotContext)}.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class ParabolicSARRenderer extends BaseRenderer {

    private static final double AF_START = 0.02;
    private static final double AF_INCREMENT = 0.02;
    private static final double AF_MAX = 0.20;

    private final double[] px = new double[2];
    private final float[] dotX = new float[1];
    private final float[] dotY = new float[1];

    // Cached indicator arrays (x + sar + trend flag)
    private transient ChartModel cachedModel;
    private transient int cachedPointCount;
    private transient double[] xValues;
    private transient double[] sarValues;
    private transient boolean[] uptrend;

    public ParabolicSARRenderer() {
        super("parabolic_sar");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < 3) return;

        ensureCache(model);
        if (cachedPointCount < 2 || xValues == null || sarValues == null || uptrend == null) return;

        final IndicatorRendererSupport.IndexRange range = IndicatorRendererSupport.visibleRange(context, cachedPointCount, 2);
        final int start = range.getStart();
        final int endExclusive = range.getEndExclusive();
        if (endExclusive - start < 2) return;

        final IndicatorRendererSupport.Viewport vp = IndicatorRendererSupport.viewport(context);

        final double leftX = vp.getX();
        final double rightX = vp.getMaxX();

        final ChartTheme theme = getTheme();
        final ArberColor uptrendColor = theme.getBullishColor();
        final ArberColor downtrendColor = theme.getBearishColor();
        final ArberColor borderColor = theme.getForeground();

        // Build a single path containing all dot circles (sub-path per dot).
        // We draw two paths (up/down) to avoid per-dot color switching overhead.
        final float dotSize = ChartScale.scale(6.0f);
        final float half = dotSize * 0.5f;
        final float border = ChartScale.scale(0.75f);

        // Uptrend dots
        boolean anyUp = false;
        for (int i = start; i < endExclusive; i++) {
            if (!uptrend[i]) continue;
            context.mapToPixel(xValues[i], sarValues[i], px);
            final double cx = px[0];
            if (cx < leftX - dotSize || cx > rightX + dotSize) continue;
            anyUp = true;
            dotX[0] = (float) (cx - half);
            dotY[0] = (float) (px[1] - half);
            canvas.setColor(uptrendColor);
            canvas.fillRect(dotX[0], dotY[0], dotSize, dotSize);
            canvas.setColor(borderColor);
            canvas.setStroke(border);
            canvas.drawRect(dotX[0], dotY[0], dotSize, dotSize);
        }

        // Downtrend dots
        boolean anyDown = false;
        for (int i = start; i < endExclusive; i++) {
            if (uptrend[i]) continue;
            context.mapToPixel(xValues[i], sarValues[i], px);
            final double cx = px[0];
            if (cx < leftX - dotSize || cx > rightX + dotSize) continue;
            anyDown = true;
            dotX[0] = (float) (cx - half);
            dotY[0] = (float) (px[1] - half);
            canvas.setColor(downtrendColor);
            canvas.fillRect(dotX[0], dotY[0], dotSize, dotSize);
            canvas.setColor(borderColor);
            canvas.setStroke(border);
            canvas.drawRect(dotX[0], dotY[0], dotSize, dotSize);
        }
    }

    private void ensureCache(ChartModel model) {
        final int n = model.getPointCount();
        if (n < 3) {
            cachedModel = model;
            cachedPointCount = 0;
            return;
        }

        if (cachedModel == model && cachedPointCount == n && xValues != null && sarValues != null && uptrend != null) {
            return;
        }

        cachedModel = model;
        cachedPointCount = n;

        // Ensure each array exists and has enough capacity.
        if (xValues == null || xValues.length < n) {
            xValues = new double[n];
        }
        if (sarValues == null || sarValues.length < n) {
            sarValues = new double[n];
        }
        if (uptrend == null || uptrend.length < n) {
            uptrend = new boolean[n];
        }

        final double[] xs = xValues;
        final double[] sars = sarValues;
        final boolean[] ups = uptrend;

        for (int i = 0; i < n; i++) {
            xs[i] = model.getX(i);
        }

        // Initialize
        final double close0 = model.getY(0);
        final double close1 = model.getY(1);
        boolean isUp = close1 > close0;
        double sar = isUp ? model.getMin(0) : model.getMax(0);
        double ep = isUp ? model.getMax(1) : model.getMin(1);
        double af = AF_START;

        sars[0] = sar;
        ups[0] = isUp;

        for (int i = 1; i < n; i++) {
            final double high = model.getMax(i);
            final double low = model.getMin(i);

            // SAR(today) = SAR(yesterday) + AF * (EP - SAR(yesterday))
            sar = Math.fma(af, (ep - sar), sar);

            if (isUp) {
                if (low < sar) {
                    // reversal to downtrend
                    isUp = false;
                    sar = ep;
                    ep = low;
                    af = AF_START;
                } else {
                    if (high > ep) {
                        ep = high;
                        af = Math.min(af + AF_INCREMENT, AF_MAX);
                    }
                    if (i >= 2) {
                        sar = Math.min(sar, model.getMin(i - 1));
                        sar = Math.min(sar, model.getMin(i - 2));
                    }
                }
            } else {
                if (high > sar) {
                    // reversal to uptrend
                    isUp = true;
                    sar = ep;
                    ep = high;
                    af = AF_START;
                } else {
                    if (low < ep) {
                        ep = low;
                        af = Math.min(af + AF_INCREMENT, AF_MAX);
                    }
                    if (i >= 2) {
                        sar = Math.max(sar, model.getMax(i - 1));
                        sar = Math.max(sar, model.getMax(i - 2));
                    }
                }
            }

            sars[i] = sar;
            ups[i] = isUp;
        }
    }
}
