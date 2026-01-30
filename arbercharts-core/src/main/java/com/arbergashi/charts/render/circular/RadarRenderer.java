package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;

import java.util.Optional;

/**
 * <h1>Modern Radar/Spider Renderer</h1>
 * <p>
 * Draws a professional, interactive radar chart with a spider-web grid, filled area, and hover effects,
 * adhering to strict zero-allocation guidelines.
 * </p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class RadarRenderer extends BaseRenderer {

    private int hoverIndex = -1;

    public RadarRenderer() {
        super("radar");
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n < 3) return;

        ArberRect b = context.getPlotBounds();
        if (b == null || b.getWidth() <= 1 || b.getHeight() <= 1) return;

        double angleStep = 360.0 / n;
        ArberColor accent = resolveSeriesColor(model, context);

        drawSpiderWeb(canvas, context, n, angleStep, model);
        drawRadarPath(canvas, model, context, n, angleStep, accent);
        drawDataNodes(canvas, model, context, n, angleStep, accent);
    }

    private void drawSpiderWeb(ArberCanvas canvas, PlotContext context, int segments, double angleStep, ChartModel model) {
        ChartTheme theme = getResolvedTheme(context);
        canvas.setColor(theme.getGridColor());
        canvas.setStroke(1.0f);

        ArberRect b = context.getPlotBounds();
        double cx = b.centerX();
        double cy = b.centerY();
        double maxRadius = Math.min(b.getWidth(), b.getHeight()) / 2.0 * 0.8;

        int ringCount = 5;
        for (int i = 1; i <= ringCount; i++) {
            double radius = maxRadius * ((double) i / ringCount);
            drawPolygonRing(canvas, cx, cy, radius, segments, angleStep);
        }

        for (int i = 0; i < segments; i++) {
            double angleRad = Math.toRadians(i * angleStep - 90);
            double x = cx + Math.cos(angleRad) * maxRadius;
            double y = cy + Math.sin(angleRad) * maxRadius;
            drawLine(canvas, cx, cy, x, y);
        }
    }

    private void drawRadarPath(ArberCanvas canvas, ChartModel model, PlotContext context, int n, double angleStep, ArberColor accent) {
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            mapModelToCartesian(i, model, context, angleStep, xs, ys);
        }

        canvas.setColor(ColorRegistry.applyAlpha(accent, 0.25f));
        canvas.fillPolygon(xs, ys, n);

        float strokeWidth = (hoverIndex != -1) ? 2.5f : 1.5f;
        ArberColor strokeColor = (hoverIndex != -1) ? ColorRegistry.adjustBrightness(accent, 1.1) : accent;
        canvas.setStroke(strokeWidth);
        canvas.setColor(strokeColor);
        canvas.drawPolyline(xs, ys, n);
    }

    private void drawDataNodes(ArberCanvas canvas, ChartModel model, PlotContext context, int n, double angleStep, ArberColor accent) {
        for (int i = 0; i < n; i++) {
            float[] xs = new float[1];
            float[] ys = new float[1];
            mapModelToCartesian(i, model, context, angleStep, xs, ys);

            double nodeRadius = (i == hoverIndex) ? ChartScale.scale(5f) : ChartScale.scale(3f);
            canvas.setColor(accent);
            drawCircleFill(canvas, xs[0], ys[0], nodeRadius);
            canvas.setColor(themeBackground(context));
            drawCircleFill(canvas, xs[0], ys[0], nodeRadius / 2.0);
        }
    }

    private void mapModelToCartesian(int index, ChartModel model, PlotContext context, double angleStep, float[] xs, float[] ys) {
        ArberRect b = context.getPlotBounds();
        double cx = b.centerX();
        double cy = b.centerY();
        double maxRadius = Math.min(b.getWidth(), b.getHeight()) / 2.0 * 0.8;

        double value = model.getY(index);
        double radius = maxRadius * (value / context.getMaxY());
        double angleRad = Math.toRadians(index * angleStep - 90);

        xs[0] = (float) (cx + Math.cos(angleRad) * radius);
        ys[0] = (float) (cy + Math.sin(angleRad) * radius);
    }

    private void mapModelToCartesian(int index, ChartModel model, PlotContext context, double angleStep, float[] xs, float[] ys, int offset) {
        ArberRect b = context.getPlotBounds();
        double cx = b.centerX();
        double cy = b.centerY();
        double maxRadius = Math.min(b.getWidth(), b.getHeight()) / 2.0 * 0.8;

        double value = model.getY(index);
        double radius = maxRadius * (value / context.getMaxY());
        double angleRad = Math.toRadians(index * angleStep - 90);

        xs[offset] = (float) (cx + Math.cos(angleRad) * radius);
        ys[offset] = (float) (cy + Math.sin(angleRad) * radius);
    }

    private void drawPolygonRing(ArberCanvas canvas, double cx, double cy, double radius, int segments, double angleStep) {
        float[] xs = new float[segments];
        float[] ys = new float[segments];
        for (int s = 0; s < segments; s++) {
            double angleRad = Math.toRadians(s * angleStep - 90);
            xs[s] = (float) (cx + Math.cos(angleRad) * radius);
            ys[s] = (float) (cy + Math.sin(angleRad) * radius);
        }
        canvas.drawPolyline(xs, ys, segments);
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        float[] xs = new float[]{(float) x1, (float) x2};
        float[] ys = new float[]{(float) y1, (float) y2};
        canvas.drawPolyline(xs, ys, 2);
    }

    private void drawCircleFill(ArberCanvas canvas, double cx, double cy, double r) {
        int segments = 20;
        float[] xs = new float[segments];
        float[] ys = new float[segments];
        for (int i = 0; i < segments; i++) {
            double a = (i * 2.0 * Math.PI) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.fillPolygon(xs, ys, segments);
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n < 3) return Optional.empty();

        double angleStep = 360.0 / n;
        double thresholdSq = Math.pow(ChartScale.scale(10), 2);

        float[] xs = new float[1];
        float[] ys = new float[1];
        for (int i = 0; i < n; i++) {
            mapModelToCartesian(i, model, context, angleStep, xs, ys);

            double dx = pixel.x() - xs[0];
            double dy = pixel.y() - ys[0];

            if ((dx * dx + dy * dy) < thresholdSq) {
                this.hoverIndex = i;
                return Optional.of(i);
            }
        }

        this.hoverIndex = -1;
        return Optional.empty();
    }

    private ArberColor resolveSeriesColor(ChartModel model, PlotContext context) {
        if (model.getColor() != null) return model.getColor();
        return getResolvedTheme(context).getSeriesColor(0);
    }

    @Override
    public void clearHover() {
        this.hoverIndex = -1;
    }
}
