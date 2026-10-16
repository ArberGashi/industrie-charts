package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.grid.PolarGeometry;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;

import java.util.Optional;

/**
 * <h2>Modern Radar/Spider Renderer</h2>
 * <p>
 * Draws an interactive radar chart with a closed spider-web grid, filled area, and hover effects.
 * Drawing buffers are reused across frames and grow with the number of axes.
 * Nonfinite samples leave gaps in the outline and suppress its area fill.
 * </p>
 *
 * @author Arber Gashi
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
        if (n < 3 || !hasValidGeometry(context)) {
            hoverIndex = -1;
            return;
        }
        if (hoverIndex >= n) hoverIndex = -1;

        double angleStep = 360.0 / n;
        ArberColor accent = resolveSeriesColor(model, context);
        float[] xs = RendererAllocationCache.getFloatArray(this, "radar.data.x", n + 1);
        float[] ys = RendererAllocationCache.getFloatArray(this, "radar.data.y", n + 1);
        boolean complete = true;
        for (int i = 0; i < n; i++) {
            complete &= mapModelToCartesian(i, model, context, angleStep, xs, ys, i);
        }
        xs[n] = xs[0];
        ys[n] = ys[0];

        drawSpiderWeb(canvas, context, n, angleStep);
        drawRadarPath(canvas, xs, ys, n, complete, accent);
        drawDataNodes(canvas, context, xs, ys, n, accent);
    }

    private void drawSpiderWeb(ArberCanvas canvas, PlotContext context, int segments, double angleStep) {
        ChartTheme theme = getResolvedTheme(context);
        canvas.setColor(theme.getGridColor());
        canvas.setStroke(1.0f);

        ArberRect b = context.getPlotBounds();
        double cx = b.centerX();
        double cy = b.centerY();
        double maxRadius = PolarGeometry.radius(b);

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

    private void drawRadarPath(ArberCanvas canvas, float[] xs, float[] ys, int n, boolean complete, ArberColor accent) {
        if (complete) {
            canvas.setColor(ColorRegistry.applyAlpha(accent, 0.25f));
            canvas.fillPolygon(xs, ys, n);
        }

        float strokeWidth = (hoverIndex != -1) ? 2.5f : 1.5f;
        ArberColor strokeColor = (hoverIndex != -1) ? ColorRegistry.adjustBrightness(accent, 1.1) : accent;
        canvas.setStroke(strokeWidth);
        canvas.setColor(strokeColor);
        if (complete) {
            canvas.drawPolyline(xs, ys, n + 1);
        } else {
            for (int i = 0; i < n; i++) {
                if (Float.isFinite(xs[i]) && Float.isFinite(ys[i])
                        && Float.isFinite(xs[i + 1]) && Float.isFinite(ys[i + 1])) {
                    drawLine(canvas, xs[i], ys[i], xs[i + 1], ys[i + 1]);
                }
            }
        }
    }

    private void drawDataNodes(ArberCanvas canvas, PlotContext context, float[] xs, float[] ys, int n, ArberColor accent) {
        for (int i = 0; i < n; i++) {
            if (!Float.isFinite(xs[i]) || !Float.isFinite(ys[i])) continue;

            double nodeRadius = (i == hoverIndex) ? ChartScale.scale(5f) : ChartScale.scale(3f);
            canvas.setColor(accent);
            drawCircleFill(canvas, xs[i], ys[i], nodeRadius);
            canvas.setColor(themeBackground(context));
            drawCircleFill(canvas, xs[i], ys[i], nodeRadius / 2.0);
        }
    }

    private boolean mapModelToCartesian(int index, ChartModel model, PlotContext context, double angleStep, float[] xs, float[] ys, int offset) {
        ArberRect b = context.getPlotBounds();
        double cx = b.centerX();
        double cy = b.centerY();
        double maxRadius = PolarGeometry.radius(b);

        double value = model.getY(index);
        double radius = maxRadius * (value / context.getMaxY());
        double angleRad = Math.toRadians(index * angleStep - 90);

        xs[offset] = (float) (cx + Math.cos(angleRad) * radius);
        ys[offset] = (float) (cy + Math.sin(angleRad) * radius);
        return Float.isFinite(xs[offset]) && Float.isFinite(ys[offset]);
    }

    private boolean hasValidGeometry(PlotContext context) {
        ArberRect bounds = context.getPlotBounds();
        return bounds != null && bounds.width() > 1 && bounds.height() > 1
                && Double.isFinite(bounds.x()) && Double.isFinite(bounds.y())
                && Double.isFinite(bounds.width()) && Double.isFinite(bounds.height())
                && Float.isFinite((float) bounds.x()) && Float.isFinite((float) bounds.y())
                && Float.isFinite((float) bounds.maxX()) && Float.isFinite((float) bounds.maxY())
                && Double.isFinite(context.getMaxY()) && context.getMaxY() > 0;
    }

    private void drawPolygonRing(ArberCanvas canvas, double cx, double cy, double radius, int segments, double angleStep) {
        float[] xs = RendererAllocationCache.getFloatArray(this, "radar.grid.x", segments + 1);
        float[] ys = RendererAllocationCache.getFloatArray(this, "radar.grid.y", segments + 1);
        for (int s = 0; s < segments; s++) {
            double angleRad = Math.toRadians(s * angleStep - 90);
            xs[s] = (float) (cx + Math.cos(angleRad) * radius);
            ys[s] = (float) (cy + Math.sin(angleRad) * radius);
        }
        xs[segments] = xs[0];
        ys[segments] = ys[0];
        canvas.drawPolyline(xs, ys, segments + 1);
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        float[] xs = RendererAllocationCache.getFloatArray(this, "radar.line.x", 2);
        float[] ys = RendererAllocationCache.getFloatArray(this, "radar.line.y", 2);
        xs[0] = (float) x1;
        ys[0] = (float) y1;
        xs[1] = (float) x2;
        ys[1] = (float) y2;
        canvas.drawPolyline(xs, ys, 2);
    }

    private void drawCircleFill(ArberCanvas canvas, double cx, double cy, double r) {
        int segments = 20;
        float[] xs = RendererAllocationCache.getFloatArray(this, "radar.node.x", segments);
        float[] ys = RendererAllocationCache.getFloatArray(this, "radar.node.y", segments);
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
        if (n < 3 || !hasValidGeometry(context)) {
            hoverIndex = -1;
            return Optional.empty();
        }

        double angleStep = 360.0 / n;
        double thresholdSq = Math.pow(ChartScale.scale(10), 2);

        float[] xs = RendererAllocationCache.getFloatArray(this, "radar.hit.x", 1);
        float[] ys = RendererAllocationCache.getFloatArray(this, "radar.hit.y", 1);
        for (int i = 0; i < n; i++) {
            if (!mapModelToCartesian(i, model, context, angleStep, xs, ys, 0)) continue;

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
