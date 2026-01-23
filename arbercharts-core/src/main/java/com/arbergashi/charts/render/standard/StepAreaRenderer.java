package com.arbergashi.charts.render.standard;


import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.MathUtils;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Step-area renderer: like an area chart, but with step transitions (horizontal then vertical).
 * Very fast and well-suited for discrete measurements.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-15
 */
public final class StepAreaRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];
    private final double[] p1 = new double[2];

    public StepAreaRenderer() {
        super("stepArea");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int n0 = model.getPointCount();
        if (n0 < 2) return;

        final double[] xData = model.getXData();
        final double[] yData = model.getYData();
        final int n = Math.min(n0, Math.min(xData.length, yData.length));
        if (n < 2) return;

        Color c = getSeriesColor(model);

        context.mapToPixel(0, 0.0, p0);
        double zeroY = p0[1];
        double baselineY = MathUtils.clamp(zeroY, context.plotBounds().getY(), context.plotBounds().getMaxY());

        // Find first finite point.
        int first = -1;
        for (int i = 0; i < n; i++) {
            if (Double.isFinite(xData[i]) && Double.isFinite(yData[i])) {
                first = i;
                break;
            }
        }
        if (first < 0 || first + 1 >= n) return;

        Path2D area = getPathCache();
        area.reset();
        context.mapToPixel(xData[first], yData[first], p0);
        if (!Double.isFinite(p0[0]) || !Double.isFinite(p0[1])) return;
        area.moveTo(p0[0], baselineY);
        area.lineTo(p0[0], p0[1]);

        double lastX = xData[first];
        double lastY = yData[first];
        int lastFinite = first;

        for (int i = first + 1; i < n; i++) {
            double x = xData[i];
            double y = yData[i];
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;

            context.mapToPixel(lastX, lastY, p0);
            context.mapToPixel(x, y, p1);
            if (!Double.isFinite(p0[0]) || !Double.isFinite(p0[1]) || !Double.isFinite(p1[0]) || !Double.isFinite(p1[1])) {
                lastX = x;
                lastY = y;
                continue;
            }

            area.lineTo(p1[0], p0[1]);
            area.lineTo(p1[0], p1[1]);

            lastX = x;
            lastY = y;
            lastFinite = i;
        }

        if (lastFinite == first) return;

        context.mapToPixel(xData[lastFinite], yData[lastFinite], p0);
        if (!Double.isFinite(p0[0]) || !Double.isFinite(p0[1])) return;
        area.lineTo(p0[0], baselineY);
        area.closePath();

        float alpha = 0.22f;
        g2.setColor(com.arbergashi.charts.tools.RendererAllocationCache.getColor(this, "stepArea.fill", c.getRed(), c.getGreen(), c.getBlue(), (int) (alpha * 255)));
        g2.fill(area);

        g2.setStroke(getCachedStroke(ChartScale.scale(2.0f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(c);
        g2.draw(area);
    }
}
