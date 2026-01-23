package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Professional, zero-allocation, high-precision line chart renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class LineRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];
    private final double[] prevBuf = new double[2];
    private final double[] currBuf = new double[2];

    public LineRenderer() {
        super("line");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int n0 = model.getPointCount();
        if (n0 < 2) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();

        // Framework safety: some models return backing arrays larger than the logical point count.
        // Always bound iteration to the minimum available length.
        final int n = Math.min(n0, Math.min(xData.length, yData.length));
        if (n < 2) return;

        g2.setStroke(getSeriesStroke());
        g2.setColor(getSeriesColor(model));

        Path2D path = getPathCache();
        final Rectangle2D viewBounds = g2.getClip() != null ? g2.getClip().getBounds2D() : context.plotBounds();
        boolean hasMoved = false;

        for (int i = 0; i < n; i++) {
            context.mapToPixel(xData[i], yData[i], p0);

            // Simple culling logic
            if (i > 0) {
                double prevX = xData[i - 1];
                double currX = xData[i];
                context.mapToPixel(prevX, 0, prevBuf);
                context.mapToPixel(currX, 0, currBuf);
                if (prevBuf[0] > viewBounds.getMaxX() && currBuf[0] > viewBounds.getMaxX()) {
                    continue;
                }
            }

            if (!hasMoved) {
                path.moveTo(p0[0], p0[1]);
                hasMoved = true;
            } else {
                path.lineTo(p0[0], p0[1]);
            }
        }
        g2.draw(path);
    }
}
