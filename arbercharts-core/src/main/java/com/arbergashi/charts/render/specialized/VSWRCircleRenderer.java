package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

/**
 * Draws VSWR reference circles in Smith chart space.
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class VSWRCircleRenderer extends BaseRenderer {
    private static final String KEY_ENABLED = "Chart.smith.vswr.enabled";
    private static final String KEY_LEVELS = "Chart.smith.vswr.levels";
    private static final String KEY_COLOR = "Chart.smith.vswr.color";

    private static final int SEGMENTS = 160;

    private String cachedSpec;
    private double[] cachedLevels = new double[0];

    public VSWRCircleRenderer() {
        super("smith_vswr");
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!ChartAssets.getBoolean(KEY_ENABLED, true)) return;
        double[] levels = levels();
        if (levels.length == 0) return;

        ArberColor accent = ChartAssets.getColor(KEY_COLOR, themeAccent(context));
        canvas.setColor(ColorUtils.applyAlpha(accent, 0.55f));
        canvas.setStroke(ChartScale.scale(1.05f));

        float[] xs = RendererAllocationCache.getFloatArray(this, "vswr.x", SEGMENTS + 1);
        float[] ys = RendererAllocationCache.getFloatArray(this, "vswr.y", SEGMENTS + 1);
        final double[] px = pBuffer();
        for (double vswr : levels) {
            if (!(vswr > 1.0)) continue;
            double gamma = (vswr - 1.0) / (vswr + 1.0);
            if (!(gamma > 0.0) || gamma >= 1.0) continue;
            drawCircle(canvas, context, gamma, px, xs, ys);
        }
    }

    private void drawCircle(ArberCanvas canvas, PlotContext context, double radius, double[] px, float[] xs, float[] ys) {
        for (int i = 0; i <= SEGMENTS; i++) {
            double t = (Math.PI * 2.0 * i) / SEGMENTS;
            double x = Math.cos(t) * radius;
            double y = Math.sin(t) * radius;
            context.mapToPixel(x, y, px);
            double sx = context.snapPixel(px[0]);
            double sy = context.snapPixel(px[1]);
            xs[i] = (float) sx;
            ys[i] = (float) sy;
        }
        canvas.drawPolyline(xs, ys, SEGMENTS + 1);
    }

    private double[] levels() {
        String spec = ChartAssets.getString(KEY_LEVELS, "");
        if (spec.equals(cachedSpec)) return cachedLevels;
        cachedSpec = spec;
        cachedLevels = parseLevels(spec);
        return cachedLevels;
    }

    private static double[] parseLevels(String spec) {
        if (spec == null || spec.isBlank()) return new double[0];
        String[] parts = spec.split(",");
        double[] values = new double[parts.length];
        int count = 0;
        for (String part : parts) {
            try {
                double v = Double.parseDouble(part.trim());
                if (Double.isFinite(v) && v > 1.0) {
                    values[count++] = v;
                }
            } catch (NumberFormatException ignore) {
                // Skip invalid VSWR levels.
            }
        }
        if (count == values.length) return values;
        double[] resized = new double[count];
        System.arraycopy(values, 0, resized, 0, count);
        return resized;
    }
}
