package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FlowChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * AlluvialRenderer visualizes changes in structure over time.
 * It expects a FlowChartModel.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * @see FlowChartModel
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class AlluvialRenderer extends BaseRenderer {

    private String[] nodeIds = new String[0];
    private int[] nodeX = new int[0];
    private int[] nodeY = new int[0];
    private int[] nodeW = new int[0];
    private int[] nodeH = new int[0];
    private int nodesCap = 0;

    public AlluvialRenderer() {
        super("alluvial");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!(model instanceof FlowChartModel flowModel)) {
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
        ArberRect bounds = context.getPlotBounds();
        int steps = 3;
        int nodesPerStep = Math.max(1, n / steps);
        int xStep = Math.max(1, (int) (bounds.width() / (steps)));
        int yStep = (int) (bounds.height() / (nodesPerStep + 1));

        for (int i = 0; i < n; i++) {
            int stepIndex = i / nodesPerStep;
            int nodeInStep = i % nodesPerStep;
            int x = (int) bounds.x() + stepIndex * xStep + xStep / 4;
            int y = (int) bounds.y() + (nodeInStep + 1) * yStep;
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

            drawFlow(canvas, model, context, sIdx, tIdx, linkIndex);
            linkIndex++;
        }

        // Draw nodes (strata)
        canvas.setColor(themeGrid(context));
        for (int i = 0; i < n; i++) {
            canvas.fillRect(nodeX[i], nodeY[i], nodeW[i], nodeH[i]);
        }
    }

    private void drawFlow(ArberCanvas canvas, ChartModel model, PlotContext context, int sIdx, int tIdx, int linkIndex) {
        int x1 = nodeX[sIdx] + nodeW[sIdx];
        int y1 = nodeY[sIdx];
        int x2 = nodeX[tIdx];
        int y2 = nodeY[tIdx];
        int y2b = nodeY[tIdx] + nodeH[tIdx];
        int y1b = nodeY[sIdx] + nodeH[sIdx];

        float[] xs = new float[] {x1, x2, x2, x1};
        float[] ys = new float[] {y1, y2, y2b, y1b};

        ArberColor base = seriesOrBase(model, context, 0);
        ArberColor flowColor = isMultiColor() ? themeSeries(context, linkIndex) : base;
        if (flowColor == null) flowColor = base;
        canvas.setColor(ColorRegistry.applyAlpha(flowColor, 0.28f));
        canvas.fillPolygon(xs, ys, xs.length);
    }
}
