package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.MathUtils;

import java.awt.*;

/**
 * <h1>ColumnRenderer - Vertical Column Chart</h1>
 *
 * <p>Professional column chart renderer for categorical data visualization.
 * Displays vertical columns with gradient fill and shadows.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>Vertical Orientation:</b> Columns grow upward from baseline</li>
 *   <li><b>Gradient Fill:</b> Professional depth visualization</li>
 *   <li><b>Negative Support:</b> Columns below baseline for negative values</li>
 *   <li><b>Spacing Control:</b> Automatic column width based on data density</li>
 *   <li><b>Zero-Allocation:</b> Shape pooling for performance</li>
 * </ul>
 *
 * <h2>Differences from BarRenderer:</h2>
 * <ul>
 *   <li>BarRenderer: Horizontal bars (good for long category names)</li>
 *   <li>ColumnRenderer: Vertical columns (good for time series, comparisons)</li>
 * </ul>
 *
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li>Sales by quarter</li>
 *   <li>Revenue comparisons</li>
 *   <li>Survey results</li>
 *   <li>Statistical distributions</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class ColumnRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];

    public ColumnRenderer() {
        super("column");
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 1) return;

        final Color baseColor = getSeriesColor(model);
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        // More stable width logic (DPI-safe) with a sensible minimum.
        final double columnWidth = Math.max(ChartScale.scale(2.0), (context.plotBounds().getWidth() / (double) count) * 0.7);

        // Find zero baseline
        context.mapToPixel(0, 0.0, p0);
        final double baselineY = MathUtils.clamp(p0[1], context.plotBounds().getY(), context.plotBounds().getMaxY());

        final Rectangle viewBounds = g.getClipBounds() != null ? g.getClipBounds() : context.plotBounds().getBounds();

        for (int i = 0; i < count; i++) {
            context.mapToPixel(xData[i], yData[i], p0);

            final double x = p0[0] - columnWidth / 2.0;
            final double columnHeight = Math.abs(p0[1] - baselineY);
            if (columnHeight < 1.0) continue;

            final double y = Math.min(p0[1], baselineY);

            if (!viewBounds.intersects(x, y, columnWidth, columnHeight)) {
                continue;
            }

            Shape column = getRect(x, y, columnWidth, columnHeight);

            // Draw gradient fill
            g.setPaint(getCachedGradient(baseColor, (float) columnHeight));
            g.fill(column);

            // Draw border
            g.setColor(baseColor);
            g.setStroke(getCachedStroke(ChartScale.scale(1.0f)));
            g.draw(column);
        }
    }
}
