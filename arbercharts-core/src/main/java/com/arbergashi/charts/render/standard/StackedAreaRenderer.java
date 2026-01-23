package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.MathUtils;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * StackedAreaRenderer.
 *
 * <p>Lightweight stacked area implementation.
 * Points are expected to encode multiple series at the same x by using label as series key.
 * (For a multi-model stack, provide pre-aggregated totals as one model.)</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class StackedAreaRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    public StackedAreaRenderer() {
        super("stackedArea");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < 2) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();

        Path2D area = getPathCache();
        boolean started = false;

        context.mapToPixel(0, 0.0, p0);
        double yBase = MathUtils.clamp(p0[1], context.plotBounds().getY(), context.plotBounds().getMaxY());

        double xLast = 0;

        final Rectangle2D view = context.plotBounds();

        for (int i = 0; i < n; i++) {
            context.mapToPixel(xData[i], yData[i], p0);
            if (!started) {
                if (p0[0] < view.getMinX() - 5 || p0[0] > view.getMaxX() + 5) {
                    continue;
                }
                area.moveTo(p0[0], yBase);
                area.lineTo(p0[0], p0[1]);
                started = true;
            } else {
                area.lineTo(p0[0], p0[1]);
            }
            xLast = p0[0];
        }

        if (!started) return;

        area.lineTo(xLast, yBase);
        area.closePath();

        Color base = getSeriesColor(model);
        g2.setPaint(getCachedGradient(base, (float) context.plotBounds().getHeight()));
        g2.fill(area);

        g2.setColor(base);
        g2.setStroke(getSeriesStroke());
        g2.draw(area);
    }
}
