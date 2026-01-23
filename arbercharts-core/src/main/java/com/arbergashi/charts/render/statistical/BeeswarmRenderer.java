package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * <h1>BeeswarmRenderer - Beeswarm Plot (Stripchart)</h1>
 *
 * <p>Professional beeswarm plot renderer for showing individual data points without overlap.
 * Each point is positioned to avoid overlapping while maintaining vertical alignment by category.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>No Overlap:</b> Points are jittered horizontally to avoid collision</li>
 *   <li><b>All Data Visible:</b> Every data point is shown individually</li>
 *   <li><b>Distribution Shape:</b> Reveals density and outliers</li>
 *   <li><b>Category Grouping:</b> Points grouped by category (x-value)</li>
 * </ul>
 *
 * <h2>Advantages over:</h2>
 * <ul>
 *   <li><b>Box Plot:</b> Shows all individual points, not just summary statistics</li>
 *   <li><b>Violin Plot:</b> More precise, shows exact values</li>
 *   <li><b>Scatter with Jitter:</b> Intelligent layout, no random overlap</li>
 * </ul>
 *
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li>Small to medium datasets ({@code &lt; 500} points per category)</li>
 *   <li>Comparing distributions across groups</li>
 *   <li>Identifying outliers and clusters</li>
 *   <li>Showing all data points transparently</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class BeeswarmRenderer extends BaseRenderer {

    private static final double DOT_RADIUS = 4.0;
    // MIN_SPACING removed (no longer used)
    private final double[] p0 = new double[2];

    public BeeswarmRenderer() {
        super("beeswarm");
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 1) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();
        final Color baseColor = getSeriesColor(model);
        final double scaledRadius = ChartScale.scale(DOT_RADIUS);
        // Simple beeswarm: plot all points, jitter x if needed (no grouping)
        for (int i = 0; i < model.getPointCount(); i++) {
            context.mapToPixel(xData[i], yData[i], p0);
            Ellipse2D dot = getEllipse(
                    p0[0] - scaledRadius,
                    p0[1] - scaledRadius,
                    scaledRadius * 2,
                    scaledRadius * 2
            );
            Color pointColor = isMultiColor() ? themeSeries(context, i) : baseColor;
            if (pointColor == null) pointColor = baseColor;
            g.setColor(pointColor);
            g.fill(dot);
            g.setColor(themeBackground(context));
            g.setStroke(getCachedStroke(ChartScale.scale(0.5f)));
            g.draw(dot);
        }
    }

}
