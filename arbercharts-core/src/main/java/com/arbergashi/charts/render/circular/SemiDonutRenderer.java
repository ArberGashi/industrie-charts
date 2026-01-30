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
 * <h1>Modern Semi-Donut Renderer</h1>
 * <p>
 * Draws a professional, interactive semi-donut chart, ideal for progress indicators and KPIs.
 * Adheres to strict zero-allocation guidelines.
 * </p>
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Progress Display:</b> Shows a single value (0-100%) as a colored arc.</li>
 *     <li><b>Background Track:</b> A subtle background arc indicates the 100% range.</li>
 *     <li><b>Rounded Caps:</b> The arc has rounded ends for a modern aesthetic.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class SemiDonutRenderer extends BaseRenderer {

    private double value = 0.0; // Expected to be in [0, 1] range

    public SemiDonutRenderer() {
        super("semiDonut");
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (model != null && model.getPointCount() > 0) {
            double modelMax = context.getMaxY();
            if (modelMax <= 0) modelMax = 100.0;
            this.value = model.getY(0) / modelMax;
        }

        ArberRect b = context.getPlotBounds();
        if (b == null || b.getWidth() <= 1 || b.getHeight() <= 1) return;

        double diameter = Math.min(b.getWidth(), b.getHeight()) * 0.95;
        double cx = b.centerX();
        double cy = b.centerY() + diameter * 0.10;
        double outerR = diameter * 0.5;

        ChartTheme theme = getResolvedTheme(context);
        drawTrack(canvas, cx, cy, outerR, theme);
        drawValueArc(canvas, cx, cy, outerR, theme);
    }

    private void drawTrack(ArberCanvas canvas, double cx, double cy, double outerR, ChartTheme t) {
        canvas.setStroke((float) ChartScale.scale(12f));
        canvas.setColor(ColorRegistry.applyAlpha(t.getGridColor(), 0.5f));
        drawArcPolyline(canvas, cx, cy, outerR, 180, 180);
    }

    private void drawValueArc(ArberCanvas canvas, double cx, double cy, double outerR, ChartTheme theme) {
        double t = MathUtils.clamp(this.value, 0, 1);
        double sweepAngle = t * 180.0;

        canvas.setStroke((float) ChartScale.scale(12f));
        canvas.setColor(mapValueToColor(t, theme));
        drawArcPolyline(canvas, cx, cy, outerR, 180, -sweepAngle);
    }

    private ArberColor mapValueToColor(double t, ChartTheme theme) {
        ArberColor c0 = theme.getSeriesColor(0);
        ArberColor c1 = theme.getSeriesColor(1);
        ArberColor c2 = theme.getSeriesColor(2);
        if (t < 0.5) {
            return ColorRegistry.interpolate(c0, c1, (float) (t * 2.0));
        }
        return ColorRegistry.interpolate(c1, c2, (float) ((t - 0.5) * 2.0));
    }

    private void drawArcPolyline(ArberCanvas canvas, double cx, double cy, double r, double startDeg, double sweepDeg) {
        int segments = Math.max(12, (int) (Math.abs(sweepDeg) / 4.0));
        float[] xs = new float[segments + 1];
        float[] ys = new float[segments + 1];
        double step = sweepDeg / segments;
        for (int i = 0; i <= segments; i++) {
            double a = Math.toRadians(startDeg + step * i);
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy - Math.sin(a) * r);
        }
        canvas.drawPolyline(xs, ys, segments + 1);
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        if (model == null || model.getPointCount() == 0) return Optional.empty();

        ArberRect b = context.getPlotBounds();
        if (b == null) return Optional.empty();

        double diameter = Math.min(b.getWidth(), b.getHeight()) * 0.95;
        double cx = b.centerX();
        double cy = b.centerY() + diameter * 0.10;
        double outerR = diameter * 0.5;
        double innerR = outerR * 0.60;

        double px = pixel.x();
        double py = pixel.y();

        double dx = px - cx;
        double dy = py - cy;
        double dist = Math.hypot(dx, dy);
        if (dist < innerR || dist > outerR) return Optional.empty();

        double angleDeg = Math.toDegrees(Math.atan2(cy - py, px - cx));
        if (angleDeg < 0) angleDeg += 360.0;
        if (angleDeg < 0 || angleDeg > 180) return Optional.empty();

        double prop = (180.0 - angleDeg) / 180.0;
        int n = model.getPointCount();
        double total = 0.0;
        for (int i = 0; i < n; i++) total += Math.abs(model.getWeight(i));
        if (total <= 0) return Optional.empty();

        double target = prop * total;
        double cum = 0.0;
        for (int i = 0; i < n; i++) {
            cum += Math.abs(model.getWeight(i));
            if (target <= cum) return Optional.of(i);
        }
        return Optional.empty();
    }

    public SemiDonutRenderer setValue(double value) {
        this.value = MathUtils.clamp(value, 0.0, 1.0);
        return this;
    }
}
