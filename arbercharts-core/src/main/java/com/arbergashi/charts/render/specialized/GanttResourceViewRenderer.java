package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

/**
 * Gantt Resource View: draws tasks as horizontal bars per resource row.
 * Input: ChartPoint.ofRange(x, y, min, max) where y is resource index or group id, min=start, max=end, label task name.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class GanttResourceViewRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("gantt_resource", new RendererDescriptor("gantt_resource", "renderer.gantt_resource", "/icons/gantt.svg"), GanttResourceViewRenderer::new);
    }

    public GanttResourceViewRenderer() {
        super("gantt_resource");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int count = model.getPointCount();
        if (count == 0) return;

        ArberRect bounds = context.getPlotBounds();
        double height = bounds.height();

        int maxResource = 1;
        for (int i = 0; i < count; i++) {
            int r = Math.max(0, (int) Math.round(model.getY(i)));
            if (r + 1 > maxResource) maxResource = r + 1;
        }

        double rowH = Math.max(12.0, height / Math.max(1, maxResource));
        canvas.setStroke(getSeriesStrokeWidth());

        double[] buf = pBuffer();
        for (int i = 0; i < count; i++) {
            double resourceVal = model.getY(i);
            int resource = Math.max(0, (int) Math.round(resourceVal));
            double start = model.getMin(i);
            double end = model.getMax(i);
            context.mapToPixel(start, 0, buf);
            double x1 = buf[0];
            context.mapToPixel(end, 0, buf);
            double x2 = buf[0];
            double y = bounds.y() + resource * rowH + rowH * 0.15;
            double h = rowH * 0.7;
            ArberColor barColor = seriesOrBase(model, context, i);
            canvas.setColor(barColor);
            canvas.fillRect((float) x1, (float) y, (float) Math.max(1.0, x2 - x1), (float) h);
        }
    }
}
