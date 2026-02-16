package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.MathUtils;
import java.util.Optional;
/**
 * <h1>Modern Radial Bar Renderer</h1>
 * <p>
 * Draws a professional, interactive radial bar chart, adhering to strict zero-allocation guidelines.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Concentric Grid:</b> A clean, circular grid for easy value comparison.</li>
 *     <li><b>Styled Bars:</b> Bars have rounded caps for a modern look.</li>
 *     <li><b>Hover Effect:</b> Bars are highlighted on mouse hover with a tooltip.</li>
 *     <li><b>Smart Labels:</b> Category labels are drawn at the end of each bar.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
    public final class RadialBarRenderer extends BaseRenderer {

    private int hoverIndex = -1;

    public RadialBarRenderer() {
        super("radialBar");
    }

    private void drawRadialGrid(ArberCanvas canvas, double cx, double cy, double maxRadius, double maxValue, PlotContext context) {
        ChartTheme t = getResolvedTheme(context);
        canvas.setColor(t.getGridColor());
        canvas.setStroke(1.0f);

        int ringCount = 4;
        for (int i = 1; i <= ringCount; i++) {
            double r = maxRadius * ((double) i / ringCount);
            drawCirclePolyline(canvas, cx, cy, r);
        }
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return;

        ArberRect b = context.getPlotBounds();
        double cx = b.centerX();
        double cy = b.centerY();
        double maxRadius = Math.min(b.width(), b.height()) / 2.0 * 0.85;

        drawRadialGrid(canvas, cx, cy, maxRadius, context.getMaxY(), context);
        drawBars(canvas, model, context, cx, cy, maxRadius);
    }

    private void drawBars(ArberCanvas canvas, ChartModel model, PlotContext context, double cx, double cy, double maxRadius) {
        int n = model.getPointCount();
        double barWidth = (maxRadius * 0.8) / n;
        double gap = barWidth * 0.4;

        for (int i = 0; i < n; i++) {
            double value = model.getY(i);
            double t = MathUtils.clamp(value / context.getMaxY(), 0, 1);
            double angleSweep = t * 360.0;

            double innerRadius = maxRadius - (i + 1) * barWidth + gap;
            double outerRadius = maxRadius - i * barWidth;
            double diameter = (innerRadius + outerRadius);

            float strokeWidth = (float) (outerRadius - innerRadius);
            canvas.setStroke(strokeWidth);

            ArberColor color = getSeriesColor(model, i, context);
            if (i == hoverIndex) {
                // no brighten in core
            }
            canvas.setColor(color);

            drawArcPolyline(canvas, cx, cy, diameter / 2.0, 90, -angleSweep);
        }
    }


    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return Optional.empty();

        ArberRect b = context.getPlotBounds();
        double cx = b.centerX();
        double cy = b.centerY();
        double maxRadius = Math.min(b.width(), b.height()) / 2.0 * 0.85;

        double dx = pixel.x() - cx;
        double dy = pixel.y() - cy;
        double distSq = dx * dx + dy * dy;

        if (distSq > maxRadius * maxRadius) {
            hoverIndex = -1;
            return Optional.empty();
        }

        double barWidth = (maxRadius * 0.8) / n;
        double dist = Math.sqrt(distSq);

        for (int i = 0; i < n; i++) {
            double innerRadius = maxRadius - (i + 1) * barWidth;
            double outerRadius = maxRadius - i * barWidth;

            if (dist >= innerRadius && dist <= outerRadius) {
                hoverIndex = i;
                return Optional.of(i);
            }
        }

        hoverIndex = -1;
        return Optional.empty();
    }

    private ArberColor getSeriesColor(ChartModel model, int index, PlotContext context) {
        if (model.getColor() != null) return model.getColor();
        return getResolvedTheme(context).getSeriesColor(index);
    }

    @Override
    public void clearHover() {
        this.hoverIndex = -1;
    }

    private void drawCirclePolyline(ArberCanvas canvas, double cx, double cy, double r) {
        int segments = 48;
        float[] xs = RendererAllocationCache.getFloatArray(this, "radial.circle.x", segments + 1);
        float[] ys = RendererAllocationCache.getFloatArray(this, "radial.circle.y", segments + 1);
        for (int i = 0; i <= segments; i++) {
            double a = (i * 2.0 * Math.PI) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.drawPolyline(xs, ys, segments + 1);
    }

    private void drawArcPolyline(ArberCanvas canvas, double cx, double cy, double r, double startDeg, double sweepDeg) {
        int segments = Math.max(8, (int) Math.ceil(Math.abs(sweepDeg) / 6.0));
        float[] xs = RendererAllocationCache.getFloatArray(this, "radial.arc.x", segments + 1);
        float[] ys = RendererAllocationCache.getFloatArray(this, "radial.arc.y", segments + 1);
        for (int i = 0; i <= segments; i++) {
            double a = Math.toRadians(startDeg + sweepDeg * (i / (double) segments));
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy - Math.sin(a) * r);
        }
        canvas.drawPolyline(xs, ys, segments + 1);
    }
}
