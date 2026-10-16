package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ColorUtils;

/**
 * Professional, zero-allocation histogram renderer (headless).
 *
 * <p>Draws simple filled bars; gradients and labels are bridge concerns.</p>
 * <p>X values are observations. A finite positive component 2 supplies their
 * weight; other weights count as one observation. Bin edges and weighted counts
 * are mapped through the plot context, so explicit axis ranges remain valid.</p>
 * @author Arber Gashi
 * @since 2026-01-30
 */
public final class HistogramRenderer extends BaseRenderer {

    private int frameId = 1;

    public HistogramRenderer() {
        super("histogram");
    }

    @Override
    public double[] getPreferredYRange(ChartModel model) {
        double[] range = pBuffer4();
        if (computeBins(model, binCount(), range) == 0) return null;
        return new double[]{0.0, range[2] * 1.05};
    }

    @Override
    public double[] getPreferredXRange(ChartModel model) {
        double[] range = pBuffer4();
        if (computeBins(model, binCount(), range) == 0) return null;
        return new double[]{range[0], range[1]};
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int bins = binCount();
        double[] range = pBuffer4();
        int touchedCount = computeBins(model, bins, range);
        if (touchedCount == 0) return;
        double minX = range[0];
        double binWidth = (range[1] - minX) / bins;
        double[] counts = RendererAllocationCache.getDoubleArray(this, "hist.counts", bins);
        int[] touched = RendererAllocationCache.getIntArray(this, "hist.touched", bins);
        ArberRect bounds = context.getPlotBounds();
        double baseline = Math.max(context.getMinY(), Math.min(context.getMaxY(), 0.0));
        double[] pixel = pBuffer();
        boolean drawOutline = ChartAssets.getBoolean("chart.histogram.drawOutline", false);
        for (int i = 0; i < touchedCount; i++) {
            int bin = touched[i];
            double count = counts[bin];
            if (!(count > 0.0)) continue;
            double left = minX + bin * binWidth;
            context.mapToPixel(left, count, pixel);
            double firstX = pixel[0];
            double top = pixel[1];
            context.mapToPixel(left + binWidth * 0.9, baseline, pixel);
            if (!Double.isFinite(firstX) || !Double.isFinite(top)
                    || !Double.isFinite(pixel[0]) || !Double.isFinite(pixel[1])) continue;
            double x = Math.max(bounds.x(), Math.min(firstX, pixel[0]));
            double right = Math.min(bounds.maxX(), Math.max(firstX, pixel[0]));
            double y = Math.max(bounds.y(), Math.min(top, pixel[1]));
            double bottom = Math.min(bounds.maxY(), Math.max(top, pixel[1]));
            if (!(right > x) || !(bottom > y)) continue;
            ArberColor binColor = seriesOrBase(model, context, bin);
            canvas.setColor(ColorUtils.applyAlpha(binColor, 0.67f));
            canvas.fillRect((float) x, (float) y, (float) (right - x), (float) (bottom - y));
            if (drawOutline) {
                canvas.setColor(ColorUtils.adjustBrightness(binColor, 0.7f));
                canvas.setStroke(1.0f);
                canvas.drawRect((float) x, (float) y, (float) (right - x), (float) (bottom - y));
            }
        }
    }

    private int binCount() {
        int bins = ChartAssets.getInt("chart.histogram.bins", 20);
        return bins > 0 ? bins : 20;
    }

    private int computeBins(ChartModel model, int bins, double[] range) {
        final int n = model.getPointCount();
        if (n == 0) return 0;

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            if (!Double.isFinite(x)) continue;
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
        }
        if (!Double.isFinite(minX) || !Double.isFinite(maxX)) return 0;
        if (minX == maxX) {
            double padding = Math.max(0.5, Math.abs(minX) * 0.05);
            minX -= padding;
            maxX += padding;
        }
        double binWidth = (maxX - minX) / bins;
        if (!(binWidth > 0.0) || !Double.isFinite(binWidth)) return 0;

        double[] counts = RendererAllocationCache.getDoubleArray(this, "hist.counts", bins);
        int[] marks = RendererAllocationCache.getIntArray(this, "hist.marks", bins);
        int[] touched = RendererAllocationCache.getIntArray(this, "hist.touched", bins);
        int currentFrame = nextFrameId(marks);
        int touchedCount = 0;

        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            if (!Double.isFinite(x)) continue;
            int bin = Math.max(0, Math.min(bins - 1, (int) ((x - minX) / binWidth)));
            double weight = model.getValue(i, 2);
            double w = Double.isFinite(weight) && weight > 0.0 ? weight : 1.0;
            if (marks[bin] != currentFrame) {
                marks[bin] = currentFrame;
                counts[bin] = 0.0;
                touched[touchedCount++] = bin;
            }
            counts[bin] += w;
        }

        double maxCount = 0;
        for (int i = 0; i < touchedCount; i++) {
            double c = counts[touched[i]];
            if (c > maxCount) maxCount = c;
        }
        range[0] = minX;
        range[1] = maxX;
        range[2] = maxCount;
        return touchedCount;
    }

    private int nextFrameId(int[] marks) {
        if (frameId == Integer.MAX_VALUE) {
            for (int i = 0; i < marks.length; i++) {
                marks[i] = 0;
            }
            frameId = 1;
        } else {
            frameId++;
        }
        return frameId;
    }
}
