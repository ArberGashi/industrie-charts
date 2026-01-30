package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ColorRegistry;
import com.arbergashi.charts.util.ColorUtils;

/**
 * Horizon chart renderer: compact, multi-band representation of a time series.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class HorizonRenderer extends BaseRenderer {

    private static final int MAX_BANDS = 4;

    static {
        RendererRegistry.register("horizon", new RendererDescriptor("horizon", "renderer.horizon", "/icons/horizon.svg"), HorizonRenderer::new);
    }

    private final double[] map = new double[2];

    public HorizonRenderer() {
        super("horizon");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n < 2) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();
        int len = Math.min(n, Math.min(xData.length, yData.length));
        if (len < 2) return;

        ArberRect bounds = context.getPlotBounds();
        double midY = bounds.y() + bounds.height() * 0.5;
        double halfH = Math.max(1.0, bounds.height() * 0.5);
        double bandH = bounds.height() / MAX_BANDS;

        ArberColor base = seriesOrBase(model, context, 0);
        ArberColor negBase = ColorRegistry.adjustBrightness(base, 0.7);
        ArberColor[] pos = (ArberColor[]) RendererAllocationCache.getArray(this, "horizon.pos", ArberColor.class, MAX_BANDS);
        ArberColor[] neg = (ArberColor[]) RendererAllocationCache.getArray(this, "horizon.neg", ArberColor.class, MAX_BANDS);
        for (int i = 0; i < MAX_BANDS; i++) {
            float a = 0.22f + 0.18f * i;
            pos[i] = ColorUtils.applyAlpha(base, a);
            neg[i] = ColorUtils.applyAlpha(negBase, a);
        }

        float[] rect = RendererAllocationCache.getFloatArray(this, "horizon.rect", 4);

        for (int i = 0; i < len - 1; i++) {
            context.mapToPixel(xData[i], yData[i], map);
            double x0 = map[0];
            double py = map[1];
            context.mapToPixel(xData[i + 1], yData[i + 1], map);
            double x1 = map[0];
            if (x1 < x0) {
                double t = x0;
                x0 = x1;
                x1 = t;
            }
            double width = Math.max(1.0, x1 - x0);

            double norm = (midY - py) / halfH; // positive -> above midline
            double abs = Math.min(1.0, Math.abs(norm));
            int level = (int) Math.ceil(abs * MAX_BANDS);
            if (level <= 0) continue;
            if (level > MAX_BANDS) level = MAX_BANDS;

            if (norm >= 0) {
                for (int b = 1; b <= level; b++) {
                    double yTop = midY - b * bandH;
                    rect[0] = (float) x0;
                    rect[1] = (float) yTop;
                    rect[2] = (float) width;
                    rect[3] = (float) bandH;
                    canvas.setColor(pos[b - 1]);
                    canvas.fillRect(rect[0], rect[1], rect[2], rect[3]);
                }
            } else {
                for (int b = 1; b <= level; b++) {
                    double yTop = midY + (b - 1) * bandH;
                    rect[0] = (float) x0;
                    rect[1] = (float) yTop;
                    rect[2] = (float) width;
                    rect[3] = (float) bandH;
                    canvas.setColor(neg[b - 1]);
                    canvas.fillRect(rect[0], rect[1], rect[2], rect[3]);
                }
            }
        }
    }
}
