package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorUtils;

/**
 * Heatmap Contour Renderer (headless).
 * Displays temperature or concentration distributions using a grid of colored rectangles.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public class HeatmapContourRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("heatmap_contour", new RendererDescriptor("heatmap_contour", "renderer.heatmap_contour", "/icons/heatmap.svg"), HeatmapContourRenderer::new);
    }

    private boolean interpolate = true;

    public HeatmapContourRenderer() {
        super("heatmap_contour");
    }

    public boolean isInterpolate() {
        return interpolate;
    }

    public HeatmapContourRenderer setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
        return this;
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < count; i++) {
            double v = model.getWeight(i);
            if (v < minZ) minZ = v;
            if (v > maxZ) maxZ = v;
        }
        double rangeZ = Math.max(1e-10, maxZ - minZ);

        double avgStepX = context.rangeX() / Math.sqrt(Math.max(1, count));
        double avgStepY = context.rangeY() / Math.sqrt(Math.max(1, count));

        double[] pPixel = pBuffer();
        double[] pSize = pBuffer4();

        for (int i = 0; i < count; i++) {
            double x = model.getX(i);
            double y = model.getY(i);
            double intensity = (model.getWeight(i) - minZ) / rangeZ;
            ArberColor c = getColorForIntensity(intensity, context);
            canvas.setColor(c);

            double x1 = x - avgStepX / 2.0;
            double y1 = y - avgStepY / 2.0;

            context.mapToPixel(x1, y1 + avgStepY, pPixel);
            context.mapToPixel(x + avgStepX / 2.0, y1, pSize);

            double w = Math.abs(pSize[0] - pPixel[0]);
            double h = Math.abs(pSize[1] - pPixel[1]);

            canvas.fillRect((float) pPixel[0], (float) pPixel[1], (float) Math.max(1.0, w), (float) Math.max(1.0, h));
        }
    }

    private ArberColor getColorForIntensity(double intensity, PlotContext context) {
        ArberColor c0;
        ArberColor c1;
        ArberColor c2;
        ArberColor c3;
        if (isMultiColor()) {
            c0 = themeSeries(context, 0);
            c1 = themeSeries(context, 1);
            c2 = themeSeries(context, 2);
            c3 = themeSeries(context, 3);
        } else {
            ArberColor base = themeAccent(context);
            c0 = ColorUtils.applyAlpha(base, 0.25f);
            c1 = ColorUtils.applyAlpha(base, 0.45f);
            c2 = ColorUtils.applyAlpha(base, 0.65f);
            c3 = ColorUtils.applyAlpha(base, 0.85f);
        }
        if (intensity <= 0) return c0;
        if (intensity >= 1) return c3;

        if (intensity < 0.33) {
            return ColorUtils.interpolate(c0, c1, (float) (intensity / 0.33));
        } else if (intensity < 0.66) {
            return ColorUtils.interpolate(c1, c2, (float) ((intensity - 0.33) / 0.33));
        } else {
            return ColorUtils.interpolate(c2, c3, (float) ((intensity - 0.66) / 0.34));
        }
    }
}
