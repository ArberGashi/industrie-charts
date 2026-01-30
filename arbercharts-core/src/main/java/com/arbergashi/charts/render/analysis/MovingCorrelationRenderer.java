package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * Rolling Pearson correlation overlay rendered as a bottom-band indicator.
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class MovingCorrelationRenderer extends BaseRenderer {
    private static final String KEY_ENABLED = "Chart.analysis.correlation.enabled";
    private static final String KEY_WINDOW = "Chart.analysis.correlation.window";
    private static final String KEY_COMPONENT_A = "Chart.analysis.correlation.componentA";
    private static final String KEY_COMPONENT_B = "Chart.analysis.correlation.componentB";
    private static final String KEY_ALPHA = "Chart.analysis.correlation.alpha";
    private static final String KEY_BAND_RATIO = "Chart.analysis.correlation.bandHeightRatio";
    private static final String KEY_STROKE = "Chart.analysis.correlation.strokeWidth";
    private static final String KEY_COLOR_POS = "Chart.analysis.correlation.colorPositive";
    private static final String KEY_COLOR_NEG = "Chart.analysis.correlation.colorNegative";

    public MovingCorrelationRenderer() {
        super("moving_correlation");
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!ChartAssets.getBoolean(KEY_ENABLED, true)) return;
        final int n = model.getPointCount();
        if (n < 4) return;

        final ArberRect plot = context.getPlotBounds();
        final double clipLeft = plot.x();
        final double clipRight = plot.maxX();

        final int windowCap = Math.max(32, ChartAssets.getInt(KEY_WINDOW, 128));
        final int compA = Math.max(0, ChartAssets.getInt(KEY_COMPONENT_A, 1));
        final int compB = Math.max(0, ChartAssets.getInt(KEY_COMPONENT_B, 2));

        final double[] aVals = RendererAllocationCache.getDoubleArray(this, "corr.a", Math.min(windowCap, n));
        final double[] bVals = RendererAllocationCache.getDoubleArray(this, "corr.b", Math.min(windowCap, n));
        final double[] px = pBuffer();

        int count = 0;
        for (int i = Math.max(0, n - windowCap); i < n && count < aVals.length; i++) {
            final double x = model.getX(i);
            if (!Double.isFinite(x)) continue;
            context.mapToPixel(x, model.getY(i), px);
            final double pxX = px[0];
            if (pxX < clipLeft || pxX > clipRight) continue;

            final double a = model.getValue(i, compA);
            final double b = model.getValue(i, compB);
            if (!Double.isFinite(a) || !Double.isFinite(b)) continue;
            aVals[count] = a;
            bVals[count] = b;
            count++;
        }

        if (count < 4) return;

        final double corr = pearson(aVals, bVals, count);
        if (!Double.isFinite(corr)) return;

        renderBand(canvas, context, plot, corr);
    }

    private void renderBand(ArberCanvas canvas, PlotContext context, ArberRect plot, double corr) {
        final double bandRatio = clamp01(ChartAssets.getFloat(KEY_BAND_RATIO, 0.12f));
        final double bandHeight = Math.max(18.0, plot.height() * bandRatio);
        final double bandTop = plot.maxY() - bandHeight;

        final ArberColor positive = ChartAssets.getColor(KEY_COLOR_POS, ColorRegistry.of(0, 255, 0, 255));
        final ArberColor negative = ChartAssets.getColor(KEY_COLOR_NEG, ColorRegistry.of(255, 0, 0, 255));
        final ArberColor accent = corr >= 0.0 ? positive : negative;

        final float baseAlpha = clamp01(ChartAssets.getFloat(KEY_ALPHA, 0.35f));
        final float fillAlpha = clamp01((float) (Math.abs(corr) * baseAlpha));

        canvas.setColor(ColorRegistry.applyAlpha(accent, fillAlpha));
        canvas.fillRect((float) plot.x(), (float) bandTop, (float) plot.width(), (float) bandHeight);

        final float strokeWidth = ChartScale.scale(ChartAssets.getFloat(KEY_STROKE, 1.4f));
        canvas.setStroke(strokeWidth);

        // Neutral mid-line (correlation = 0)
        final double midY = context.snapPixel(bandTop + bandHeight * 0.5);
        ChartTheme theme = getResolvedTheme(context);
        canvas.setColor(ColorRegistry.applyAlpha(theme.getGridColor(), 0.45f));
        drawLine(canvas, plot.x(), midY, plot.maxX(), midY);

        // Indicator line mapped from [-1,1] to band height.
        final double norm = Math.max(-1.0, Math.min(1.0, corr));
        final double y = context.snapPixel(bandTop + (1.0 - ((norm + 1.0) * 0.5)) * bandHeight);
        canvas.setColor(ColorRegistry.applyAlpha(accent, clamp01(fillAlpha + 0.25f)));
        drawLine(canvas, plot.x(), y, plot.maxX(), y);
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        float[] xs = RendererAllocationCache.getFloatArray(this, "corr.line.x", 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "corr.line.y", 2);
        xs[0] = (float) x1;
        ys[0] = (float) y1;
        xs[1] = (float) x2;
        ys[1] = (float) y2;
        canvas.drawPolyline(xs, ys, 2);
    }

    private static double pearson(double[] a, double[] b, int n) {
        double meanA = 0.0;
        double meanB = 0.0;
        double varA = 0.0;
        double varB = 0.0;
        double cov = 0.0;

        for (int i = 0; i < n; i++) {
            double x = a[i];
            double y = b[i];
            double dx = x - meanA;
            double dy = y - meanB;
            int k = i + 1;
            meanA += dx / k;
            meanB += dy / k;
            varA += dx * (x - meanA);
            varB += dy * (y - meanB);
            cov += dx * (y - meanB);
        }

        if (!(varA > 1e-12) || !(varB > 1e-12)) return Double.NaN;
        return cov / Math.sqrt(varA * varB);
    }

    private static float clamp01(float v) {
        if (!Float.isFinite(v)) return 0f;
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}
