package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.TooltipContentProvider;
import com.arbergashi.charts.render.TooltipContext;
import com.arbergashi.charts.render.TooltipValueWriter;
import com.arbergashi.charts.util.ChartScale;

/**
 * Professional, zero-allocation, high-precision area chart renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class AreaRenderer extends BaseRenderer implements TooltipContentProvider {

    private final double[] p0 = new double[2];
    private float[] areaXs = new float[0];
    private float[] areaYs = new float[0];
    private float[] lineXs = new float[0];
    private float[] lineYs = new float[0];

    public AreaRenderer() {
        super("area");
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

        final double[] xData = model.getXData();
        final double[] yData = model.getYData();
        final int n = Math.min(n0, Math.min(xData.length, yData.length));
        if (n < 2) return;

        ArberRect bounds = context.getPlotBounds();
        float baseY = (float) (bounds.y() + bounds.height());

        int first = -1;
        for (int i = 0; i < n; i++) {
            double x = xData[i];
            double y = yData[i];
            if (Double.isFinite(x) && Double.isFinite(y)) {
                first = i;
                break;
            }
        }
        if (first < 0) return;

        int last = first;
        for (int i = first + 1; i < n; i++) {
            double x = xData[i];
            double y = yData[i];
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
            last = i;
        }
        if (last == first) return;

        int polyCount = (last - first + 1) + 2;
        ensureAreaCapacity(polyCount);

        context.mapToPixel(xData[first], 0, p0);
        areaXs[0] = (float) p0[0];
        areaYs[0] = baseY;

        int idx = 1;
        for (int i = first; i <= last; i++) {
            double x = xData[i];
            double y = yData[i];
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
            context.mapToPixel(x, y, p0);
            if (!Double.isFinite(p0[0]) || !Double.isFinite(p0[1])) continue;
            areaXs[idx] = (float) p0[0];
            areaYs[idx] = (float) p0[1];
            idx++;
        }

        context.mapToPixel(xData[last], 0, p0);
        areaXs[idx] = (float) p0[0];
        areaYs[idx] = baseY;
        idx++;

        ChartTheme theme = getResolvedTheme(context);
        ArberColor series = theme.getSeriesColor(getLayerIndex());
        canvas.setColor(series);
        canvas.fillPolygon(areaXs, areaYs, idx);

        int lineCount = idx - 2;
        ensureLineCapacity(lineCount);
        for (int i = 0; i < lineCount; i++) {
            lineXs[i] = areaXs[i + 1];
            lineYs[i] = areaYs[i + 1];
        }

        canvas.setStroke(resolveStrokeWidth());
        canvas.setColor(series);
        canvas.drawPolyline(lineXs, lineYs, lineCount);
    }

    private void ensureAreaCapacity(int needed) {
        if (areaXs.length >= needed && areaYs.length >= needed) return;
        int size = Math.max(needed, areaXs.length == 0 ? 128 : areaXs.length * 2);
        areaXs = new float[size];
        areaYs = new float[size];
    }

    private void ensureLineCapacity(int needed) {
        if (lineXs.length >= needed && lineYs.length >= needed) return;
        int size = Math.max(needed, lineXs.length == 0 ? 128 : lineXs.length * 2);
        lineXs = new float[size];
        lineYs = new float[size];
    }

    private float resolveStrokeWidth() {
        float base = 1.5f;
        PlotContext ctx = getActiveContext();
        if (ctx != null && ctx.getRenderHints() != null) {
            Float hinted = ctx.getRenderHints().getStrokeWidth();
            if (hinted != null && Float.isFinite(hinted) && hinted > 0f) {
                base = hinted;
            }
        }
        return ChartScale.scale(base);
    }
}
