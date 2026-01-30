package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FlowChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ColorUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SankeyRenderer visualizes flow data.
 * It expects a FlowChartModel.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class SankeyRenderer extends BaseRenderer {

    public SankeyRenderer() {
        super("sankey");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!(model instanceof FlowChartModel flowModel)) {
            return;
        }

        List<? extends FlowChartModel.Node> nodes = flowModel.getNodes();
        List<? extends FlowChartModel.Link> links = flowModel.getLinks();
        if (nodes.isEmpty() || links.isEmpty()) return;

        // Simplified layout engine for demo purposes.
        Map<String, ArberRect> nodeRects = layoutNodes(nodes, context.getPlotBounds());
        Map<String, Integer> nodeIndex = RendererAllocationCache.getMap(this, "sankey.nodeIndex");
        for (int i = 0; i < nodes.size(); i++) {
            nodeIndex.put(nodes.get(i).getId(), i);
        }

        // Draw links
        for (FlowChartModel.Link link : links) {
            ArberRect sourceRect = nodeRects.get(link.getSource());
            ArberRect targetRect = nodeRects.get(link.getTarget());
            if (sourceRect == null || targetRect == null) continue;

            Integer srcIdx = nodeIndex.get(link.getSource());
            drawLink(canvas, sourceRect, targetRect, link.getValue(), context, srcIdx == null ? 0 : srcIdx);
        }

        // Draw nodes
        for (FlowChartModel.Node node : nodes) {
            ArberRect rect = nodeRects.get(node.getId());
            if (rect == null) continue;
            ArberColor nodeColor;
            if (isMultiColor()) {
                Integer idx = nodeIndex.get(node.getId());
                nodeColor = themeSeries(context, idx == null ? 0 : idx);
                if (nodeColor == null) nodeColor = themeGrid(context);
            } else {
                nodeColor = themeGrid(context);
            }
            canvas.setColor(nodeColor);
            canvas.fillRect((float) rect.x(), (float) rect.y(), (float) rect.width(), (float) rect.height());
        }
    }

    private Map<String, ArberRect> layoutNodes(List<? extends FlowChartModel.Node> nodes, ArberRect bounds) {
        Map<String, ArberRect> rects = new HashMap<>();
        double nodeWidth = 100.0;
        double yStep = bounds.height() / (nodes.size() + 1.0);
        for (int i = 0; i < nodes.size(); i++) {
            FlowChartModel.Node node = nodes.get(i);
            // Simple alternating layout
            double x = (i % 2 == 0) ? bounds.x() : bounds.x() + bounds.width() - nodeWidth;
            double y = bounds.y() + yStep * (i / 2.0 + 1.0);
            rects.put(node.getId(), new ArberRect(x, y, nodeWidth, 30.0));
        }
        return rects;
    }

    private void drawLink(ArberCanvas canvas, ArberRect r1, ArberRect r2, double value, PlotContext context, int colorIndex) {
        float x1 = (float) r1.maxX();
        float y1 = (float) (r1.y() + r1.height() * 0.5);
        float x2 = (float) r2.x();
        float y2 = (float) (r2.y() + r2.height() * 0.5);

        float c1x = x1 + 100f;
        float c1y = y1;
        float c2x = x2 - 100f;
        float c2y = y2;

        int segments = 24;
        float[] xs = RendererAllocationCache.getFloatArray(this, "sankey.link.x", segments + 1);
        float[] ys = RendererAllocationCache.getFloatArray(this, "sankey.link.y", segments + 1);
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float u = 1f - t;
            float tt = t * t;
            float uu = u * u;
            float uuu = uu * u;
            float ttt = tt * t;
            xs[i] = uuu * x1 + 3f * uu * t * c1x + 3f * u * tt * c2x + ttt * x2;
            ys[i] = uuu * y1 + 3f * uu * t * c1y + 3f * u * tt * c2y + ttt * y2;
        }

        canvas.setStroke((float) Math.max(1f, (float) value / 2.0f));
        ArberColor linkColor = isMultiColor() ? themeSeries(context, colorIndex) : themeAccent(context);
        if (linkColor == null) linkColor = themeAccent(context);
        canvas.setColor(ColorUtils.applyAlpha(linkColor, 0.28f));
        canvas.drawPolyline(xs, ys, segments + 1);
    }
}
