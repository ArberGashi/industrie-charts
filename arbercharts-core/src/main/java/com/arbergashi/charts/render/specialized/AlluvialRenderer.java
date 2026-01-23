package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FlowChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * AlluvialRenderer visualizes changes in structure over time.
 * It expects a FlowChartModel.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * @see FlowChartModel
 */
public final class AlluvialRenderer extends BaseRenderer {

    private final Path2D.Double flowPath = new Path2D.Double();
    private String[] nodeIds = new String[0];
    private int[] nodeX = new int[0];
    private int[] nodeY = new int[0];
    private int[] nodeW = new int[0];
    private int[] nodeH = new int[0];
    private int nodesCap = 0;

    public AlluvialRenderer() {
        super("alluvial");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        if (!(model instanceof FlowChartModel flowModel)) {
            drawErrorMessage(g2, context, "AlluvialRenderer requires a FlowChartModel");
            return;
        }

        java.util.List<? extends FlowChartModel.Node> nodes = flowModel.getNodes();
        java.util.List<? extends FlowChartModel.Link> links = flowModel.getLinks();
        if (nodes.isEmpty() || links.isEmpty()) return;

        // Ensure capacity
        int n = nodes.size();
        if (n > nodesCap) {
            nodesCap = Math.max(n, nodesCap * 2);
            nodeIds = (String[]) com.arbergashi.charts.tools.RendererAllocationCache.getArray(this, "alluvial.nodeIds", String.class, nodesCap);
            nodeX = com.arbergashi.charts.tools.RendererAllocationCache.getIntArray(this, "alluvial.nodeX", nodesCap);
            nodeY = com.arbergashi.charts.tools.RendererAllocationCache.getIntArray(this, "alluvial.nodeY", nodesCap);
            nodeW = com.arbergashi.charts.tools.RendererAllocationCache.getIntArray(this, "alluvial.nodeW", nodesCap);
            nodeH = com.arbergashi.charts.tools.RendererAllocationCache.getIntArray(this, "alluvial.nodeH", nodesCap);
        }

        // Layout nodes (simple deterministic layout)
        Rectangle bounds = context.plotBounds().getBounds();
        int steps = 3;
        int nodesPerStep = Math.max(1, n / steps);
        int xStep = Math.max(1, bounds.width / (steps));
        int yStep = bounds.height / (nodesPerStep + 1);

        for (int i = 0; i < n; i++) {
            int stepIndex = i / nodesPerStep;
            int nodeInStep = i % nodesPerStep;
            int x = bounds.x + stepIndex * xStep + xStep / 4;
            int y = bounds.y + (nodeInStep + 1) * yStep;
            nodeIds[i] = nodes.get(i).getId();
            nodeX[i] = x;
            nodeY[i] = y;
            nodeW[i] = 50;
            nodeH[i] = 20;
        }

        // Draw links (flows) using index lookup (linear search to avoid allocations)
        int linkIndex = 0;
        for (FlowChartModel.Link link : links) {
            String src = link.getSource();
            String tgt = link.getTarget();
            int sIdx = -1, tIdx = -1;
            for (int i = 0; i < n; i++) {
                if (nodeIds[i].equals(src)) sIdx = i;
                if (nodeIds[i].equals(tgt)) tIdx = i;
                if (sIdx != -1 && tIdx != -1) break;
            }
            if (sIdx == -1 || tIdx == -1) continue;

            drawFlow(g2, model, context, sIdx, tIdx, linkIndex);
            linkIndex++;
        }

        // Draw nodes (strata)
        g2.setColor(themeGrid(context));
        for (int i = 0; i < n; i++) {
            g2.fill(getRect(nodeX[i], nodeY[i], nodeW[i], nodeH[i]));
            g2.setColor(themeForeground(context));
            g2.drawString(nodes.get(i).getLabel(), nodeX[i] + 5, nodeY[i] + 15);
            g2.setColor(themeGrid(context));
        }
    }

    private void drawFlow(Graphics2D g2, ChartModel model, PlotContext context, int sIdx, int tIdx, int linkIndex) {
        int x1 = nodeX[sIdx] + nodeW[sIdx];
        int y1 = nodeY[sIdx];
        int x2 = nodeX[tIdx];
        int y2 = nodeY[tIdx];
        int y2b = nodeY[tIdx] + nodeH[tIdx];
        int y1b = nodeY[sIdx] + nodeH[sIdx];

        flowPath.reset();
        flowPath.moveTo(x1, y1);
        flowPath.lineTo(x2, y2);
        flowPath.lineTo(x2, y2b);
        flowPath.lineTo(x1, y1b);
        flowPath.closePath();

        Color base = seriesOrBase(model, context, 0);
        Color flowColor = isMultiColor() ? themeSeries(context, linkIndex) : base;
        if (flowColor == null) flowColor = base;
        g2.setColor(com.arbergashi.charts.util.ColorUtils.withAlpha(flowColor, 0.28f));
        g2.fill(flowPath);
    }

    private void drawErrorMessage(Graphics2D g2, PlotContext context, String message) {
        g2.setColor(themeAccent(context));
        g2.drawString(message, context.plotBounds().getBounds().x + 10, context.plotBounds().getBounds().y + 20);
    }
}
