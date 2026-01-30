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
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class HistogramRenderer extends BaseRenderer {

    private int frameId = 1;

    public HistogramRenderer() {
        super("histogram");
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
        }
        if (minX >= maxX) return;

        int bins = ChartAssets.getInt("chart.histogram.bins", 20);
        if (bins <= 0) bins = 20;

        double[] counts = RendererAllocationCache.getDoubleArray(this, "hist.counts", bins);
        int[] marks = RendererAllocationCache.getIntArray(this, "hist.marks", bins);
        int[] touched = RendererAllocationCache.getIntArray(this, "hist.touched", bins);
        int currentFrame = nextFrameId(marks);
        int touchedCount = 0;
        double binWidth = (maxX - minX) / bins;

        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            int bin = (int) ((x - minX) / binWidth);
            if (bin >= bins) bin = bins - 1;
            double w = model.getValue(i, 2) > 0 ? model.getValue(i, 2) : 1.0;
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
        if (maxCount <= 0) return;

        ArberRect bounds = context.getPlotBounds();
        double plotX = bounds.x();
        double plotY = bounds.y();
        double plotW = bounds.width();
        double plotH = bounds.height();

        double barPixelWidth = plotW / bins;
        boolean drawOutline = ChartAssets.getBoolean("chart.histogram.drawOutline", false);

        for (int i = 0; i < touchedCount; i++) {
            int bin = touched[i];
            double c = counts[bin];
            if (c <= 0) continue;

            ArberColor binColor = seriesOrBase(model, context, bin);
            double heightRatio = c / maxCount;
            double h = plotH * heightRatio;
            double x = plotX + bin * barPixelWidth;
            double y = plotY + (plotH - h);
            float w = (float) Math.max(1.0, barPixelWidth * 0.9);
            float xi = (float) x;
            float yi = (float) y;
            float hi = (float) h;

            canvas.setColor(ColorUtils.applyAlpha(binColor, 0.67f));
            canvas.fillRect(xi, yi, w, hi);

            if (drawOutline) {
                canvas.setColor(ColorUtils.adjustBrightness(binColor, 0.7f));
                canvas.setStroke(1.0f);
                canvas.drawRect(xi, yi, w, hi);
            }
        }
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
