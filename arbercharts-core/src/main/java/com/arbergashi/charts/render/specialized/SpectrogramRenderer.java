package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorUtils;
/**
 * Spectrogram Renderer - ArberGashi Engine.
 * High-performance visualization of frequency spectra over time using a reusable pixel buffer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class SpectrogramRenderer extends BaseRenderer {

    private final ArberColor[] colorLut = new ArberColor[256];
    private com.arbergashi.charts.api.ChartTheme lastTheme;
    private boolean lastMultiColor;

    public SpectrogramRenderer() {
        super("spectrogram");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;
        double[] yData = model.getYData();
        if (yData == null || yData.length == 0) return;
        int limit = Math.min(count, yData.length);
        if (limit <= 0) return;

        ArberRect bounds = context.getPlotBounds();
        int w = (int) Math.round(bounds.width());
        int h = (int) Math.round(bounds.height());
        if (w <= 0 || h <= 0) return;
        ensureLut(context);

        // Simple mapping: sample points across width
        double minVal = 0;
        double maxVal = 1.0;
        // Calculate range O(n)
        if (limit > 0) {
            minVal = Double.MAX_VALUE;
            maxVal = -Double.MAX_VALUE;
            for (int i = 0; i < limit; i++) {
                double v = yData[i];
                if (v < minVal) minVal = v;
                if (v > maxVal) maxVal = v;
            }
        }

        double range = maxVal - minVal;
        if (range <= 0) range = 1.0;

        int pointsPerPixel = Math.max(1, limit / w);

        for (int x = 0; x < w; x++) {
            int pointIdx = Math.min(limit - 1, x * pointsPerPixel);
            double val = yData[pointIdx];

            float intensity = (float) ((val - minVal) / range);
            ArberColor color = mapToColor(intensity);
            if (color == null) continue;
            canvas.setColor(color);
            canvas.fillRect((float) (bounds.x() + x), (float) bounds.y(), 1f, (float) h);
        }
    }

    private ArberColor mapToColor(float intensity) {
        int idx = Math.min(255, Math.max(0, Math.round(intensity * 255f)));
        return colorLut[idx];
    }

    private void ensureLut(PlotContext context) {
        com.arbergashi.charts.api.ChartTheme theme = getResolvedTheme(context);
        boolean multi = isMultiColor();
        if (theme == lastTheme && lastMultiColor == multi) return;
        lastTheme = theme;
        lastMultiColor = multi;

        ArberColor c0;
        ArberColor c1;
        ArberColor c2;
        ArberColor c3;
        if (multi) {
            c0 = theme.getSeriesColor(0);
            c1 = theme.getSeriesColor(1);
            c2 = theme.getSeriesColor(2);
            c3 = theme.getSeriesColor(3);
        } else {
            ArberColor base = theme.getAccentColor();
            c0 = ColorUtils.applyAlpha(base, 0.25f);
            c1 = ColorUtils.applyAlpha(base, 0.45f);
            c2 = ColorUtils.applyAlpha(base, 0.65f);
            c3 = ColorUtils.applyAlpha(base, 0.85f);
        }
        for (int i = 0; i < 256; i++) {
            float t = i / 255.0f;
            ArberColor c;
            if (t < 0.33f) {
                c = ColorUtils.interpolate(c0, c1, t / 0.33f);
            } else if (t < 0.66f) {
                c = ColorUtils.interpolate(c1, c2, (t - 0.33f) / 0.33f);
            } else {
                c = ColorUtils.interpolate(c2, c3, (t - 0.66f) / 0.34f);
            }
            colorLut[i] = c;
        }
    }
}
