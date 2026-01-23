package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Professional, zero-allocation, high-precision step chart renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * @see BaseRenderer
 */
public final class StepRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];
    private final double[] p1 = new double[2];

    public StepRenderer() {
        super("step");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();

        g2.setStroke(getSeriesStroke());
        g2.setColor(getSeriesColor(model));

        Path2D path = getPathCache();
        context.mapToPixel(xData[0], yData[0], p0);
        path.moveTo(p0[0], p0[1]);

        for (int i = 1; i < n; i++) {
            context.mapToPixel(xData[i - 1], yData[i - 1], p0);
            context.mapToPixel(xData[i], yData[i], p1);
            path.lineTo(p1[0], p0[1]);
            path.lineTo(p1[0], p1[1]);
        }
        g2.draw(path);
    }
}
