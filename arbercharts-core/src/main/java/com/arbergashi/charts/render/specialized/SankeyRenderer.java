package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FlowChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Path2D;
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
 */
public final class SankeyRenderer extends BaseRenderer {

    public SankeyRenderer() {
        super("sankey");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        if (!(model instanceof FlowChartModel flowModel)) {
            drawErrorMessage(g2, context, "SankeyRenderer requires a FlowChartModel");
            return;
        }

        List<? extends FlowChartModel.Node> nodes = flowModel.getNodes();
        List<? extends FlowChartModel.Link> links = flowModel.getLinks();
        if (nodes.isEmpty() || links.isEmpty()) return;

        // This is a simplified layout engine for demo purposes.
        // A real implementation would be much more complex.
        Map<String, Rectangle> nodeRects = layoutNodes(nodes, context.plotBounds().getBounds());
        Map<String, Integer> nodeIndex = com.arbergashi.charts.tools.RendererAllocationCache.getMap(this, "sankey.nodeIndex");
        for (int i = 0; i < nodes.size(); i++) {
            nodeIndex.put(nodes.get(i).getId(), i);
        }

        // Draw links
        for (FlowChartModel.Link link : links) {
            Rectangle sourceRect = nodeRects.get(link.getSource());
            Rectangle targetRect = nodeRects.get(link.getTarget());
            if (sourceRect == null || targetRect == null) continue;

            Integer srcIdx = nodeIndex.get(link.getSource());
            drawLink(g2, sourceRect, targetRect, link.getValue(), context, srcIdx == null ? 0 : srcIdx);
        }

        // Draw nodes
        for (FlowChartModel.Node node : nodes) {
            Rectangle rect = nodeRects.get(node.getId());
            if (rect == null) continue;
            if (isMultiColor()) {
                Integer idx = nodeIndex.get(node.getId());
                Color nodeColor = themeSeries(context, idx == null ? 0 : idx);
                if (nodeColor == null) nodeColor = themeGrid(context);
                g2.setColor(nodeColor);
            } else {
                g2.setColor(themeGrid(context));
            }
            g2.fill(rect);
            g2.setColor(themeForeground(context));
            g2.drawString(node.getLabel(), rect.x + 5, rect.y + 15);
        }
    }

    private Map<String, Rectangle> layoutNodes(List<? extends FlowChartModel.Node> nodes, Rectangle bounds) {
        Map<String, Rectangle> rects = new HashMap<>();
        int nodeWidth = 100;
        int yStep = bounds.height / (nodes.size() + 1);
        for (int i = 0; i < nodes.size(); i++) {
            FlowChartModel.Node node = nodes.get(i);
            // Simple alternating layout
            int x = (i % 2 == 0) ? bounds.x : bounds.x + bounds.width - nodeWidth;
            int y = bounds.y + yStep * (i / 2 + 1);
            rects.put(node.getId(), new Rectangle(x, y, nodeWidth, 30));
        }
        return rects;
    }

    private void drawLink(Graphics2D g2, Rectangle r1, Rectangle r2, double value, PlotContext context, int colorIndex) {
        int x1 = r1.x + r1.width;
        int y1 = r1.y + r1.height / 2;
        int x2 = r2.x;
        int y2 = r2.y + r2.height / 2;

        Path2D path = getPathCache();
        path.reset();
        path.moveTo(x1, y1);
        path.curveTo(x1 + 100, y1, x2 - 100, y2, x2, y2);

        g2.setStroke(getCachedStroke((float) Math.max(1f, (float) value / 2.0f)));
        Color linkColor = isMultiColor() ? themeSeries(context, colorIndex) : themeAccent(context);
        if (linkColor == null) linkColor = themeAccent(context);
        g2.setColor(com.arbergashi.charts.util.ColorUtils.withAlpha(linkColor, 0.28f));
        g2.draw(path);
    }

    private void drawErrorMessage(Graphics2D g2, PlotContext context, String message) {
        g2.setColor(themeAccent(context));
        g2.drawString(message, context.plotBounds().getBounds().x + 10, context.plotBounds().getBounds().y + 20);
    }
}
