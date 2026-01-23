package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Professional, zero-allocation, high-precision spline chart renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * @see BaseRenderer
 */
public final class SplineRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];
    private final double[] p1 = new double[2];
    private final double[] p2 = new double[2];
    private final double[] p3 = new double[2];

    public SplineRenderer() {
        super("spline");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < 2) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();

        g2.setStroke(getSeriesStroke());
        g2.setColor(getSeriesColor(model));

        Path2D path = getPathCache();

        context.mapToPixel(xData[0], yData[0], p0);
        path.moveTo(p0[0], p0[1]);

        for (int i = 0; i < n - 1; i++) {
            int i0 = (i > 0 ? i - 1 : i);
            int i1 = i;
            int i2 = i + 1;
            int i3 = (i < n - 2 ? i + 2 : i + 1);
            context.mapToPixel(xData[i0], yData[i0], p0);
            context.mapToPixel(xData[i1], yData[i1], p1);
            context.mapToPixel(xData[i2], yData[i2], p2);
            context.mapToPixel(xData[i3], yData[i3], p3);

            double tension = 0.5;
            double c1x = p1[0] + (p2[0] - p0[0]) / 6.0 * tension;
            double c1y = p1[1] + (p2[1] - p0[1]) / 6.0 * tension;
            double c2x = p2[0] - (p3[0] - p1[0]) / 6.0 * tension;
            double c2y = p2[1] - (p3[1] - p1[1]) / 6.0 * tension;

            path.curveTo(c1x, c1y, c2x, c2y, p2[0], p2[1]);
        }
        g2.draw(path);
    }
}
