package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ColorUtils;

import java.util.Map;

/**
 * Sankey Pro: improved flow diagram with smoother ribbons.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class SankeyProRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("sankey_pro", new RendererDescriptor("sankey_pro", "renderer.sankey_pro", "/icons/sankey.svg"), SankeyProRenderer::new);
    }

    public SankeyProRenderer() {
        super("sankey_pro");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        ArberRect bounds = context.getPlotBounds();
        double left = bounds.x() + 40;
        double right = bounds.x() + bounds.width() - 40;
        double top = bounds.y() + 40;
        double h = bounds.height() - 80;

        // map node names to y positions (reuse map to avoid allocations)
        Map<String, Double> nodeY = RendererAllocationCache.getMap(this, "sankeypro.nodeY");
        Map<String, Integer> nodeIndex = RendererAllocationCache.getMap(this, "sankeypro.nodeIndex");
        int idx = 0;
        for (int i = 0; i < count; i++) {
            String lbl = model.getLabel(i);
            if (lbl == null) continue;
            String[] parts = lbl.split(":");
            if (parts.length < 2) continue;
            if (!nodeY.containsKey(parts[0])) {
                nodeY.put(parts[0], top + (idx * (h / Math.max(1, count))));
                nodeIndex.put(parts[0], idx++);
            }
            if (!nodeY.containsKey(parts[1])) {
                nodeY.put(parts[1], top + (idx * (h / Math.max(1, count))));
                nodeIndex.put(parts[1], idx++);
            }
        }

        ArberColor baseColor = getSeriesColor(model);

        // draw smooth ribbons via cubic curves between source and destination nodes
        for (int i = 0; i < count; i++) {
            String lbl = model.getLabel(i);
            if (lbl == null) continue;
            String[] parts = lbl.split(":");
            if (parts.length < 2) continue;
            Double y1 = nodeY.get(parts[0]);
            Double y2 = nodeY.get(parts[1]);
            if (y1 == null || y2 == null) continue;
            double weight = Math.clamp(model.getY(i), 1.0, 20.0);
            ArberColor linkColor = baseColor;
            if (isMultiColor()) {
                Integer srcIdx = nodeIndex.get(parts[0]);
                linkColor = themeSeries(context, srcIdx == null ? 0 : srcIdx);
                if (linkColor == null) linkColor = baseColor;
            }

            double x1 = left;
            double x2 = right;
            double cx1 = left + (right - left) * 0.35;
            double cx2 = left + (right - left) * 0.65;

            int segments = 24;
            float[] xs = RendererAllocationCache.getFloatArray(this, "sankeypro.link.x", segments + 1);
            float[] ys = RendererAllocationCache.getFloatArray(this, "sankeypro.link.y", segments + 1);
            for (int j = 0; j <= segments; j++) {
                float t = j / (float) segments;
                float u = 1f - t;
                float tt = t * t;
                float uu = u * u;
                float uuu = uu * u;
                float ttt = tt * t;
                xs[j] = (float) (uuu * x1 + 3f * uu * t * cx1 + 3f * u * tt * cx2 + ttt * x2);
                ys[j] = (float) (uuu * y1 + 3f * uu * t * y1 + 3f * u * tt * y2 + ttt * y2);
            }

            canvas.setStroke((float) weight);
            canvas.setColor(ColorUtils.applyAlpha(linkColor, 0.28f));
            canvas.drawPolyline(xs, ys, segments + 1);
        }
    }
}
