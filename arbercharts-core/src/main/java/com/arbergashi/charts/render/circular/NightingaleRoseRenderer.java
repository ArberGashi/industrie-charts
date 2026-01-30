package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorRegistry;
import com.arbergashi.charts.util.MathUtils;

import java.util.Optional;

/**
 * <h1>Modern Nightingale Rose Renderer</h1>
 * <p>
 * A modern, interactive polar area chart where segment radii represent values.
 * Adheres to strict zero-allocation guidelines.
 * </p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class NightingaleRoseRenderer extends BaseRenderer {

    private int hoverIndex = -1;

    public NightingaleRoseRenderer() {
        super("nightingaleRose");
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;

        ArberRect b = context.getPlotBounds();
        if (b == null || b.getWidth() <= 1 || b.getHeight() <= 1) return;

        double cx = b.centerX();
        double cy = b.centerY();
        double maxRadius = Math.min(b.getWidth(), b.getHeight()) / 2.0 * 0.8;

        drawPolarGrid(canvas, context, n);

        double angleStep = 360.0 / n;
        double maxValue = getMaxValue(model);

        for (int i = 0; i < n; i++) {
            double valueRatio = model.getY(i) / maxValue;
            if (!Double.isFinite(valueRatio) || valueRatio < 0) valueRatio = 0.0;

            double r = valueRatio * maxRadius;
            if (r <= 0.0) continue;

            double startAngle = i * angleStep - 90.0;

            ArberColor color = getSeriesColor(model, i, context);
            if (i == hoverIndex) {
                color = ColorRegistry.adjustBrightness(color, 1.1);
            }

            canvas.setColor(ColorRegistry.applyAlpha(color, 0.75f));
            fillArcSegment(canvas, cx, cy, r, startAngle, -angleStep);
            canvas.setStroke(1.5f);
            canvas.setColor(color);
            drawArcPolyline(canvas, cx, cy, r, startAngle, -angleStep);
        }
    }

    private void drawPolarGrid(ArberCanvas canvas, PlotContext context, int segments) {
        ChartTheme t = getResolvedTheme(context);
        ArberRect b = context.getPlotBounds();
        double cx = b.centerX();
        double cy = b.centerY();
        double maxRadius = Math.min(b.getWidth(), b.getHeight()) / 2.0 * 0.8;

        canvas.setColor(t.getGridColor());
        canvas.setStroke(1.0f);

        int ringCount = 4;
        for (int i = 1; i <= ringCount; i++) {
            double r = maxRadius * ((double) i / ringCount);
            drawCirclePolyline(canvas, cx, cy, r, 48);
        }

        double angleStep = 360.0 / segments;
        for (int i = 0; i < segments; i++) {
            double angleRad = Math.toRadians(i * angleStep - 90);
            double x = cx + Math.cos(angleRad) * maxRadius;
            double y = cy + Math.sin(angleRad) * maxRadius;
            drawLine(canvas, cx, cy, x, y);
        }
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n <= 0) return Optional.empty();

        ArberRect b = context.getPlotBounds();
        double cx = b.centerX();
        double cy = b.centerY();
        double maxRadius = Math.min(b.getWidth(), b.getHeight()) / 2.0 * 0.8;

        double dx = pixel.x() - cx;
        double dy = pixel.y() - cy;
        double distSq = dx * dx + dy * dy;

        if (distSq > maxRadius * maxRadius) {
            hoverIndex = -1;
            return Optional.empty();
        }

        double angleStep = 360.0 / n;
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0;
        if (angle < 0.0) angle += 360.0;

        int index = (int) (angle / angleStep);
        if (index < 0 || index >= n) {
            hoverIndex = -1;
            return Optional.empty();
        }

        double maxValue = getMaxValue(model);
        double valueRatio = model.getY(index) / maxValue;
        double radius = valueRatio * maxRadius;

        if (Math.sqrt(distSq) <= radius) {
            hoverIndex = index;
            return Optional.of(index);
        }

        hoverIndex = -1;
        return Optional.empty();
    }

    private double getMaxValue(ChartModel model) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < model.getPointCount(); i++) {
            if (model.getY(i) > max) {
                max = model.getY(i);
            }
        }
        return (Double.isFinite(max) && max > 0) ? max : 1.0;
    }

    private ArberColor getSeriesColor(ChartModel model, int index, PlotContext context) {
        if (model.getColor() != null) return model.getColor();
        return getResolvedTheme(context).getSeriesColor(index);
    }

    private void drawCirclePolyline(ArberCanvas canvas, double cx, double cy, double r, int segments) {
        float[] xs = new float[segments + 1];
        float[] ys = new float[segments + 1];
        for (int i = 0; i <= segments; i++) {
            double a = (i * 2.0 * Math.PI) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.drawPolyline(xs, ys, segments + 1);
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

    private void fillArcSegment(ArberCanvas canvas, double cx, double cy, double r, double startDeg, double sweepDeg) {
        int segments = Math.max(12, (int) (Math.abs(sweepDeg) / 4.0));
        int total = segments + 2;
        float[] xs = new float[total];
        float[] ys = new float[total];
        xs[0] = (float) cx;
        ys[0] = (float) cy;
        double step = sweepDeg / segments;
        for (int i = 0; i <= segments; i++) {
            double a = Math.toRadians(startDeg + step * i);
            xs[i + 1] = (float) (cx + Math.cos(a) * r);
            ys[i + 1] = (float) (cy - Math.sin(a) * r);
        }
        canvas.fillPolygon(xs, ys, total);
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        float[] xs = new float[]{(float) x1, (float) x2};
        float[] ys = new float[]{(float) y1, (float) y2};
        canvas.drawPolyline(xs, ys, 2);
    }

    @Override
    public void clearHover() {
        this.hoverIndex = -1;
    }
}
