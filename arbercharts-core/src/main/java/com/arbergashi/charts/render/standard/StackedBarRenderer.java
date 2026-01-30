package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

/**
 * StackedBarRenderer.
 *
 * <p>Single-model stacked bar renderer: expects points to represent bar segments for the same x.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class StackedBarRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];
    private final double[] p1 = new double[2];

    public StackedBarRenderer() {
        super("stackedBar");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();

        ArberColor base = getSeriesColor(model);
        ArberRect viewBounds = context.getPlotBounds();

        double barW = Math.max(2.0, viewBounds.width() / Math.max(1, n));

        int i = 0;
        while (i < n) {
            double currentX = xData[i];
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
                if (x + barW < viewBounds.minX() || x > viewBounds.maxX()) continue;
                canvas.setColor(base);
                canvas.fillRect((float) x, (float) y, (float) barW, (float) h);
            }
            i += groupSize;
        }
    }
}
