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

/**
 * Renders a live distribution histogram along the Y-axis as a pre-data overlay (headless).
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class LiveDistributionOverlayRenderer extends BaseRenderer {
    private static final String KEY_ENABLED = "Chart.statistical.histogram.enabled";
    private static final String KEY_BINS = "Chart.statistical.histogram.bins";
    private static final String KEY_WIDTH_RATIO = "Chart.statistical.histogram.widthRatio";
    private static final String KEY_ALPHA = "Chart.statistical.histogram.alpha";
    private static final String KEY_WINDOW = "Chart.statistical.histogram.windowPoints";
    private static final String KEY_CURVE_ALPHA = "Chart.statistical.histogram.curveAlpha";
    private static final String KEY_STROKE = "Chart.statistical.histogram.strokeWidth";

    private int frameStamp = 1;
    private final float[] dashPattern = new float[2];

    public LiveDistributionOverlayRenderer() {
        super("live_distribution_overlay");
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!ChartAssets.getBoolean(KEY_ENABLED, true)) return;
        final int n = model.getPointCount();
        if (n < 4) return;

        final ArberRect plot = context.getPlotBounds();
        final double minY = context.getMinY();
        final double maxY = context.getMaxY();
        if (!(maxY > minY)) return;

        final int bins = Math.max(12, ChartAssets.getInt(KEY_BINS, 30));
        final double binSize = (maxY - minY) / bins;
        if (!(binSize > 0.0)) return;

        frameStamp++;
        if (frameStamp == Integer.MAX_VALUE) {
            frameStamp = 1;
            int[] reset = RendererAllocationCache.getIntArray(this, "dist.stamps", bins);
            java.util.Arrays.fill(reset, 0);
        }

        final double[] counts = RendererAllocationCache.getDoubleArray(this, "dist.counts", bins);
        final int[] stamps = RendererAllocationCache.getIntArray(this, "dist.stamps", bins);
        final int[] touched = RendererAllocationCache.getIntArray(this, "dist.touched", bins);
        int touchedCount = 0;

        final int windowCap = Math.max(128, ChartAssets.getInt(KEY_WINDOW, 2048));
        final double clipLeft = plot.x();
        final double clipRight = plot.maxX();
        final double[] px = pBuffer();

        int included = 0;
        double mean = 0.0;
        double m2 = 0.0;

        for (int i = Math.max(0, n - windowCap); i < n; i++) {
            final double x = model.getX(i);
            final double y = model.getY(i);
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;

            context.mapToPixel(x, y, px);
            final double pxX = px[0];
            if (pxX < clipLeft || pxX > clipRight) continue;

            int bin = (int) ((y - minY) / binSize);
            if (bin < 0 || bin >= bins) continue;

            if (stamps[bin] != frameStamp) {
                stamps[bin] = frameStamp;
                counts[bin] = 1.0;
                touched[touchedCount++] = bin;
            } else {
                counts[bin] += 1.0;
            }

            included++;
            double delta = y - mean;
            mean += delta / included;
            m2 += delta * (y - mean);
        }

        if (touchedCount == 0 || included < 4) return;

        double maxCount = 0.0;
        for (int i = 0; i < touchedCount; i++) {
            double v = counts[touched[i]];
            if (v > maxCount) maxCount = v;
        }
        if (!(maxCount > 0.0)) return;

        final double widthRatio = Math.min(0.5, Math.max(0.08, ChartAssets.getFloat(KEY_WIDTH_RATIO, 0.18f)));
        final double maxWidth = plot.width() * widthRatio;
        final double binPixelHeight = plot.height() / bins;

        final ArberColor base = themeAccent(context);
        final float alpha = clamp01(ChartAssets.getFloat(KEY_ALPHA, 0.28f));
        canvas.setColor(ColorUtils.applyAlpha(base, alpha));

        for (int i = 0; i < touchedCount; i++) {
            int bin = touched[i];
            double count = counts[bin];
            if (count <= 0.0) continue;

            double width = (count / maxCount) * maxWidth;
            double y = plot.maxY() - (bin + 1) * binPixelHeight;
            canvas.fillRect((float) (plot.maxX() - width), (float) y, (float) width, (float) Math.max(1.0, binPixelHeight - 1.0));
        }

        final double variance = m2 / Math.max(1, included - 1);
        final double stdDev = Math.sqrt(Math.max(0.0, variance));
        if (!(stdDev > 1e-9)) return;

        final float strokeWidth = ChartScale.scale(ChartAssets.getFloat(KEY_STROKE, 1.2f));
        canvas.setStroke(strokeWidth);
        final float curveAlpha = clamp01(ChartAssets.getFloat(KEY_CURVE_ALPHA, 0.7f));
        canvas.setColor(ColorUtils.applyAlpha(base, curveAlpha));

        final double invStdSqrt2Pi = 1.0 / (stdDev * Math.sqrt(2.0 * Math.PI));
        double prevX = Double.NaN;
        double prevY = Double.NaN;

        float[] xs = RendererAllocationCache.getFloatArray(this, "dist.curveX", bins);
        float[] ys = RendererAllocationCache.getFloatArray(this, "dist.curveY", bins);
        int count = 0;

        for (int bin = 0; bin < bins; bin++) {
            double centerY = minY + (bin + 0.5) * binSize;
            double z = (centerY - mean) / stdDev;
            double density = invStdSqrt2Pi * Math.exp(-0.5 * z * z);
            double normalized = density / invStdSqrt2Pi; // exp(-0.5*z^2)
            double width = normalized * maxWidth;

            context.mapToPixel(context.getMinX(), centerY, px);
            double yPix = context.snapPixel(px[1]);
            double xPix = context.snapPixel(plot.maxX() - width);

            xs[count] = (float) xPix;
            ys[count] = (float) yPix;
            count++;

            if (Double.isFinite(prevX)) {
                // no-op, we build polyline below
            }
            prevX = xPix;
            prevY = yPix;
        }
        if (count > 1) {
            canvas.drawPolyline(xs, ys, count);
        }
    }

    private static float clamp01(float v) {
        if (!Float.isFinite(v)) return 0f;
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}
