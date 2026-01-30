package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.TooltipContentProvider;
import com.arbergashi.charts.render.TooltipContext;
import com.arbergashi.charts.render.TooltipValueWriter;
/**
 * Professional, zero-allocation, high-precision line chart renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class LineRenderer extends BaseRenderer implements TooltipContentProvider {

    private final double[] p0 = new double[2];
    private final double[] prevBuf = new double[2];
    private final double[] currBuf = new double[2];

    public LineRenderer() {
        super("line");
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

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n0 = model.getPointCount();
        if (n0 < 2) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();
        final int n = Math.min(n0, Math.min(xData.length, yData.length));
        if (n < 2) return;

        ChartTheme theme = getResolvedTheme(context);
        ArberColor color = theme.getSeriesColor(getLayerIndex());
        canvas.setColor(color);
        float strokeWidth = 1.5f;
        if (context != null && context.getRenderHints() != null) {
            Float hinted = context.getRenderHints().getStrokeWidth();
            if (hinted != null && Float.isFinite(hinted) && hinted > 0f) {
                strokeWidth = hinted;
            }
        }
        canvas.setStroke(strokeWidth);

        boolean moved = false;
        for (int i = 0; i < n; i++) {
            context.mapToPixel(xData[i], yData[i], p0);
            float x = (float) p0[0];
            float y = (float) p0[1];
            if (!moved) {
                canvas.moveTo(x, y);
                moved = true;
            } else {
                canvas.lineTo(x, y);
            }
        }
    }
}
