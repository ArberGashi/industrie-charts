package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.MathUtils;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Area renderer relative to a configurable baseline.
 * Key: {@code chart.render.baseline.value} (default 0.0).
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-15
 */
public final class BaselineAreaRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    public BaselineAreaRenderer() {
        super("baselineArea");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < 2) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();

        double baselineValue = 0.0;
        try {
            baselineValue = Double.parseDouble(com.arbergashi.charts.util.ChartAssets.getString("chart.render.baseline.value", "0.0"));
        } catch (Exception ignored) {
        }
        context.mapToPixel(0, baselineValue, p0);
        double baselineY = MathUtils.clamp(p0[1], context.plotBounds().getY(), context.plotBounds().getMaxY());

        Color c = getSeriesColor(model);

        Path2D area = getPathCache();
        area.reset();
        area.moveTo(xData[0], baselineY);
        for (int i = 0; i < n; i++) {
            context.mapToPixel(xData[i], yData[i], p0);
            area.lineTo(p0[0], p0[1]);
        }
        context.mapToPixel(xData[n - 1], baselineY, p0);
        area.lineTo(p0[0], p0[1]);
        area.closePath();

        g2.setPaint(getCachedGradient(c, (float) context.plotBounds().getHeight()));
        g2.fill(area);

        g2.setStroke(getSeriesStroke());
        g2.setColor(c);
        g2.draw(area);
    }
}
