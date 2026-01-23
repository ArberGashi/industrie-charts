package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Professional, zero-allocation, high-precision area chart renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class AreaRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    public AreaRenderer() {
        super("area");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int n0 = model.getPointCount();
        if (n0 < 2) return;

        final double[] xData = model.getXData();
        final double[] yData = model.getYData();
        final int n = Math.min(n0, Math.min(xData.length, yData.length));
        if (n < 2) return;

        Path2D path = getPathCache();
        path.reset();
        Rectangle2D bounds = context.plotBounds();

        // Find first finite point
        int first = -1;
        for (int i = 0; i < n; i++) {
            double x = xData[i];
            double y = yData[i];
            if (Double.isFinite(x) && Double.isFinite(y)) {
                first = i;
                break;
            }
        }
        if (first < 0) return;

        context.mapToPixel(xData[first], 0, p0);
        path.moveTo(p0[0], bounds.getY() + bounds.getHeight());

        context.mapToPixel(xData[first], yData[first], p0);
        path.lineTo(p0[0], p0[1]);

        int lastFinite = first;
        for (int i = first + 1; i < n; i++) {
            double x = xData[i];
            double y = yData[i];
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
            context.mapToPixel(x, y, p0);
            if (!Double.isFinite(p0[0]) || !Double.isFinite(p0[1])) continue;
            path.lineTo(p0[0], p0[1]);
            lastFinite = i;
        }

        if (lastFinite == first) return; // need at least 2 points to form an area

        context.mapToPixel(xData[lastFinite], 0, p0);
        path.lineTo(p0[0], bounds.getY() + bounds.getHeight());
        path.closePath();

        g2.setPaint(getCachedGradient(getSeriesColor(model), (float) bounds.getHeight()));
        g2.fill(path);

        g2.setStroke(getSeriesStroke());
        g2.setColor(getSeriesColor(model));

        Path2D topLine = getPathCache();
        topLine.reset();
        context.mapToPixel(xData[first], yData[first], p0);
        topLine.moveTo(p0[0], p0[1]);
        for (int i = first + 1; i <= lastFinite; i++) {
            double x = xData[i];
            double y = yData[i];
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
            context.mapToPixel(x, y, p0);
            if (!Double.isFinite(p0[0]) || !Double.isFinite(p0[1])) continue;
            topLine.lineTo(p0[0], p0[1]);
        }
        g2.draw(topLine);
    }
}
