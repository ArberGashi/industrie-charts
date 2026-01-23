package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;

/**
 * Heatmap Contour Renderer (JDK 25 Standard).
 * Displays temperature or concentration distributions using a grid of colored rectangles.
 * Professional Implementation: Clean code, High-DPI, Zero-Warnings.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
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

    public void setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
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
            Color c = getColorForIntensity(intensity, context);
            g2.setColor(c);

            double x1 = x - avgStepX / 2.0;
            double y1 = y - avgStepY / 2.0;

            context.mapToPixel(x1, y1 + avgStepY, pPixel);
            context.mapToPixel(x + avgStepX / 2.0, y1, pSize);

            double w = Math.abs(pSize[0] - pPixel[0]);
            double h = Math.abs(pSize[1] - pPixel[1]);

            g2.fill(getRect(pPixel[0], pPixel[1], Math.max(1.0, w), Math.max(1.0, h)));
        }
    }

    private Color getColorForIntensity(double intensity, PlotContext context) {
        Color c0;
        Color c1;
        Color c2;
        Color c3;
        if (isMultiColor()) {
            c0 = themeSeries(context, 0);
            c1 = themeSeries(context, 1);
            c2 = themeSeries(context, 2);
            c3 = themeSeries(context, 3);
        } else {
            Color base = themeAccent(context);
            c0 = ColorUtils.withAlpha(base, 0.25f);
            c1 = ColorUtils.withAlpha(base, 0.45f);
            c2 = ColorUtils.withAlpha(base, 0.65f);
            c3 = ColorUtils.withAlpha(base, 0.85f);
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
