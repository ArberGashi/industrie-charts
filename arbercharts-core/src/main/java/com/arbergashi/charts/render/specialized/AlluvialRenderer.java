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
 * @since 2026-01-01
 * @see FlowChartModel
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class AlluvialRenderer extends BaseRenderer {

    private String[] nodeIds = new String[0];
    private float[] nodeX = new float[0];
    private float[] nodeY = new float[0];
    private float[] nodeW = new float[0];
    private float[] nodeH = new float[0];
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
            nodeX = com.arbergashi.charts.tools.RendererAllocationCache.getFloatArray(this, "alluvial.nodeX", nodesCap);
            nodeY = com.arbergashi.charts.tools.RendererAllocationCache.getFloatArray(this, "alluvial.nodeY", nodesCap);
            nodeW = com.arbergashi.charts.tools.RendererAllocationCache.getFloatArray(this, "alluvial.nodeW", nodesCap);
            nodeH = com.arbergashi.charts.tools.RendererAllocationCache.getFloatArray(this, "alluvial.nodeH", nodesCap);
        }

        // Layout nodes (simple deterministic layout)
        ArberRect bounds = context.getPlotBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0) return;
        int nodesPerStep = Math.max(1, (n + 2) / 3);
        int steps = (n + nodesPerStep - 1) / nodesPerStep;
        double cellWidth = bounds.width() / steps;
        double cellHeight = bounds.height() / nodesPerStep;
        float width = (float) Math.min(50, cellWidth * 0.8);
        float height = (float) Math.min(20, cellHeight * 0.8);

        for (int i = 0; i < n; i++) {
            int stepIndex = i / nodesPerStep;
            int nodeInStep = i % nodesPerStep;
            nodeIds[i] = nodes.get(i).getId();
            nodeX[i] = (float) (bounds.x() + (stepIndex + 0.5) * cellWidth - width / 2);
            nodeY[i] = (float) (bounds.y() + (nodeInStep + 0.5) * cellHeight - height / 2);
            nodeW[i] = width;
            nodeH[i] = height;
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
        float x1 = nodeX[sIdx] + nodeW[sIdx];
        float y1 = nodeY[sIdx];
        float x2 = nodeX[tIdx];
        float y2 = nodeY[tIdx];
        float y2b = nodeY[tIdx] + nodeH[tIdx];
        float y1b = nodeY[sIdx] + nodeH[sIdx];

        float[] xs = new float[] {x1, x2, x2, x1};
        float[] ys = new float[] {y1, y2, y2b, y1b};

        ArberColor base = seriesOrBase(model, context, 0);
        ArberColor flowColor = isMultiColor() ? themeSeries(context, linkIndex) : base;
        if (flowColor == null) flowColor = base;
        canvas.setColor(ColorRegistry.applyAlpha(flowColor, 0.28f));
        canvas.fillPolygon(xs, ys, xs.length);
    }
}
