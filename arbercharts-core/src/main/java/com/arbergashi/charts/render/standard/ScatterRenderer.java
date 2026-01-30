package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.TooltipContentProvider;
import com.arbergashi.charts.render.TooltipContext;
import com.arbergashi.charts.render.TooltipValueWriter;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;

/**
 * Professional, zero-allocation, high-precision scatter plot renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class ScatterRenderer extends BaseRenderer implements TooltipContentProvider {

    private final double[] p0 = new double[2];

    public ScatterRenderer() {
        super("scatter");
    }

    @Override
    public void getContent(StringBuilder target, TooltipContext ctx) {
        if (target == null || ctx == null) return;
        int idx = ctx.getIndex();
        if (idx < 0 || idx >= ctx.getModel().getPointCount()) return;
        double xVal = ctx.getModel().getX(idx);
        double yVal = ctx.getModel().getY(idx);
        target.append("X: ");
        TooltipValueWriter.appendAxisValue(target, ctx.getXAxis(), xVal);
        target.append('\n');
        target.append("Y: ");
        TooltipValueWriter.appendAxisValue(target, ctx.getYAxis(), yVal);
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n0 = model.getPointCount();
        if (n0 == 0) return;

        final double[] xData = model.getXData();
        final double[] yData = model.getYData();
        final int n = Math.min(n0, Math.min(xData.length, yData.length));
        if (n == 0) return;

        ArberColor color = getSeriesColor(model);
        double size = ChartScale.scale(4.0);
        double halfSize = size / 2.0;
        ArberRect viewBounds = context.getPlotBounds();

        float[] px = RendererAllocationCache.getFloatArray(this, "scatter.x", 8);
        float[] py = RendererAllocationCache.getFloatArray(this, "scatter.y", 8);

        canvas.setColor(color);
        for (int i = 0; i < n; i++) {
            final double x = xData[i];
            final double y = yData[i];
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;

            context.mapToPixel(x, y, p0);
            if (!Double.isFinite(p0[0]) || !Double.isFinite(p0[1])) continue;

            if (p0[0] < viewBounds.minX() - halfSize || p0[0] > viewBounds.maxX() + halfSize ||
                    p0[1] < viewBounds.minY() - halfSize || p0[1] > viewBounds.maxY() + halfSize) {
                continue;
            }

            buildOctagon((float) p0[0], (float) p0[1], (float) (halfSize), px, py);
            canvas.fillPolygon(px, py, 8);
        }
    }

    private static void buildOctagon(float cx, float cy, float r, float[] xs, float[] ys) {
        for (int i = 0; i < 8; i++) {
            double a = i * (Math.PI * 2.0 / 8.0);
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
    }
}
