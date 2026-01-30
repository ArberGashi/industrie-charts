package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * Professional, zero-allocation Gantt chart renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class GanttRenderer extends BaseRenderer {

    private final double[] pxStart = new double[2];
    private final double[] pxEnd = new double[2];

    public GanttRenderer() {
        super("gantt");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        final ArberColor accent = getSeriesColor(model);
        final ArberRect viewBounds = context.getPlotBounds();
        final double barHeight = Math.min(ChartScale.scale(24.0), (viewBounds.height() / n) * 0.65);
        final double minBarWidth = ChartScale.scale(4.0);
        final float borderStroke = ChartScale.scale(1.0f);

        for (int i = 0; i < n; i++) {
            double taskIndex = model.getY(i);
            double start = model.getX(i);
            double duration = model.getValue(i, 2); // weight

            context.mapToPixel(start, taskIndex, pxStart);
            context.mapToPixel(start + duration, taskIndex, pxEnd);

            final double x = pxStart[0];
            final double y = pxStart[1] - (barHeight / 2.0);
            final double width = Math.max(pxEnd[0] - pxStart[0], minBarWidth);
            if (x + width < viewBounds.minX() || x > viewBounds.maxX()) continue;
            if (y + barHeight < viewBounds.minY() || y > viewBounds.maxY()) continue;

            canvas.setColor(accent);
            canvas.fillRect((float) x, (float) y, (float) width, (float) barHeight);

            if (width > 5) {
                canvas.setColor(ColorRegistry.applyAlpha(accent, 0.8f));
                canvas.setStroke(borderStroke);
                canvas.drawRect((float) x, (float) y, (float) width, (float) barHeight);
            }
        }
    }

    // This renderer can use the default getPointAt from BaseRenderer, 
    // as it's based on the center of the task (start + duration/2).
    // For more precise bar-only hit-testing, a custom implementation would be needed,
    // but the default is a good approximation.
}
