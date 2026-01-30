package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
import com.arbergashi.charts.util.MathUtils;

import java.util.Optional;

/**
 * <h1>Modern Stacked Radial Bar Renderer</h1>
 * <p>
 * Draws a stacked radial bar chart, ideal for comparing composite values across categories.
 * Adheres to strict zero-allocation guidelines.
 * </p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class RadialStackedRenderer extends BaseRenderer {

    private int hoverCategory = -1;
    private int hoverSeries = -1;

    public RadialStackedRenderer() {
        super("radialStacked");
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return;

        drawRadialGrid(canvas, context, n);
        drawStackedBars(canvas, model, context, n);
    }

    private void drawRadialGrid(ArberCanvas canvas, PlotContext context, int n) {
        ChartTheme t = getResolvedTheme(context);
        ArberRect b = context.getPlotBounds();
        double cx = b.centerX();
        double cy = b.centerY();
        double maxRadius = Math.min(b.width(), b.height()) / 2.0 * 0.85;

        canvas.setColor(t.getGridColor());
        canvas.setStroke(1.0f);

        double angleStep = 360.0 / n;
        for (int i = 0; i < n; i++) {
            double angleRad = Math.toRadians(i * angleStep - 90);
            double x = cx + Math.cos(angleRad) * maxRadius;
            double y = cy + Math.sin(angleRad) * maxRadius;
            float[] xs = {(float) cx, (float) x};
            float[] ys = {(float) cy, (float) y};
            canvas.drawPolyline(xs, ys, 2);
        }
    }

    private void drawStackedBars(ArberCanvas canvas, ChartModel model, PlotContext context, int n) {
        double angleStep = 360.0 / n;
        double barWidth = (360.0 / n) * 0.6;

        double maxY = Math.max(1e-9, context.getMaxY());
        for (int i = 0; i < n; i++) {
            double startAngle = i * angleStep - 90 - barWidth / 2.0;

            double prevValue = model.getX(i);
            double currentValue = model.getY(i);

            double r1 = (prevValue / maxY) * getPlotRadius(context);
            double r2 = ((prevValue + currentValue) / maxY) * getPlotRadius(context);

            ArberColor color = getSeriesColor(model);
            if (i == hoverCategory) {
                color = ColorRegistry.adjustBrightness(color, 1.1);
            }

            drawSegment(canvas, context, r1, r2, startAngle, barWidth, color);
        }
    }

    private void drawSegment(ArberCanvas canvas, PlotContext context, double r1, double r2, double startAngle, double extent, ArberColor color) {
        double cx = context.getPlotBounds().centerX();
        double cy = context.getPlotBounds().centerY();

        int segments = Math.max(12, (int) (Math.abs(extent) / 4.0));
        float[] xs = new float[(segments + 1) * 2];
        float[] ys = new float[(segments + 1) * 2];
        double step = extent / segments;

        for (int i = 0; i <= segments; i++) {
            double a = Math.toRadians(startAngle + step * i);
            xs[i] = (float) (cx + Math.cos(a) * r2);
            ys[i] = (float) (cy - Math.sin(a) * r2);
        }
        for (int i = 0; i <= segments; i++) {
            double a = Math.toRadians(startAngle + extent - step * i);
            xs[segments + 1 + i] = (float) (cx + Math.cos(a) * r1);
            ys[segments + 1 + i] = (float) (cy - Math.sin(a) * r1);
        }

        canvas.setColor(ColorRegistry.applyAlpha(color, 0.8f));
        canvas.fillPolygon(xs, ys, xs.length);
        canvas.setColor(color);
        canvas.setStroke(1.0f);
        canvas.drawPolyline(xs, ys, xs.length);
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return Optional.empty();

        double cx = context.getPlotBounds().centerX();
        double cy = context.getPlotBounds().centerY();

        double dx = pixel.x() - cx;
        double dy = pixel.y() - cy;

        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
        if (angle < 0) angle += 360;

        double angleStep = 360.0 / n;
        int index = (int) (angle / angleStep);

        if (index >= 0 && index < n) {
            hoverCategory = index;
            return Optional.of(index);
        }

        hoverCategory = -1;
        return Optional.empty();
    }

    private double getPlotRadius(PlotContext context) {
        ArberRect b = context.getPlotBounds();
        return Math.min(b.width(), b.height()) / 2.0 * 0.85;
    }

    @Override
    public void clearHover() {
        this.hoverCategory = -1;
        this.hoverSeries = -1;
    }
}
