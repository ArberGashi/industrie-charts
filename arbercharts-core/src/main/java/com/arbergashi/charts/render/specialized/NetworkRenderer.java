package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FlowChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;


/**
 * NetworkRenderer visualizes graph data (nodes and edges).
 * It expects a FlowChartModel.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 *
 */
public final class NetworkRenderer extends BaseRenderer {

    private String[] nodeIds = new String[8];
    private int[] nodeX = new int[8];
    private int[] nodeY = new int[8];
    private Color[] nodeColor = new Color[8];
    // initial capacity mirrors the backing arrays
    private int nodesCap = nodeIds.length;

    public NetworkRenderer() {
        super("network");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        if (!(model instanceof FlowChartModel flowModel)) {
            drawErrorMessage(g2, context, "NetworkRenderer requires a FlowChartModel");
            return;
        }

        java.util.List<? extends FlowChartModel.Node> nodes = flowModel.getNodes();
        java.util.List<? extends FlowChartModel.Link> links = flowModel.getLinks();
        if (nodes.isEmpty()) return;

        int n = nodes.size();
        if (n > nodesCap) {
            int newCap = Math.max(n, nodesCap * 2);
            String[] nid = (String[]) com.arbergashi.charts.tools.RendererAllocationCache.getArray(this, "nodeIds", String.class, newCap);
            System.arraycopy(nodeIds, 0, nid, 0, Math.min(nodeIds.length, nid.length));
            nodeIds = nid;
            int[] nx = com.arbergashi.charts.tools.RendererAllocationCache.getIntArray(this, "nodeX", newCap);
            System.arraycopy(nodeX, 0, nx, 0, Math.min(nodeX.length, nx.length));
            nodeX = nx;
            int[] ny = com.arbergashi.charts.tools.RendererAllocationCache.getIntArray(this, "nodeY", newCap);
            System.arraycopy(nodeY, 0, ny, 0, Math.min(nodeY.length, ny.length));
            nodeY = ny;
            Color[] nc = (Color[]) com.arbergashi.charts.tools.RendererAllocationCache.getArray(this, "nodeColor", Color.class, newCap);
            System.arraycopy(nodeColor, 0, nc, 0, Math.min(nodeColor.length, nc.length));
            nodeColor = nc;
            nodesCap = newCap;
        }

        Rectangle bounds = context.plotBounds().getBounds();
        int w = Math.max(1, bounds.width - 40);
        int h = Math.max(1, bounds.height - 40);

        // Deterministic layout via id hash to avoid Random allocation
        Color baseColor = getSeriesColor(model);
        for (int i = 0; i < n; i++) {
            FlowChartModel.Node node = nodes.get(i);
            String id = node.getId();
            nodeIds[i] = id;
            int hash = id.hashCode();
            int rx = Math.abs((hash * 31) ^ (hash >>> 16));
            int ry = Math.abs((hash * 17) ^ (hash >>> 8));
            nodeX[i] = bounds.x + 20 + (rx % w);
            nodeY[i] = bounds.y + 20 + (ry % h);
            // Cache color
            nodeColor[i] = isMultiColor() ? getColorForId(id, context) : baseColor;
        }

        // Draw links - use linear scan to map ids to indices (nodes usually small)
        g2.setColor(com.arbergashi.charts.util.ColorUtils.withAlpha(themeGrid(context), 0.4f));
        g2.setStroke(getCachedStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        for (FlowChartModel.Link link : links) {
            String s = link.getSource();
            String t = link.getTarget();
            int si = -1, ti = -1;
            for (int i = 0; i < n; i++) {
                if (nodeIds[i].equals(s)) si = i;
                if (nodeIds[i].equals(t)) ti = i;
                if (si != -1 && ti != -1) break;
            }
            if (si != -1 && ti != -1) {
                g2.drawLine(nodeX[si], nodeY[si], nodeX[ti], nodeY[ti]);
            }
        }

        // Draw nodes
        for (int i = 0; i < n; i++) {
            g2.setColor(nodeColor[i]);
            g2.fill(getEllipse(nodeX[i] - 8, nodeY[i] - 8, 16, 16));
            g2.setColor(nodeColor[i].darker());
            g2.draw(getEllipse(nodeX[i] - 8, nodeY[i] - 8, 16, 16));
            g2.setColor(themeForeground(context));
            g2.drawString(nodes.get(i).getLabel(), nodeX[i] + 10, nodeY[i] + 5);
        }
    }

    private Color getColorForId(String id, PlotContext context) {
        int hash = id.hashCode();
        int idx = Math.abs(hash) % 6;
        return themeSeries(context, idx);
    }

    private void drawErrorMessage(Graphics2D g2, PlotContext context, String message) {
        g2.setColor(themeAccent(context));
        g2.drawString(message, context.plotBounds().getBounds().x + 10, context.plotBounds().getBounds().y + 20);
    }
}
