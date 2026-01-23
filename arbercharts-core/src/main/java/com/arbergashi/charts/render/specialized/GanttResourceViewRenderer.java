package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;

/**
 * Gantt Resource View: draws tasks as horizontal bars per resource row.
 * Input: ChartPoint.ofRange(x, y, min, max) where y is resource index or group id, min=start, max=end, label task name.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class GanttResourceViewRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("gantt_resource", new RendererDescriptor("gantt_resource", "renderer.gantt_resource", "/icons/gantt.svg"), GanttResourceViewRenderer::new);
    }

    public GanttResourceViewRenderer() {
        super("gantt_resource");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int count = model.getPointCount();
        if (count == 0) return;

        Rectangle bounds = context.plotBounds().getBounds();
        double height = bounds.getHeight();

        // compute number of resources (y as integer indices)
        int maxResource = 1;
        for (int i = 0; i < count; i++) {
            int r = Math.max(0, (int) Math.round(model.getY(i)));
            if (r + 1 > maxResource) maxResource = r + 1;
        }

        double rowH = Math.max(12.0, height / Math.max(1, maxResource));
        g2.setStroke(getSeriesStroke());

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
            double y = bounds.getY() + resource * rowH + rowH * 0.15;
            double h = rowH * 0.7;
            Shape rect = getRect(x1, y, Math.max(1.0, x2 - x1), h);
            Color barColor = seriesOrBase(model, context, i);
            g2.setColor(barColor);
            g2.fill(rect);
            // small label
            String label = model.getLabel(i);
            if (label != null && !label.isBlank()) {
                drawLabel(g2, label, g2.getFont(), themeForeground(context), (float) x1 + 4f, (float) (y + h / 2));
            }
        }
    }
}
