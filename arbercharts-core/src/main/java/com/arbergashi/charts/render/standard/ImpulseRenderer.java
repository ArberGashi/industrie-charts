package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.MathUtils;

import java.awt.*;

/**
 * Impulse/Stem Plot Renderer: line from a baseline to each point plus an optional dot.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class ImpulseRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    public ImpulseRenderer() {
        super("impulse");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        // BaseRenderer.render() already applies quality hints and clip guards.
        int count = model.getPointCount();
        if (count < 1) return;

        Color c = getSeriesColor(model);
        double baselineValue = 0.0;
        String baselineStr = ChartAssets.getString("chart.render.impulse.baseline", "0");
        try {
            baselineValue = Double.parseDouble(baselineStr.trim().replace(',', '.'));
        } catch (Exception ignored) {
        }

        context.mapToPixel(0, baselineValue, p0);
        double baselineY = MathUtils.clamp(p0[1], context.plotBounds().getY(), context.plotBounds().getMaxY());

        float w = ChartAssets.getFloat("chart.render.impulse.width", 1.2f);
        float sw = ChartScale.scale(w);
        g2.setStroke(getCachedStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(c);

        boolean showDot = ChartAssets.getBoolean("chart.render.impulse.dot", true);
        double r = ChartScale.scale(ChartAssets.getFloat("chart.render.impulse.radius", 2.8f));

        final Rectangle viewBounds = g2.getClipBounds() != null ? g2.getClipBounds() : context.plotBounds().getBounds();

        if (ChartAssets.getBoolean("chart.render.impulse.showBaseline", false)) {
            g2.setColor(themeGrid(context));
            g2.draw(getLine(context.plotBounds().getX(), baselineY, context.plotBounds().getMaxX(), baselineY));
            g2.setColor(c);
        }

        final double pad = Math.max(r, sw);

        double[] xData = model.getXData();
        double[] yData = model.getYData();
        for (int i = 0; i < count; i++) {
            context.mapToPixel(xData[i], yData[i], p0);
            double x = p0[0];
            double y = p0[1];

            double minY = Math.min(baselineY, y);
            double maxY = Math.max(baselineY, y);

            if (!viewBounds.intersects(x - pad, minY, pad * 2.0, maxY - minY)) continue;

            g2.draw(getLine(x, baselineY, x, y));
            if (showDot) {
                g2.fill(getEllipse(x - r, y - r, r * 2, r * 2));
            }
        }
    }
}
