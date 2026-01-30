package com.arbergashi.charts.render.specialized;

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
 * Pre-data glow overlay for radar charts.
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class RadarGlowRenderer extends BaseRenderer {
    private static final String KEY_ENABLED = "Chart.radar.glow.enabled";
    private static final String KEY_INTENSITY = "Chart.radar.glow.intensity";
    private static final String KEY_PULSE_SPEED = "Chart.radar.glow.pulseSpeed";
    private static final String KEY_MIN_ALPHA = "Chart.radar.glow.minAlpha";
    private static final String KEY_MAX_ALPHA = "Chart.radar.glow.maxAlpha";
    private static final String KEY_GRADIENT = "Chart.radar.glow.gradient";

    public RadarGlowRenderer() {
        super("radar_glow");
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!ChartAssets.getBoolean(KEY_ENABLED, true)) return;
        final int n = model.getPointCount();
        if (n < 3) return;

        ArberRect plot = context.getPlotBounds();
        if (plot == null || plot.getWidth() <= 1 || plot.getHeight() <= 1) return;

        final double maxY = context.getMaxY();
        if (!(maxY > 0.0)) return;

        final double cx = plot.centerX();
        final double cy = plot.centerY();
        final double maxRadius = Math.min(plot.getWidth(), plot.getHeight()) * 0.5 * 0.8;
        final double angleStep = (Math.PI * 2.0) / n;

        float[] xs = RendererAllocationCache.getFloatArray(this, "radar.glow.xs", n);
        float[] ys = RendererAllocationCache.getFloatArray(this, "radar.glow.ys", n);
        double peakRatio = 0.0;
        for (int i = 0; i < n; i++) {
            double value = model.getY(i);
            if (!Double.isFinite(value)) value = 0.0;
            double ratio = Math.max(0.0, Math.min(1.0, value / maxY));
            peakRatio = Math.max(peakRatio, ratio);

            double radius = maxRadius * ratio;
            double angle = i * angleStep - Math.PI * 0.5;
            xs[i] = (float) (cx + Math.cos(angle) * radius);
            ys[i] = (float) (cy + Math.sin(angle) * radius);
        }

        float intensity = clamp01(ChartAssets.getFloat(KEY_INTENSITY, 0.4f));
        float minAlpha = clamp01(ChartAssets.getFloat(KEY_MIN_ALPHA, 0.1f));
        float maxAlpha = clamp01(ChartAssets.getFloat(KEY_MAX_ALPHA, 0.6f));
        float pulseSpeed = Math.max(0.1f, ChartAssets.getFloat(KEY_PULSE_SPEED, 1.5f));
        boolean gradient = ChartAssets.getBoolean(KEY_GRADIENT, true);

        double seconds = System.nanoTime() * 1.0e-9;
        double pulse = 0.5 + 0.5 * Math.sin(seconds * pulseSpeed * Math.PI * 2.0);
        double gradientBoost = gradient ? 1.1 : 1.0;
        float alpha = (float) (minAlpha + (maxAlpha - minAlpha) * pulse * peakRatio * intensity * gradientBoost);
        alpha = clamp01(alpha);
        if (alpha <= 0f) return;

        ArberColor accent = themeAccent(context);
        canvas.setColor(ColorRegistry.applyAlpha(accent, alpha));
        canvas.fillPolygon(xs, ys, n);

        float strokeWidth = ChartScale.scale(1.4f);
        canvas.setStroke(strokeWidth);
        canvas.setColor(ColorRegistry.applyAlpha(accent, clamp01(alpha * 1.15f)));
        canvas.drawPolyline(xs, ys, n);
    }

    private static float clamp01(float v) {
        if (!Float.isFinite(v)) return 0f;
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}
