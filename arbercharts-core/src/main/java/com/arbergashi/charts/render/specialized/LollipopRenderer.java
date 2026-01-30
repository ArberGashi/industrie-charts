package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.MathUtils;
import com.arbergashi.charts.tools.RendererAllocationCache;
/**
 * Lollipop chart: a lightweight, highly readable alternative to a bar chart.
 * Draws a stick from the baseline to the value and a circular head at the end.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class LollipopRenderer extends BaseRenderer {

    public LollipopRenderer() {
        super("lollipop");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        ArberColor c = getResolvedTheme(context).getSeriesColor(0);

        double[] buf = pBuffer();
        context.mapToPixel(0, 0.0, buf);
        double zeroY = buf[1];
        ArberRect bounds = context.getPlotBounds();
        double baselineY = MathUtils.clamp(zeroY, bounds.y(), bounds.maxY());

        float stickWidth = ChartAssets.getFloat("chart.render.lollipop.stickWidth", 1.5f);
        double radius = ChartScale.scale(ChartAssets.getFloat("chart.render.lollipop.radius", 4.5f));

        ChartTheme theme = getResolvedTheme(context);

        canvas.setStroke(ChartScale.scale(stickWidth));
        canvas.setColor(theme.getGridColor());

        // Optional: subtle baseline
        if (ChartAssets.getBoolean("chart.render.lollipop.showBaseline", true)) {
            drawLine(canvas, bounds.x(), baselineY, bounds.maxX(), baselineY);
        }

        for (int i = 0; i < count; i++) {
            ArberColor pointColor = isMultiColor() ? themeSeries(context, i) : c;
            if (pointColor == null) pointColor = c;
            canvas.setColor(pointColor);
            context.mapToPixel(xData[i], yData[i], buf);
            double px = buf[0];
            double py = buf[1];
            drawLine(canvas, px, baselineY, px, py);
            fillCircle(canvas, px, py, radius);
        }
    }

    public LollipopRenderer setMultiColor(boolean enabled){
        super.setMultiColor(enabled);
        return this;
        
    }

    private void drawLine(ArberCanvas canvas, double x0, double y0, double x1, double y1) {
        float[] xs = lineX();
        float[] ys = lineY();
        xs[0] = (float) x0;
        ys[0] = (float) y0;
        xs[1] = (float) x1;
        ys[1] = (float) y1;
        canvas.drawPolyline(xs, ys, 2);
    }

    private void fillCircle(ArberCanvas canvas, double cx, double cy, double r) {
        int segments = 12;
        float[] xs = circleX(segments);
        float[] ys = circleY(segments);
        for (int i = 0; i < segments; i++) {
            double a = (2.0 * Math.PI * i) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.fillPolygon(xs, ys, segments);
    }

    private float[] lineX() {
        return RendererAllocationCache.getFloatArray(this, "lineX", 2);
    }

    private float[] lineY() {
        return RendererAllocationCache.getFloatArray(this, "lineY", 2);
    }

    private float[] circleX(int size) {
        return RendererAllocationCache.getFloatArray(this, "circleX", size);
    }

    private float[] circleY(int size) {
        return RendererAllocationCache.getFloatArray(this, "circleY", size);
    }
}
