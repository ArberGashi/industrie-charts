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
import com.arbergashi.charts.util.ColorUtils;

/**
 * Joyplot / Ridgeline renderer: draws stacked density-like curves.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class JoyplotRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("joyplot", new RendererDescriptor("joyplot", "renderer.joyplot", "/icons/joyplot.svg"), JoyplotRenderer::new);
    }

    private final double[] pBuffer = new double[2];

    public JoyplotRenderer() {
        super("joyplot");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        ArberRect bounds = context.getPlotBounds();
        double baseY = bounds.y() + bounds.height();

        int lines = Math.min(8, n / 20 + 1);
        ArberColor baseColor = getSeriesColor(model);

        int step = Math.max(1, n / 200);
        float[] xs = RendererAllocationCache.getFloatArray(this, "joy.x", (n / step) + 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "joy.y", (n / step) + 2);

        for (int row = 0; row < lines; row++) {
            int out = 0;
            int idx = 0;
            for (int i = 0; i < n; i++) {
                if ((idx++ % step) != 0) continue;
                context.mapToPixel(xData[i], yData[i], pBuffer);
                double x = pBuffer[0];
                double y = baseY - (row * (bounds.height() / (lines * 1.5))) - yData[i] * 0.1;
                xs[out] = (float) x;
                ys[out] = (float) y;
                out++;
            }
            if (out < 2) continue;
            ArberColor rowColor = isMultiColor() ? themeSeries(context, row) : baseColor;
            if (rowColor == null) rowColor = baseColor;
            canvas.setColor(ColorUtils.applyAlpha(rowColor, 0.9f));
            canvas.drawPolyline(xs, ys, out);
        }
    }

    public JoyplotRenderer setMultiColor(boolean enabled) {
        super.setMultiColor(enabled);
        return this;
    }
}
