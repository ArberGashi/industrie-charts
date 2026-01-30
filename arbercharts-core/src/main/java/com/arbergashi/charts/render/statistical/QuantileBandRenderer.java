package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.util.Arrays;
/**
 * Renders a statistical quantile band (e.g., 25-75%) as a pre-data overlay.
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class QuantileBandRenderer extends BaseRenderer {
    private static final String KEY_ENABLED = "Chart.statistical.quantileBand.enabled";
    private static final String KEY_ALPHA_OUTER = "Chart.statistical.quantileBand.alphaOuter";
    private static final String KEY_ALPHA_INNER = "Chart.statistical.quantileBand.alphaInner";
    private static final String KEY_ALPHA_MEDIAN = "Chart.statistical.quantileBand.alphaMedian";
    // Legacy keys kept as a fallback for older configs.
    private static final String KEY_ALPHA = "Chart.statistical.quantileBand.alpha";
    private static final String KEY_BORDER_ALPHA = "Chart.statistical.quantileBand.borderAlpha";
    private static final String KEY_MEDIAN_ALPHA = "Chart.statistical.quantileBand.medianAlpha";
    private static final String KEY_STROKE = "Chart.statistical.quantileBand.strokeWidth";
    private static final String KEY_WINDOW = "Chart.statistical.quantileBand.windowPoints";
    private static final String KEY_QUANTILES = "Chart.statisticalGrid.quantiles";

    public QuantileBandRenderer() {
        super("quantile_band");
    }

    @Override
    public String getName() {
        return "Quantile Band";
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
        final int windowCap = Math.max(64, ChartAssets.getInt(KEY_WINDOW, 2048));

        double[] values = RendererAllocationCache.getDoubleArray(this, "quantile.values", Math.min(windowCap, n));
        double[] px = pBuffer();

        int count = 0;
        for (int i = Math.max(0, n - windowCap); i < n; i++) {
            double x = model.getX(i);
            double y = model.getY(i);
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
            context.mapToPixel(x, y, px);
            double pxX = px[0];
            if (pxX < clipLeft || pxX > clipRight) continue;
            if (count >= values.length) break;
            values[count++] = y;
        }

        if (count < 4) return;

        Arrays.sort(values, 0, count);

        double[] quantiles = parseQuantiles(ChartAssets.getString(KEY_QUANTILES, ""));
        if (quantiles.length < 2) return;

        double lowerQ = quantiles[0];
        double upperQ = quantiles[quantiles.length - 1];
        double medianQ = 0.5;
        boolean hasMedian = false;
        for (double q : quantiles) {
            if (Math.abs(q - 0.5) < 1e-6) {
                hasMedian = true;
                medianQ = q;
                break;
            }
        }
        if (!hasMedian) {
            medianQ = quantiles[quantiles.length / 2];
        }

        double lower = quantile(values, count, lowerQ);
        double upper = quantile(values, count, upperQ);
        double median = quantile(values, count, medianQ);
        if (!Double.isFinite(lower) || !Double.isFinite(upper) || !Double.isFinite(median)) return;

        double midX = model.getX(n - 1);
        context.mapToPixel(midX, lower, px);
        double yLower = context.snapPixel(px[1]);
        context.mapToPixel(midX, upper, px);
        double yUpper = context.snapPixel(px[1]);
        context.mapToPixel(midX, median, px);
        double yMedian = context.snapPixel(px[1]);

        double top = Math.min(yLower, yUpper);
        double height = Math.max(1.0, Math.abs(yUpper - yLower));

        ArberColor base = themeAccent(context);
        float fillAlpha = clamp01(getResolvedAlpha(KEY_ALPHA_OUTER, KEY_ALPHA, 0.18f));
        float borderAlpha = clamp01(getResolvedAlpha(KEY_ALPHA_INNER, KEY_BORDER_ALPHA, 0.28f));
        float medianAlpha = clamp01(getResolvedAlpha(KEY_ALPHA_MEDIAN, KEY_MEDIAN_ALPHA, 0.55f));

        canvas.setColor(ColorUtils.applyAlpha(base, fillAlpha));
        canvas.fillRect((float) plot.x(), (float) top, (float) plot.width(), (float) height);

        float stroke = ChartScale.scale(ChartAssets.getFloat(KEY_STROKE, 1.2f));
        canvas.setStroke(stroke);

        canvas.setColor(ColorUtils.applyAlpha(base, borderAlpha));
        drawLine(canvas, plot.x(), yLower, plot.maxX(), yLower);
        drawLine(canvas, plot.x(), yUpper, plot.maxX(), yUpper);

        canvas.setColor(ColorUtils.applyAlpha(base, medianAlpha));
        drawLine(canvas, plot.x(), yMedian, plot.maxX(), yMedian);
    }

    private static double[] parseQuantiles(String spec) {
        if (spec == null || spec.isBlank()) return new double[0];
        String[] parts = spec.split(",");
        double[] q = new double[parts.length];
        int n = 0;
        for (String p : parts) {
            try {
                double v = Double.parseDouble(p.trim());
                if (Double.isFinite(v) && v > 0.0 && v < 1.0) {
                    q[n++] = v;
                }
            } catch (NumberFormatException ignore) {
                // Ignore invalid quantiles.
            }
        }
        Arrays.sort(q, 0, n);
        return Arrays.copyOf(q, n);
    }

    private static double quantile(double[] sorted, int n, double q) {
        if (n <= 0 || !(q >= 0.0 && q <= 1.0)) return Double.NaN;
        double pos = q * (n - 1);
        int lo = (int) Math.floor(pos);
        int hi = Math.min(n - 1, lo + 1);
        double t = pos - lo;
        return sorted[lo] * (1.0 - t) + sorted[hi] * t;
    }

    private static float clamp01(float v) {
        if (!Float.isFinite(v)) return 0f;
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    private static float getResolvedAlpha(String primaryKey, String legacyKey, float defaultValue) {
        float primary = ChartAssets.getFloat(primaryKey, Float.NaN);
        if (Float.isFinite(primary)) {
            return primary;
        }
        return ChartAssets.getFloat(legacyKey, defaultValue);
    }

    private void drawLine(ArberCanvas canvas, double x0, double y0, double x1, double y1) {
        float[] xs = RendererAllocationCache.getFloatArray(this, "qb.lineX", 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "qb.lineY", 2);
        xs[0] = (float) x0;
        ys[0] = (float) y0;
        xs[1] = (float) x1;
        ys[1] = (float) y1;
        canvas.drawPolyline(xs, ys, 2);
    }
}
