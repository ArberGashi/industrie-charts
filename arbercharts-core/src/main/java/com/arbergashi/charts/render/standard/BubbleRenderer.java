package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Professional, zero-allocation, high-precision bubble chart renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class BubbleRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    public BubbleRenderer() {
        super("bubble");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();
        double[] weightData = yData; // Fallback: use yData as weights if no dedicated array

        g2.setColor(getSeriesColor(model));

        double maxWeight = 1.0;
        for (int i = 0; i < n; i++) {
            double w = weightData[i];
            if (w > maxWeight) maxWeight = w;
        }

        final Shape clip = g2.getClip();
        final Rectangle2D viewBounds = clip != null ? clip.getBounds2D() : context.plotBounds();
        double maxBubbleSize = ChartScale.scale(50.0);

        for (int i = 0; i < n; i++) {
            double weight = weightData[i];
            double size = (weight / maxWeight) * maxBubbleSize;
            if (size < 2) size = 2;
            double halfSize = size / 2.0;

            context.mapToPixel(xData[i], yData[i], p0);

            if (p0[0] < viewBounds.getMinX() - halfSize || p0[0] > viewBounds.getMaxX() + halfSize ||
                    p0[1] < viewBounds.getMinY() - halfSize || p0[1] > viewBounds.getMaxY() + halfSize) {
                continue;
            }

            g2.fill(getEllipse(p0[0] - halfSize, p0[1] - halfSize, size, size));
        }
    }
}
