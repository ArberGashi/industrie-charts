package com.arbergashi.charts.render.analysis;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Marker overlay for minimum/maximum (y) values within a series.
 * Useful for sparklines and monitoring dashboards.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class MinMaxMarkerRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];
    private final Ellipse2D.Double ellipseCache = new Ellipse2D.Double();

    public MinMaxMarkerRenderer() {
        super("minMaxMarker");
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        int minIdx = 0;
        int maxIdx = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < count; i++) {
            double y = yData[i];
            if (y < min) {
                min = y;
                minIdx = i;
            }
            if (y > max) {
                max = y;
                maxIdx = i;
            }
        }

        double r = ChartScale.scale(ChartAssets.getFloat("chart.render.minmax.radius", 4.0f));
        final ChartTheme theme = resolveTheme(context);
        Color base = theme.getAxisLabelColor();
        Color minC = ColorUtils.withAlpha(base, 0.85f);
        Color maxC = ColorUtils.withAlpha(getSeriesColor(model), 0.9f);
        if (isMultiColor()) {
            Color minBase = themeSeries(context, 0);
            Color maxBase = themeSeries(context, 1);
            if (minBase != null) minC = ColorUtils.withAlpha(minBase, 0.85f);
            if (maxBase != null) maxC = ColorUtils.withAlpha(maxBase, 0.9f);
        }

        if (minIdx == maxIdx) {
            drawDot(g2, context, xData[minIdx], yData[minIdx], r, maxC);
            return;
        }

        if (ChartAssets.getBoolean("chart.render.minmax.showMin", true)) {
            drawDot(g2, context, xData[minIdx], yData[minIdx], r, minC);
        }
        if (ChartAssets.getBoolean("chart.render.minmax.showMax", true)) {
            drawDot(g2, context, xData[maxIdx], yData[maxIdx], r, maxC);
        }
    }

    private void drawDot(Graphics2D g2, PlotContext context, double x, double y, double r, Color c) {
        context.mapToPixel(x, y, pBuffer);
        g2.setColor(c);
        ellipseCache.setFrame(pBuffer[0] - r, pBuffer[1] - r, r * 2, r * 2);
        g2.fill(ellipseCache);
    }
}
