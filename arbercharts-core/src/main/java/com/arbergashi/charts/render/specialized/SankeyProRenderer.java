package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Sankey Pro: improved flow diagram with smoother ribbons and label caching.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class SankeyProRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("sankey_pro", new RendererDescriptor("sankey_pro", "renderer.sankey_pro", "/icons/sankey.svg"), SankeyProRenderer::new);
    }

    public SankeyProRenderer() {
        super("sankey_pro");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        Rectangle2D bounds = context.plotBounds();
        double left = bounds.getX() + 40;
        double right = bounds.getX() + bounds.getWidth() - 40;
        double top = bounds.getY() + 40;
        double h = bounds.getHeight() - 80;

        // map node names to y positions (reuse map to avoid allocations)
        java.util.Map<String, Double> nodeY = com.arbergashi.charts.tools.RendererAllocationCache.getMap(this, "sankeypro.nodeY");
        java.util.Map<String, Integer> nodeIndex = com.arbergashi.charts.tools.RendererAllocationCache.getMap(this, "sankeypro.nodeIndex");
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

        Color baseColor = getSeriesColor(model);
        if (!isMultiColor()) {
            g2.setColor(baseColor);
        }

        // draw smooth ribbons via cubic curves between source and destination nodes
        for (int i = 0; i < count; i++) {
            String lbl = model.getLabel(i);
            if (lbl == null) continue;
            String[] parts = lbl.split(":");
            if (parts.length < 2) continue;
            Double y1 = nodeY.get(parts[0]);
            Double y2 = nodeY.get(parts[1]);
            if (y1 == null || y2 == null) continue;
            // JDK 25: Use Math.clamp() for weight bounds
            double weight = Math.clamp(model.getY(i), 1.0, 20.0);
            Color linkColor = baseColor;
            if (isMultiColor()) {
                Integer srcIdx = nodeIndex.get(parts[0]);
                linkColor = themeSeries(context, srcIdx == null ? 0 : srcIdx);
                if (linkColor == null) linkColor = baseColor;
                g2.setColor(linkColor);
            }

            // control points to smooth the flow
            double x1 = left, x2 = right;
            double cx1 = left + (right - left) * 0.35;
            double cx2 = left + (right - left) * 0.65;

            g2.setStroke(getCachedStroke((float) weight, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(getCubicCurve(x1, y1, cx1, y1, cx2, y2, x2, y2));
        }

        // draw node labels using BaseRenderer infrastructure
        for (Map.Entry<String, Double> e : nodeY.entrySet()) {
            drawLabel(g2, e.getKey(), g2.getFont(), themeAxisLabel(context), (float) (left - 30), e.getValue().floatValue());
        }
    }
}
