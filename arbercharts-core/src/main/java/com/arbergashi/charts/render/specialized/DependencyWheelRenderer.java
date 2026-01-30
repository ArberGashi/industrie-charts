package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;

import java.util.Map;

/**
 * Dependency wheel: visualizes dependencies among modules in a circular layout.
 * Input: label "src:dst" and weight in y.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class DependencyWheelRenderer extends BaseRenderer {

    public DependencyWheelRenderer() {
        super("dependency_wheel");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        ArberRect bounds = context.getPlotBounds();
        double cx = bounds.centerX();
        double cy = bounds.centerY();
        double r = Math.min(bounds.width(), bounds.height()) * 0.35;

        Map<String, Double> angles = RendererAllocationCache.getMap(this, "angles");
        int idx = 0;
        for (int i = 0; i < count; i++) {
            String lbl = model.getLabel(i);
            if (lbl == null) continue;
            String[] parts = lbl.split(":");
            if (parts.length < 2) continue;
            if (!angles.containsKey(parts[0])) angles.put(parts[0], idx++ * (2 * Math.PI / Math.max(1, count)));
            if (!angles.containsKey(parts[1])) angles.put(parts[1], idx++ * (2 * Math.PI / Math.max(1, count)));
        }

        float[] lineX = RendererAllocationCache.getFloatArray(this, "dep.line.x", 2);
        float[] lineY = RendererAllocationCache.getFloatArray(this, "dep.line.y", 2);
        canvas.setStroke(getSeriesStrokeWidth());

        // draw links
        for (int i = 0; i < count; i++) {
            String lbl = model.getLabel(i);
            if (lbl == null) continue;
            String[] parts = lbl.split(":");
            if (parts.length < 2) continue;
            Double a1 = angles.get(parts[0]);
            Double a2 = angles.get(parts[1]);
            if (a1 == null || a2 == null) continue;
            ArberColor linkColor = seriesOrBase(model, context, i);
            canvas.setColor(linkColor);
            double x1 = cx + r * Math.cos(a1);
            double y1 = cy + r * Math.sin(a1);
            double x2 = cx + r * Math.cos(a2);
            double y2 = cy + r * Math.sin(a2);
            lineX[0] = (float) x1;
            lineY[0] = (float) y1;
            lineX[1] = (float) x2;
            lineY[1] = (float) y2;
            canvas.drawPolyline(lineX, lineY, 2);
        }

        // draw nodes arc segments (polyline approximation)
        ArberColor axisColor = themeAxisLabel(context);
        canvas.setColor(axisColor);
        int segs = Math.max(6, Math.min(32, angles.size() * 2));
        float[] arcX = RendererAllocationCache.getFloatArray(this, "dep.arc.x", segs + 1);
        float[] arcY = RendererAllocationCache.getFloatArray(this, "dep.arc.y", segs + 1);
        double start = 0;
        double seg = 2 * Math.PI / Math.max(1, angles.size());
        double rr = r + 20;
        for (int n = 0; n < angles.size(); n++) {
            double a0 = start;
            double a1 = start + seg;
            for (int i = 0; i <= segs; i++) {
                double t = a0 + (a1 - a0) * (i / (double) segs);
                arcX[i] = (float) (cx + Math.cos(t) * rr);
                arcY[i] = (float) (cy + Math.sin(t) * rr);
            }
            canvas.drawPolyline(arcX, arcY, segs + 1);
            start += seg;
        }
    }
}
