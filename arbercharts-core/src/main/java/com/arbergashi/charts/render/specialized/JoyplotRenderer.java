package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Joyplot / Ridgeline renderer: draws stacked density-like curves.
 * Optimized to reuse Path2D and avoid allocations in the draw loop.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class JoyplotRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("joyplot", new RendererDescriptor("joyplot", "renderer.joyplot", "/icons/joyplot.svg"), JoyplotRenderer::new);
    }

    private final double[] pBuffer = new double[2];
    private transient Path2D.Double path;
    public JoyplotRenderer() {
        super("joyplot");
    }

    private Path2D.Double path() {
        if (path == null) path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        return path;
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        Rectangle2D bounds = context.plotBounds();
        double baseY = bounds.getY() + bounds.getHeight();

        // Draw several offset ridgelines using same data with slight vertical offsets
        int lines = Math.min(8, n / 20 + 1);
        Color baseColor = getSeriesColor(model);
        for (int row = 0; row < lines; row++) {
            Path2D.Double pth = path();
            pth.reset();
            boolean first = true;
            int step = Math.max(1, n / 200);
            int idx = 0;
            for (int i = 0; i < n; i++) {
                if ((idx++ % step) != 0) continue;
                context.mapToPixel(xData[i], yData[i], pBuffer);
                double x = pBuffer[0];
                double y = baseY - (row * (bounds.getHeight() / (lines * 1.5))) - yData[i] * 0.1;
                if (first) {
                    pth.moveTo(x, y);
                    first = false;
                } else pth.lineTo(x, y);
            }
            // Use ColorUtils for alpha to avoid new Color(rgb, hasAlpha) if possible, or just cache base
            Color rowColor = isMultiColor() ? themeSeries(context, row) : baseColor;
            if (rowColor == null) rowColor = baseColor;
            g2.setColor(ColorUtils.withAlpha(rowColor, 0.9f));
            g2.draw(pth);
        }
    }

    public JoyplotRenderer setMultiColor(boolean enabled) {
        super.setMultiColor(enabled);
        return this;
    }
}
