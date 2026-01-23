package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Professional, zero-allocation histogram renderer.
 *
 * <p>Uses cached gradient images and touched-bin resets to keep allocations near
 * the no-op baseline.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class HistogramRenderer extends BaseRenderer {

    private int frameId = 1;
    private static final float FILL_ALPHA = 0.67f;
    private static final int GRADIENT_HEIGHT = 100;

    public HistogramRenderer() {
        super("histogram");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
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

        double[] counts = com.arbergashi.charts.tools.RendererAllocationCache.getDoubleArray(this, "hist.counts", bins);
        int[] marks = com.arbergashi.charts.tools.RendererAllocationCache.getIntArray(this, "hist.marks", bins);
        int[] touched = com.arbergashi.charts.tools.RendererAllocationCache.getIntArray(this, "hist.touched", bins);
        int currentFrame = nextFrameId(marks);
        int touchedCount = 0;
        double binWidth = (maxX - minX) / bins;

        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            int bin = (int) ((x - minX) / binWidth);
            if (bin >= bins) bin = bins - 1;
            double w = model.getValue(i, 2) > 0 ? model.getValue(i, 2) : 1.0; // weight
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

        Rectangle2D bounds = context.plotBounds();
        double plotX = bounds.getX();
        double plotY = bounds.getY();
        double plotW = bounds.getWidth();
        double plotH = bounds.getHeight();

        double barPixelWidth = plotW / bins;

        BufferedImage[] fillImages = (BufferedImage[]) RendererAllocationCache.getArray(this, "hist.fillImages", BufferedImage.class, bins);
        int[] fillKeys = RendererAllocationCache.getIntArray(this, "hist.fillKeys", bins);
        int[] fillWidths = RendererAllocationCache.getIntArray(this, "hist.fillWidths", bins);
        int[] fillHeights = RendererAllocationCache.getIntArray(this, "hist.fillHeights", bins);
        Color[] fillLowColors = (Color[]) RendererAllocationCache.getArray(this, "hist.fillLow", Color.class, bins);
        Color[] fillHighColors = (Color[]) RendererAllocationCache.getArray(this, "hist.fillHigh", Color.class, bins);
        Color[] outlineColors = (Color[]) RendererAllocationCache.getArray(this, "hist.outlineColors", Color.class, bins);
        int[] outlineKeys = RendererAllocationCache.getIntArray(this, "hist.outlineKeys", bins);

        for (int i = 0; i < touchedCount; i++) {
            int bin = touched[i];
            double c = counts[bin];
            if (c <= 0) continue;

            // Each bin gets a distinct color from the theme palette (cached alpha variants)
            Color binColor = seriesOrBase(model, context, bin);
            double heightRatio = c / maxCount;
            double h = plotH * heightRatio;
            double x = plotX + bin * barPixelWidth;
            double y = plotY + (plotH - h);
            int w = (int) Math.round(Math.max(1.0, barPixelWidth * 0.9));
            int xi = (int) Math.round(x);
            int yi = (int) Math.round(y);
            int hi = (int) Math.round(h);
            BufferedImage fill = cachedFillImage(g2, binColor, bin, w, hi, fillImages, fillKeys, fillWidths, fillHeights,
                    fillLowColors, fillHighColors);
            g2.drawImage(fill, xi, yi, null);
        }

        if (ChartAssets.getBoolean("chart.histogram.drawOutline", false)) {
            g2.setStroke(getSeriesStroke());
            for (int i = 0; i < touchedCount; i++) {
                int bin = touched[i];
                double c = counts[bin];
                if (c <= 0) continue;

                Color binColor = seriesOrBase(model, context, bin);
                g2.setColor(cachedOutlineColor(binColor, bin, outlineColors, outlineKeys));

                double heightRatio = c / maxCount;
                double h = plotH * heightRatio;
                double x = plotX + bin * barPixelWidth;
                double y = plotY + (plotH - h);
                int w = (int) Math.round(Math.max(1.0, barPixelWidth * 0.9));
                int xi = (int) Math.round(x);
                int yi = (int) Math.round(y);
                int hi = (int) Math.round(h);
                int ow = Math.max(0, w - 1);
                int oh = Math.max(0, hi - 1);
                g2.drawRect(xi, yi, ow, oh);
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

    private BufferedImage cachedFillImage(Graphics2D g2, Color base, int index, int width, int height,
                                          BufferedImage[] images, int[] colorKeys, int[] widths, int[] heights,
                                          Color[] lowColors, Color[] highColors) {
        int key = (base != null) ? base.getRGB() : 0;
        if (width < 1) width = 1;
        if (height < 1) height = 1;
        if (images[index] == null || colorKeys[index] != key || widths[index] != width || heights[index] != height) {
            lowColors[index] = ColorUtils.withAlpha(base, FILL_ALPHA * 0.4f);
            highColors[index] = ColorUtils.withAlpha(base, FILL_ALPHA);
            images[index] = createCompatibleImage(g2, width, height);
            int[] row = RendererAllocationCache.getIntArray(this, "hist.fillRow", width);
            int lr = lowColors[index].getRed();
            int lg = lowColors[index].getGreen();
            int lb = lowColors[index].getBlue();
            int la = lowColors[index].getAlpha();
            int hr = highColors[index].getRed();
            int hg = highColors[index].getGreen();
            int hb = highColors[index].getBlue();
            int ha = highColors[index].getAlpha();
            float denom = (height - 1);
            for (int y = 0; y < height; y++) {
                float t = denom > 0 ? (y / denom) : 0f;
                int r = (int) (lr + (hr - lr) * t);
                int g = (int) (lg + (hg - lg) * t);
                int b = (int) (lb + (hb - lb) * t);
                int a = (int) (la + (ha - la) * t);
                int argb = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
                java.util.Arrays.fill(row, argb);
                images[index].setRGB(0, y, width, 1, row, 0, width);
            }
            colorKeys[index] = key;
            widths[index] = width;
            heights[index] = height;
        }
        return images[index];
    }

    private Color cachedOutlineColor(Color base, int index, Color[] outlineColors, int[] colorKeys) {
        int key = (base != null) ? base.getRGB() : 0;
        if (outlineColors[index] == null || colorKeys[index] != key) {
            outlineColors[index] = base.darker();
            colorKeys[index] = key;
        }
        return outlineColors[index];
    }

    private BufferedImage createCompatibleImage(Graphics2D g2, int w, int h) {
        GraphicsConfiguration gc = (g2 != null) ? g2.getDeviceConfiguration() : null;
        if (gc != null) {
            return gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
        }
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }
}
