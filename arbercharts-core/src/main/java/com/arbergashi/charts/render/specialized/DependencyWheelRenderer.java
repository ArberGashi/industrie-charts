package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import com.arbergashi.charts.tools.RendererAllocationCache;
/**
 * Dependency wheel: visualizes dependencies among modules in a circular layout.
 * Input: label "src:dst" and weight in y.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class DependencyWheelRenderer extends BaseRenderer {

    

    public DependencyWheelRenderer() {
        super("dependency_wheel");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        Rectangle2D bounds = context.plotBounds();
        double cx = bounds.getCenterX();
        double cy = bounds.getCenterY();
        double r = Math.min(bounds.getWidth(), bounds.getHeight()) * 0.35;

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

        g2.setStroke(getSeriesStroke());
        for (int i = 0; i < count; i++) {
            String lbl = model.getLabel(i);
            if (lbl == null) continue;
            String[] parts = lbl.split(":");
            if (parts.length < 2) continue;
            Double a1 = angles.get(parts[0]);
            Double a2 = angles.get(parts[1]);
            if (a1 == null || a2 == null) continue;
            Color linkColor = seriesOrBase(model, context, i);
            g2.setColor(linkColor);
            double x1 = cx + r * Math.cos(a1);
            double y1 = cy + r * Math.sin(a1);
            double x2 = cx + r * Math.cos(a2);
            double y2 = cy + r * Math.sin(a2);
            g2.draw(getLine(x1, y1, x2, y2));
        }

        // draw nodes arc segments
        g2.setColor(themeAxisLabel(context));
        double start = 0;
        double seg = 360.0 / Math.max(1, angles.size());
        for (String k : angles.keySet()) {
            Shape nodeArc = getArc(cx - r - 20, cy - r - 20, (r + 20) * 2, (r + 20) * 2, start, seg, Arc2D.OPEN);
            g2.draw(nodeArc);

            // label
            double mid = Math.toRadians(start + seg / 2.0);
            double lx = cx + Math.cos(mid) * (r + 30);
            double ly = cy + Math.sin(mid) * (r + 30);
            drawLabel(g2, k, g2.getFont(), themeAxisLabel(context), (float) lx, (float) ly);

            start += seg;
        }
    }
}
