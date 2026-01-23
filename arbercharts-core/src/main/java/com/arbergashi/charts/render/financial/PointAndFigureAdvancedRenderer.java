package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.tools.RendererAllocationCache;

import java.awt.*;
import java.util.List;

/**
 * Point &amp; Figure advanced renderer.
 *
 * <p>Simplified variant intended for demos. Uses a fixed box size and reversal setting to build
 * a classic Point &amp; Figure column chart.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class PointAndFigureAdvancedRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("point_and_figure_advanced", new RendererDescriptor("point_and_figure_advanced", "renderer.point_and_figure_advanced", "/icons/point_and_figure.svg"), PointAndFigureAdvancedRenderer::new);
    }

    public PointAndFigureAdvancedRenderer() {
        super("point_and_figure_advanced");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        double box = 1.0;

        List<Integer> columns = RendererAllocationCache.getList(this, "columns");
        double currentColBase = model.getY(0);
        int direction = 0; // 1 = up, -1 = down

        for (int idx = 0; idx < count; idx++) {
            double price = model.getY(idx);
            if (direction >= 0 && price >= currentColBase + box) {
                int boxes = (int) ((price - currentColBase) / box);
                for (int i = 0; i < boxes; i++) {
                    columns.add(1);
                }
                currentColBase += boxes * box;
                direction = 1;
            } else if (direction <= 0 && price <= currentColBase - box) {
                int boxes = (int) ((currentColBase - price) / box);
                for (int i = 0; i < boxes; i++) {
                    columns.add(-1);
                }
                currentColBase -= boxes * box;
                direction = -1;
            }
        }

        Rectangle bounds = context.plotBounds().getBounds();
        double w = bounds.getWidth();
        double colW = Math.max(6.0, w / Math.max(1, columns.size()));

        int i = 0;
        double[] buf = pBuffer();
        for (int v : columns) {
            double x = bounds.getX() + i * colW;
            context.mapToPixel(0, (v > 0) ? 1 : -1, buf);
            double y = buf[1];
            Shape rect = getRect(x, y - colW / 2.0, colW, colW);
            g2.setColor(v > 0 ? themeBullish(context) : themeBearish(context));
            g2.fill(rect);
            i++;
        }
    }
}
