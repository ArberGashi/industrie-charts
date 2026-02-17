package com.arbergashi.charts.demo;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ColorRegistry;
import com.arbergashi.charts.util.LatencyTracker;

final class CircularLatencyOverlayRendererAdapter extends BaseRenderer {
    private final LatencyTracker tracker;

    CircularLatencyOverlayRendererAdapter(LatencyTracker tracker) {
        super("circular_latency_overlay_adapter");
        this.tracker = tracker;
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (tracker == null || tracker.getSampleCount() <= 0) {
            return;
        }
        ArberRect bounds = context.getPlotBounds();
        if (bounds == null || bounds.width() <= 1 || bounds.height() <= 1) {
            return;
        }

        double p99 = tracker.getP99Millis();
        double p999 = tracker.getP999Millis();

        double cx = bounds.centerX();
        double cy = bounds.centerY();
        double radius = Math.min(bounds.width(), bounds.height()) * 0.36;
        double ringWidth = Math.max(8.0, radius * 0.16);

        ChartTheme theme = getResolvedTheme(context);
        ArberColor grid = ColorRegistry.applyAlpha(theme.getGridColor(), 0.45f);
        ArberColor p99Color = ColorRegistry.applyAlpha(theme.getSeriesColor(1), 0.9f);
        ArberColor p999Color = ColorRegistry.applyAlpha(theme.getSeriesColor(3), 0.9f);

        drawRing(canvas, cx, cy, radius - ringWidth, radius, -90.0, 300.0, grid);
        drawRing(canvas, cx, cy, radius - ringWidth * 2.2, radius - ringWidth * 1.2, -90.0, 300.0, grid);

        double p99Sweep = latencyToSweep(p99, 25.0);
        double p999Sweep = latencyToSweep(p999, 40.0);
        drawRing(canvas, cx, cy, radius - ringWidth, radius, -90.0, p99Sweep, p99Color);
        drawRing(canvas, cx, cy, radius - ringWidth * 2.2, radius - ringWidth * 1.2, -90.0, p999Sweep, p999Color);
    }

    private static double latencyToSweep(double millis, double maxMs) {
        if (!Double.isFinite(millis) || millis <= 0.0) {
            return 18.0;
        }
        double clamped = Math.min(maxMs, millis);
        double normalized = Math.log1p(clamped) / Math.log1p(maxMs);
        return Math.max(18.0, 300.0 * normalized);
    }

    private void drawRing(ArberCanvas canvas, double cx, double cy, double inner, double outer,
                          double startDeg, double sweepDeg, ArberColor color) {
        if (sweepDeg <= 0.0 || outer <= inner) {
            return;
        }
        int segments = Math.max(16, (int) Math.ceil(Math.abs(sweepDeg) / 4.0));
        int total = (segments + 1) * 2;
        float[] xs = RendererAllocationCache.getFloatArray(this, "latency.ring.x", total);
        float[] ys = RendererAllocationCache.getFloatArray(this, "latency.ring.y", total);
        int idx = 0;
        double start = Math.toRadians(startDeg);
        double end = Math.toRadians(startDeg + sweepDeg);
        for (int i = 0; i <= segments; i++) {
            double t = start + (end - start) * (i / (double) segments);
            xs[idx] = (float) (cx + Math.cos(t) * outer);
            ys[idx] = (float) (cy + Math.sin(t) * outer);
            idx++;
        }
        for (int i = segments; i >= 0; i--) {
            double t = start + (end - start) * (i / (double) segments);
            xs[idx] = (float) (cx + Math.cos(t) * inner);
            ys[idx] = (float) (cy + Math.sin(t) * inner);
            idx++;
        }
        canvas.setColor(color);
        canvas.fillPolygon(xs, ys, idx);
    }
}
