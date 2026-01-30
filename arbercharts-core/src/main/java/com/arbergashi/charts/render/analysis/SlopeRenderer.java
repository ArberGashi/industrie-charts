package com.arbergashi.charts.render.analysis;


import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
/**
 * Slope chart renderer (minimal): expects exactly two points.
 * Connects both points with a line and marks the endpoints.
 * Useful for simple before/after slope comparisons.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class SlopeRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];

    public SlopeRenderer() {
        super("slope");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count != 2) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();

        context.mapToPixel(xData[0], yData[0], pBuffer);
        double ax = pBuffer[0], ay = pBuffer[1];
        context.mapToPixel(xData[1], yData[1], pBuffer);
        double bx = pBuffer[0], by = pBuffer[1];

        ArberColor c = seriesOrBase(model, context, 0);
        float w = ChartAssets.getFloat("chart.render.slope.width", 2.0f);

        canvas.setColor(c);
        canvas.setStroke(ChartScale.scale(w));
        float[] xs = RendererAllocationCache.getFloatArray(this, "slope.line.x", 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "slope.line.y", 2);
        xs[0] = (float) ax;
        ys[0] = (float) ay;
        xs[1] = (float) bx;
        ys[1] = (float) by;
        canvas.drawPolyline(xs, ys, 2);

        double r = ChartScale.scale(ChartAssets.getFloat("chart.render.slope.radius", 4.0f));
        if (isMultiColor()) {
            ArberColor c0 = themeSeries(context, 0);
            if (c0 == null) c0 = c;
            canvas.setColor(c0);
        } else {
            canvas.setColor(c);
        }
        canvas.fillRect((float) (ax - r), (float) (ay - r), (float) (r * 2), (float) (r * 2));
        if (isMultiColor()) {
            ArberColor c1 = themeSeries(context, 1);
            if (c1 == null) c1 = c;
            canvas.setColor(c1);
        }
        canvas.fillRect((float) (bx - r), (float) (by - r), (float) (r * 2), (float) (r * 2));
    }
}
