package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Professional, zero-allocation, high-precision scatter plot renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class ScatterRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    public ScatterRenderer() {
        super("scatter");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int n0 = model.getPointCount();
        if (n0 == 0) return;

        final double[] xData = model.getXData();
        final double[] yData = model.getYData();
        final int n = Math.min(n0, Math.min(xData.length, yData.length));
        if (n == 0) return;

        g2.setColor(getSeriesColor(model));
        double size = ChartScale.scale(4.0);
        double halfSize = size / 2.0;

        final Shape clip = g2.getClip();
        final Rectangle2D viewBounds = clip != null ? clip.getBounds2D() : context.plotBounds();

        for (int i = 0; i < n; i++) {
            final double x = xData[i];
            final double y = yData[i];
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;

            context.mapToPixel(x, y, p0);
            if (!Double.isFinite(p0[0]) || !Double.isFinite(p0[1])) continue;

            if (p0[0] < viewBounds.getMinX() - halfSize || p0[0] > viewBounds.getMaxX() + halfSize ||
                    p0[1] < viewBounds.getMinY() - halfSize || p0[1] > viewBounds.getMaxY() + halfSize) {
                continue;
            }

            g2.fill(getEllipse(p0[0] - halfSize, p0[1] - halfSize, size, size));
        }
    }
}
