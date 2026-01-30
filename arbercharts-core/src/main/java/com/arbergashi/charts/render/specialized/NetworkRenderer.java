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

import java.util.List;

/**
 * NetworkRenderer visualizes graph data (nodes and edges).
 * It expects a FlowChartModel.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class NetworkRenderer extends BaseRenderer {

    private String[] nodeIds = new String[8];
    private int[] nodeX = new int[8];
    private int[] nodeY = new int[8];
    private ArberColor[] nodeColor = new ArberColor[8];
    private int nodesCap = nodeIds.length;

    public NetworkRenderer() {
        super("network");
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
        if (nodes.isEmpty()) return;

        int n = nodes.size();
        if (n > nodesCap) {
            int newCap = Math.max(n, nodesCap * 2);
            String[] nid = (String[]) RendererAllocationCache.getArray(this, "nodeIds", String.class, newCap);
            System.arraycopy(nodeIds, 0, nid, 0, Math.min(nodeIds.length, nid.length));
            nodeIds = nid;
            int[] nx = RendererAllocationCache.getIntArray(this, "nodeX", newCap);
            System.arraycopy(nodeX, 0, nx, 0, Math.min(nodeX.length, nx.length));
            nodeX = nx;
            int[] ny = RendererAllocationCache.getIntArray(this, "nodeY", newCap);
            System.arraycopy(nodeY, 0, ny, 0, Math.min(nodeY.length, ny.length));
            nodeY = ny;
            ArberColor[] nc = (ArberColor[]) RendererAllocationCache.getArray(this, "nodeColor", ArberColor.class, newCap);
            System.arraycopy(nodeColor, 0, nc, 0, Math.min(nodeColor.length, nc.length));
            nodeColor = nc;
            nodesCap = newCap;
        }

        ArberRect bounds = context.getPlotBounds();
        int w = Math.max(1, (int) bounds.width() - 40);
        int h = Math.max(1, (int) bounds.height() - 40);

        ArberColor baseColor = getSeriesColor(model);
        for (int i = 0; i < n; i++) {
            FlowChartModel.Node node = nodes.get(i);
            String id = node.getId();
            nodeIds[i] = id;
            int hash = id.hashCode();
            int rx = Math.abs((hash * 31) ^ (hash >>> 16));
            int ry = Math.abs((hash * 17) ^ (hash >>> 8));
            nodeX[i] = (int) bounds.x() + 20 + (rx % w);
            nodeY[i] = (int) bounds.y() + 20 + (ry % h);
            nodeColor[i] = isMultiColor() ? getColorForId(id, context) : baseColor;
        }

        float[] lineX = RendererAllocationCache.getFloatArray(this, "net.line.x", 2);
        float[] lineY = RendererAllocationCache.getFloatArray(this, "net.line.y", 2);
        canvas.setColor(ColorUtils.applyAlpha(themeGrid(context), 0.4f));
        canvas.setStroke(getSeriesStrokeWidth());

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
                lineX[0] = nodeX[si];
                lineY[0] = nodeY[si];
                lineX[1] = nodeX[ti];
                lineY[1] = nodeY[ti];
                canvas.drawPolyline(lineX, lineY, 2);
            }
        }

        float[] nodePolyX = RendererAllocationCache.getFloatArray(this, "net.node.x", 8);
        float[] nodePolyY = RendererAllocationCache.getFloatArray(this, "net.node.y", 8);
        for (int i = 0; i < n; i++) {
            ArberColor c = nodeColor[i];
            canvas.setColor(c);
            buildOctagon(nodeX[i], nodeY[i], 8, nodePolyX, nodePolyY);
            canvas.fillPolygon(nodePolyX, nodePolyY, 8);
        }
    }

    private ArberColor getColorForId(String id, PlotContext context) {
        int hash = id.hashCode();
        int idx = Math.abs(hash) % 6;
        return themeSeries(context, idx);
    }

    private static void buildOctagon(int cx, int cy, int r, float[] xs, float[] ys) {
        for (int i = 0; i < 8; i++) {
            double a = i * (Math.PI * 2.0 / 8.0);
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
    }
}
