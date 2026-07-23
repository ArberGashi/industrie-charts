package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.tools.RendererAllocationCache;

import java.util.List;
/**
 * Point &amp; Figure advanced renderer.
 *
 * <p>Simplified variant intended for demos. Uses a fixed box size and reversal setting to build
 * a classic Point &amp; Figure column chart.</p>
 *
 * @author Arber Gashi
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class PointAndFigureAdvancedRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("point_and_figure_advanced", new RendererDescriptor("point_and_figure_advanced", "renderer.point_and_figure_advanced", "/icons/point_and_figure.svg"), PointAndFigureAdvancedRenderer::new);
    }

    public PointAndFigureAdvancedRenderer() {
        super("point_and_figure_advanced");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        double box = deriveBoxSize(context);

        List<Integer> directions = RendererAllocationCache.getList(this, "pnf.directions");
        List<Double> levels = RendererAllocationCache.getList(this, "pnf.levels");
        directions.clear();
        levels.clear();

        double currentColBase = model.getY(0);
        int direction = 0; // 1 = up, -1 = down

        for (int idx = 0; idx < count; idx++) {
            double price = model.getY(idx);
            if (direction >= 0 && price >= currentColBase + box) {
                int boxes = (int) ((price - currentColBase) / box);
                for (int i = 0; i < boxes; i++) {
                    directions.add(1);
                    levels.add(currentColBase + (i + 1) * box);
                }
                currentColBase += boxes * box;
                direction = 1;
            } else if (direction <= 0 && price <= currentColBase - box) {
                int boxes = (int) ((currentColBase - price) / box);
                for (int i = 0; i < boxes; i++) {
                    directions.add(-1);
                    levels.add(currentColBase - (i + 1) * box);
                }
                currentColBase -= boxes * box;
                direction = -1;
            }
        }
        int boxCount = directions.size();
        if (boxCount == 0) return;

        int columnCount = 1;
        for (int i = 1; i < boxCount; i++) {
            if (!directions.get(i).equals(directions.get(i - 1))) columnCount++;
        }

        ArberRect bounds = context.getPlotBounds();
        double colW = Math.max(6.0, bounds.width() / Math.max(1, columnCount));

        double[] buf = pBuffer();
        context.mapToPixel(0, levels.get(0), buf);
        double y0 = buf[1];
        context.mapToPixel(0, levels.get(0) + box, buf);
        double boxH = Math.max(3.0, Math.abs(y0 - buf[1]));
        float half = (float) (Math.min(colW, boxH) * 0.4);

        canvas.setStroke(1.2f);
        int col = 0;
        int prev = directions.get(0);
        for (int i = 0; i < boxCount; i++) {
            int v = directions.get(i);
            if (v != prev) {
                col++;
                prev = v;
            }
            float x = (float) (bounds.x() + col * colW + colW * 0.5);
            context.mapToPixel(0, levels.get(i), buf);
            float y = (float) buf[1];
            if (v > 0) {
                canvas.setColor(themeBullish(context));
                drawCross(canvas, x, y, half);
            } else {
                canvas.setColor(themeBearish(context));
                canvas.drawRect(x - half, y - half, half * 2f, half * 2f);
            }
        }
    }

    private static void drawCross(ArberCanvas canvas, float x, float y, float half) {
        float[] xs = { x - half, x + half };
        float[] ys = { y - half, y + half };
        canvas.drawPolyline(xs, ys, 2);
        ys[0] = y + half;
        ys[1] = y - half;
        canvas.drawPolyline(xs, ys, 2);
    }

    private double deriveBoxSize(PlotContext context) {
        double range = context.getMaxY() - context.getMinY();
        if (Double.isFinite(range) && range > 0.0) {
            return range / 50.0;
        }
        return 1.0;
    }
}
