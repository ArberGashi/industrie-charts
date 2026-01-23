package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Spectrogram Renderer - ArberGashi Engine.
 * High-performance visualization of frequency spectra over time using a reusable pixel buffer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class SpectrogramRenderer extends BaseRenderer {

    private transient BufferedImage buffer;
    private transient int[] pixels;
    private transient int bufferW = -1;
    private transient int bufferH = -1;
    private final int[] colorLut = new int[256];
    private com.arbergashi.charts.api.ChartTheme lastTheme;
    private boolean lastMultiColor;

    public SpectrogramRenderer() {
        super("spectrogram");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;
        double[] yData = model.getYData();

        Rectangle bounds = context.plotBounds().getBounds();
        if (bounds.width <= 0 || bounds.height <= 0) return;

        int w = bounds.width;
        int h = bounds.height;
        if (buffer == null || bufferW != w || bufferH != h) {
            buffer = RendererAllocationCache.getBufferedImage(this, "buffer", w, h, BufferedImage.TYPE_INT_ARGB);
            pixels = ((DataBufferInt) buffer.getRaster().getDataBuffer()).getData();
            bufferW = w;
            bufferH = h;
        }
        ensureLut(context);

        // Simple mapping: sample points across width
        double minVal = 0;
        double maxVal = 1.0;
        // Calculate range O(n)
        if (count > 0) {
            minVal = Double.MAX_VALUE;
            maxVal = -Double.MAX_VALUE;
            for (double v : yData) {
                if (v < minVal) minVal = v;
                if (v > maxVal) maxVal = v;
            }
        }

        double range = maxVal - minVal;
        if (range <= 0) range = 1.0;

        int pointsPerPixel = Math.max(1, count / w);

        for (int x = 0; x < w; x++) {
            int pointIdx = Math.min(count - 1, x * pointsPerPixel);
            double val = yData[pointIdx];

            float intensity = (float) ((val - minVal) / range);
            int color = mapToColor(intensity);

            int baseIdx = x;
            for (int y = 0; y < h; y++) {
                pixels[baseIdx + y * w] = color;
            }
        }

        g2.drawImage(buffer, bounds.x, bounds.y, null);
    }

    private int mapToColor(float intensity) {
        int idx = Math.min(255, Math.max(0, Math.round(intensity * 255f)));
        return colorLut[idx];
    }

    private void ensureLut(PlotContext context) {
        com.arbergashi.charts.api.ChartTheme theme = resolveTheme(context);
        boolean multi = isMultiColor();
        if (theme == lastTheme && lastMultiColor == multi) return;
        lastTheme = theme;
        lastMultiColor = multi;

        Color c0;
        Color c1;
        Color c2;
        Color c3;
        if (multi) {
            c0 = theme.getSeriesColor(0);
            c1 = theme.getSeriesColor(1);
            c2 = theme.getSeriesColor(2);
            c3 = theme.getSeriesColor(3);
        } else {
            Color base = theme.getAccentColor();
            c0 = ColorUtils.withAlpha(base, 0.25f);
            c1 = ColorUtils.withAlpha(base, 0.45f);
            c2 = ColorUtils.withAlpha(base, 0.65f);
            c3 = ColorUtils.withAlpha(base, 0.85f);
        }
        for (int i = 0; i < 256; i++) {
            float t = i / 255.0f;
            Color c;
            if (t < 0.33f) {
                c = ColorUtils.interpolate(c0, c1, t / 0.33f);
            } else if (t < 0.66f) {
                c = ColorUtils.interpolate(c1, c2, (t - 0.33f) / 0.33f);
            } else {
                c = ColorUtils.interpolate(c2, c3, (t - 0.66f) / 0.34f);
            }
            colorLut[i] = c.getRGB();
        }
    }
}
