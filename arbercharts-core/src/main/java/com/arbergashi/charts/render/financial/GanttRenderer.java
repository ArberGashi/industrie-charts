package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Optional;

/**
 * Professional, zero-allocation Gantt chart renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class GanttRenderer extends BaseRenderer {

    private final double[] pxStart = new double[2];
    private final double[] pxEnd = new double[2];

    public GanttRenderer() {
        super("gantt");
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        final Color accent = getSeriesColor(model);
        final double barHeight = Math.min(ChartScale.scale(24.0), (context.plotBounds().getHeight() / n) * 0.65);
        final float arc = (float) ChartScale.scale(8.0);

        final Font baseFont = Optional.ofNullable(UIManager.getFont("Chart.font")).orElse(UIManager.getFont("Label.font"));
        // reserved for future task labeling; keep base font resolved for consistent typography

        final Rectangle2D viewBounds = g.getClipBounds() != null ? g.getClipBounds() : context.plotBounds();
        final double minBarWidth = ChartScale.scale(4.0);
        final Stroke borderStroke = getCachedStroke(ChartScale.scale(1.0f));

        for (int i = 0; i < n; i++) {
            double taskIndex = model.getY(i);
            double start = model.getX(i);
            double duration = model.getValue(i, 2); // weight

            context.mapToPixel(start, taskIndex, pxStart);
            context.mapToPixel(start + duration, taskIndex, pxEnd);

            final double x = pxStart[0];
            final double y = pxStart[1] - (barHeight / 2.0);
            final double width = Math.max(pxEnd[0] - pxStart[0], minBarWidth);
            if (!viewBounds.intersects(x, y, width, barHeight)) continue;

            final RoundRectangle2D taskBar = getRoundRectangle(x, y, width, barHeight, arc, arc);
            g.setColor(accent);
            g.fill(taskBar);

            if (width > 5) {
                g.setColor(ColorUtils.withAlpha(accent, 0.8f));
                g.setStroke(borderStroke);
                g.draw(taskBar);
            }
        }
    }

    // This renderer can use the default getPointAt from BaseRenderer, 
    // as it's based on the center of the task (start + duration/2).
    // For more precise bar-only hit-testing, a custom implementation would be needed,
    // but the default is a good approximation.
}
