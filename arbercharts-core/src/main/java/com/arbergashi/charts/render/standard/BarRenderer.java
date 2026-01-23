package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.MathUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Professional, zero-allocation, high-precision bar chart renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class BarRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    public BarRenderer() {
        super("bar");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int n0 = model.getPointCount();
        if (n0 == 0) return;

        final double[] xData = model.getXData();
        final double[] yData = model.getYData();
        final int n = Math.min(n0, Math.min(xData.length, yData.length));
        if (n == 0) return;

        Color seriesColor = getSeriesColor(model);
        double barWidth = Math.max(2.0, context.plotBounds().getWidth() / Math.max(1, n));

        context.mapToPixel(0, 0.0, p0);
        double baselineY = MathUtils.clamp(p0[1], context.plotBounds().getMinY(), context.plotBounds().getMaxY());

        final Shape clip = g2.getClip();
        final Rectangle2D viewBounds = clip != null ? clip.getBounds2D() : context.plotBounds();

        for (int i = 0; i < n; i++) {
            final double x = xData[i];
            final double y = yData[i];
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;

            context.mapToPixel(x, y, p0);
            if (!Double.isFinite(p0[0]) || !Double.isFinite(p0[1])) continue;

            double bx = p0[0] - barWidth / 2;
            if (bx + barWidth < viewBounds.getMinX() || bx > viewBounds.getMaxX()) {
                continue;
            }

            double by = Math.min(p0[1], baselineY);
            double height = Math.abs(p0[1] - baselineY);
            if (height < 1.0) height = 1.0;

            g2.setPaint(getCachedGradient(seriesColor, (float) height));
            Shape barShape = getRect(bx, by, barWidth, height);
            g2.fill(barShape);

            if (UIManager.getBoolean("Chart.bar.outline")) {
                g2.setStroke(getCachedStroke(ChartScale.scale(1.0f)));
                g2.setColor(seriesColor.darker());
                g2.draw(barShape);
            }
        }
    }
}
