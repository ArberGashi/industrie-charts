package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

/**
 * GroupedBarRenderer.
 *
 * <p>Interprets points with identical x as bars within a group; uses label as subgroup key.
 * This renderer assumes the input data is already grouped/sorted by x.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class GroupedBarRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    public GroupedBarRenderer() {
        super("groupedBar");
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

        context.mapToPixel(0, 0.0, p0);
        final double baseline = p0[1];

        final ArberRect viewBounds = context.getPlotBounds();

        int i = 0;
        while (i < n) {
            double currentX = xData[i];
            int groupSize = 1;
            for (int j = i + 1; j < n && Double.compare(xData[j], currentX) == 0; j++) {
                groupSize++;
            }
            double groupW = Math.max(2.0, viewBounds.width() / Math.max(1, n));
            double barW = groupW / Math.max(1, groupSize);
            for (int k = 0; k < groupSize; k++) {
                int idx = i + k;
                context.mapToPixel(currentX, yData[idx], p0);
                double xCenter = p0[0] - groupW / 2 + barW * (k + 0.5);
                double y = Math.min(p0[1], baseline);
                double h = Math.abs(p0[1] - baseline);
                if (h < 1.0) h = 1.0;
                double x = xCenter - barW / 2;
                if (x + barW < viewBounds.minX() || x > viewBounds.maxX()) {
                    continue;
                }
                canvas.setColor(base);
                canvas.fillRect((float) x, (float) y, (float) barW, (float) h);
            }
            i += groupSize;
        }
    }
}
