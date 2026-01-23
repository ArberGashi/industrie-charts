package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.TernaryChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Renders a Ternary Plot for 3-component data.
 * It expects a TernaryChartModel.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class TernaryPlotRenderer extends BaseRenderer {

    public TernaryPlotRenderer() {
        super("ternary_basic");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        if (!(model instanceof TernaryChartModel ternaryModel)) {
            drawErrorMessage(g2, context, "TernaryPlotRenderer requires a TernaryChartModel");
            return;
        }

        List<TernaryChartModel.TernaryPoint> data = ternaryModel.getTernaryData();
        if (data.isEmpty()) return;

        // Ternary plot geometry
        Rectangle2D bounds = context.plotBounds();
        double ax = bounds.getCenterX();
        double ay = bounds.getY();
        double bx = bounds.getX();
        double by = bounds.getMaxY();
        double cx = bounds.getMaxX();
        double cy = bounds.getMaxY();

        // Draw grid/axes (simplified)
        g2.setColor(themeGrid(context));
        g2.drawLine((int) ax, (int) ay, (int) bx, (int) by);
        g2.drawLine((int) bx, (int) by, (int) cx, (int) cy);
        g2.drawLine((int) cx, (int) cy, (int) ax, (int) ay);

        // Draw data points
        Color base = seriesOrBase(model, context, 0);
        for (int i = 0; i < data.size(); i++) {
            TernaryChartModel.TernaryPoint p = data.get(i);
            double sum = p.getA() + p.getB() + p.getC();
            if (sum == 0) continue;

            double normA = p.getA() / sum;
            double normB = p.getB() / sum;
            double normC = p.getC() / sum;

            // Barycentric coordinates to Cartesian
            double x = ax * normA + bx * normB + cx * normC;
            double y = ay * normA + by * normB + cy * normC;

            Color pointColor = isMultiColor() ? themeSeries(context, i) : base;
            if (pointColor == null) pointColor = base;
            g2.setColor(pointColor);
            g2.fill(getEllipse(x - 2, y - 2, 4, 4));
        }
    }

    private void drawErrorMessage(Graphics2D g2, PlotContext context, String message) {
        g2.setColor(themeSeries(context, 0));
        Rectangle bounds = context.plotBounds().getBounds();
        g2.drawString(message, bounds.x + 10, bounds.y + 20);
    }
}
