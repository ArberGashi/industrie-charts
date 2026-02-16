package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FinancialChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * Professional MACD indicator renderer.
 * This implementation is zero-allocation and works directly on primitive data.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class MACDRenderer extends BaseRenderer {

    private final double[] pxA = new double[2];
    private final double[] pxB = new double[2];

    private transient float[] macdX;
    private transient float[] macdY;
    private transient float[] signalX;
    private transient float[] signalY;
    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];

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

    private static void getComputedEMA(double[] values, int period, double[] out) {
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

    private static void getComputedEMAOnArray(double[] src, int start, int end, int period, double[] out) {
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

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        ensureCache(model);
        if (cachedPointCount <= 0) return;

        final double leftX = context.getPlotBounds().minX();
        final double rightX = context.getPlotBounds().maxX();

        final double w = context.getPlotBounds().getWidth();
        final double barWidth = (w / (double) cachedPointCount) * 0.6;

        final ArberColor macdColor = themeAccent(context);
        final ArberColor bullishColor = themeBullish(context);
        final ArberColor bearishColor = themeBearish(context);
        final ArberColor signalColor = ColorRegistry.adjustBrightness(macdColor, 1.2f);
        final ArberColor bullishHist = ColorRegistry.applyAlpha(bullishColor, 0.5f);
        final ArberColor bearishHist = ColorRegistry.applyAlpha(bearishColor, 0.5f);

        // Zero line
        context.mapToPixel(xValues[0], 0, pxA);
        context.mapToPixel(xValues[cachedPointCount - 1], 0, pxB);
        canvas.setColor(themeGrid(context));
        canvas.setStroke(ChartScale.scale(1.0f));
        lineX[0] = (float) pxA[0];
        lineY[0] = (float) pxA[1];
        lineX[1] = (float) pxB[0];
        lineY[1] = (float) pxB[1];
        canvas.drawPolyline(lineX, lineY, 2);

        // Histogram, MACD, and Signal lines
        drawHistogram(canvas, context, barWidth, leftX, rightX, bullishHist, bearishHist);
        drawLines(canvas, context, macdColor, signalColor);
    }

    private void drawHistogram(ArberCanvas canvas, PlotContext context, double barWidth, double leftX, double rightX, ArberColor bullish, ArberColor bearish) {
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

            canvas.setColor(histogram >= 0 ? bullish : bearish);
            canvas.fillRect((float) x, (float) y, (float) barWidth, (float) barHeight);
        }
    }

    private void drawLines(ArberCanvas canvas, PlotContext context, ArberColor macdColor, ArberColor signalColor) {
        int macdCount = 0;
        int signalCount = 0;
        ensurePathCapacity(cachedPointCount);

        for (int i = 0; i < cachedPointCount; i++) {
            double x = xValues[i];

            if (Double.isFinite(macdValues[i])) {
                context.mapToPixel(x, macdValues[i], pxA);
                macdX[macdCount] = (float) pxA[0];
                macdY[macdCount] = (float) pxA[1];
                macdCount++;
            }

            if (Double.isFinite(signalValues[i])) {
                context.mapToPixel(x, signalValues[i], pxB);
                signalX[signalCount] = (float) pxB[0];
                signalY[signalCount] = (float) pxB[1];
                signalCount++;
            }
        }

        if (macdCount >= 2) {
            canvas.setStroke(ChartScale.scale(2.0f));
            canvas.setColor(macdColor);
            canvas.drawPolyline(macdX, macdY, macdCount);
        }

        if (signalCount >= 2) {
            canvas.setStroke(ChartScale.scale(1.5f));
            canvas.setColor(signalColor);
            canvas.drawPolyline(signalX, signalY, signalCount);
        }
    }

    private void ensurePathCapacity(int count) {
        if (macdX == null || macdX.length < count) {
            macdX = new float[count];
            macdY = new float[count];
            signalX = new float[count];
            signalY = new float[count];
        }
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

        final FinancialChartModel fin = (model instanceof FinancialChartModel) ? (FinancialChartModel) model : null;
        if (fin != null) {
            for (int i = 0; i < n; i++) {
                xValues[i] = model.getX(i);
                yValues[i] = fin.getClose(i);
            }
        } else {
            for (int i = 0; i < n; i++) {
                xValues[i] = model.getX(i);
                yValues[i] = model.getY(i);
            }
        }

        getComputedEMA(yValues, 12, emaFast);
        getComputedEMA(yValues, 26, emaSlow);

        for (int i = 0; i < 25; i++) macdValues[i] = Double.NaN;
        for (int i = 25; i < n; i++) macdValues[i] = emaFast[i] - emaSlow[i];

        getComputedEMAOnArray(macdValues, 25, n, 9, signalValues);

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
