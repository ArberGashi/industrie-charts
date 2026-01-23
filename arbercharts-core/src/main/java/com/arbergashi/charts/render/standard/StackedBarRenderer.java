package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.ChartPoint;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;
import java.awt.geom.Rectangle2D;


/**
 * StackedBarRenderer.
 *
 * <p>Single-model stacked bar renderer: expects points to represent bar segments for the same x.
 * Uses {@link ChartPoint#label()} as segment key for color variation.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * @see ChartModel
 */
public final class StackedBarRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];
    private final double[] p1 = new double[2];

    public StackedBarRenderer() {
        super("stackedBar");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();

        Color base = getSeriesColor(model);

        double barW = Math.max(2.0, context.plotBounds().getWidth() / Math.max(1, n));

        final Rectangle2D viewBounds = context.plotBounds();

        // Stacking by identical x values
        int i = 0;
        while (i < n) {
            double currentX = xData[i];
            // Count group size
            int groupSize = 1;
            for (int j = i + 1; j < n && Double.compare(xData[j], currentX) == 0; j++) {
                groupSize++;
            }
            double acc = 0.0;
            for (int k = 0; k < groupSize; k++) {
                int idx = i + k;
                double y0 = acc;
                double y1 = acc + yData[idx];
                acc = y1;
                context.mapToPixel(currentX, y0, p0);
                context.mapToPixel(currentX, y1, p1);
                double x = p0[0] - barW / 2;
                double y = Math.min(p0[1], p1[1]);
                double h = Math.abs(p1[1] - p0[1]);
                if (h < 1.0) h = 1.0;
                if (x + barW < viewBounds.getMinX() || x > viewBounds.getMaxX()) continue;
                g2.setPaint(getCachedGradient(base, (float) h));
                g2.fill(getRect(x, y, barW, h));
            }
            i += groupSize;
        }

        g2.setColor(base);
        g2.setStroke(getCachedStroke((float) ChartScale.scale(1.0)));
    }
}
