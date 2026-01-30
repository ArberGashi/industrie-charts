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
import com.arbergashi.charts.util.ColorRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sunburst renderer: hierarchical ring visualization.
 * Expects ChartPoint.getLabel() to contain paths like "root/child/grandchild" and weight in y.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class SunburstRenderer extends BaseRenderer {

    private static final class Node {
        String name;
        String path;
        int depth;
        final Map<String, Node> children = new LinkedHashMap<>();
        double value;

        Node() {
            this("", "", 0);
        }

        Node(String name, String path, int depth) {
            this.name = name;
            this.path = path;
            this.depth = depth;
        }

        void reset(String name, String path, int depth) {
            this.name = name;
            this.path = path;
            this.depth = depth;
            this.value = 0.0;
            this.children.clear();
        }
    }

    static {
        RendererRegistry.register("sunburst", new RendererDescriptor("sunburst", "renderer.sunburst", "/icons/sunburst.svg"), SunburstRenderer::new);
    }

    private final transient Node root = new Node("root", "", 0);
    private transient Node[] nodePool;
    private transient int nodePoolSize;

    public SunburstRenderer() {
        super("sunburst");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        ArberRect bounds = context.getPlotBounds();
        double cx = bounds.centerX();
        double cy = bounds.centerY();
        double radius = Math.min(bounds.width(), bounds.height()) * 0.45;

        Map<String, Node> nodes = RendererAllocationCache.getMap(this, "nodes");
        root.reset("root", "", 0);
        int maxDepth = 0;
        int poolIndex = 0;

        for (int i = 0; i < count; i++) {
            String label = model.getLabel(i);
            if (label == null || label.isEmpty()) continue;
            double weight = model.getY(i);
            if (!(weight > 0)) continue;

            String[] parts = label.split("/");
            if (parts.length == 0) continue;

            Node current = root;
            current.value += weight;
            String pathKey = "";
            for (int d = 0; d < parts.length; d++) {
                String part = parts[d].trim();
                if (part.isEmpty()) continue;
                pathKey = pathKey.isEmpty() ? part : pathKey + "/" + part;
                Node child = nodes.get(pathKey);
                if (child == null) {
                    child = acquireNode(poolIndex++, part, pathKey, d + 1);
                    nodes.put(pathKey, child);
                    current.children.put(part, child);
                } else if (!current.children.containsKey(part)) {
                    current.children.put(part, child);
                }
                child.value += weight;
                current = child;
                if (child.depth > maxDepth) maxDepth = child.depth;
            }
        }

        if (root.value <= 0 || root.children.isEmpty()) return;

        double start = 0.0;
        int i = 0;
        for (Node child : root.children.values()) {
            double angle = child.value / root.value * 360.0;
            ArberColor base = seriesOrBase(model, context, i);
            drawSegment(canvas, child, base, i, root.value, start, angle, cx, cy, radius, maxDepth);
            start += angle;
            i++;
        }
    }

    private Node acquireNode(int index, String name, String path, int depth) {
        if (nodePool == null || index >= nodePool.length) {
            int newSize = Math.max(index + 1, nodePool == null ? 16 : nodePool.length * 2);
            Node[] next = new Node[newSize];
            if (nodePool != null) {
                System.arraycopy(nodePool, 0, next, 0, nodePool.length);
            }
            nodePool = next;
        }
        Node node = nodePool[index];
        if (node == null) {
            node = new Node();
            nodePool[index] = node;
        }
        node.reset(name, path, depth);
        nodePoolSize = Math.max(nodePoolSize, index + 1);
        return node;
    }

    private void drawSegment(ArberCanvas canvas, Node node, ArberColor baseColor, int index, double rootTotal,
                             double start, double sweep, double cx, double cy, double radius,
                             int maxDepth) {
        if (node.value <= 0) return;
        if (node.depth <= 0) return;

        double inner = radius * ((node.depth - 1) / (double) maxDepth);
        double outer = radius * (node.depth / (double) maxDepth);

        ArberColor segColor = deriveColor(baseColor, node, index);
        canvas.setColor(segColor);
        fillRing(canvas, cx, cy, inner, outer, start, sweep);

        if (!node.children.isEmpty()) {
            double childStart = start;
            int childIndex = 0;
            for (Node child : node.children.values()) {
                double childSweep = sweep * (child.value / node.value);
                ArberColor childBase = deriveChildBase(segColor, childIndex, node.children.size());
                drawSegment(canvas, child, childBase, childIndex, rootTotal, childStart, childSweep, cx, cy, radius, maxDepth);
                childStart += childSweep;
                childIndex++;
            }
        }
    }

    private void fillRing(ArberCanvas canvas, double cx, double cy, double inner, double outer, double startDeg, double sweepDeg) {
        double startRad = Math.toRadians(startDeg);
        double endRad = Math.toRadians(startDeg + sweepDeg);
        double sweep = Math.abs(endRad - startRad);
        int segs = Math.max(6, Math.min(72, (int) Math.ceil(sweep * outer / 20.0)));
        int count = segs * 2 + 2;
        float[] xs = RendererAllocationCache.getFloatArray(this, "sunburst.ring.x", count);
        float[] ys = RendererAllocationCache.getFloatArray(this, "sunburst.ring.y", count);

        int idx = 0;
        for (int i = 0; i <= segs; i++) {
            double t = startRad + (endRad - startRad) * (i / (double) segs);
            xs[idx] = (float) (cx + Math.cos(t) * outer);
            ys[idx] = (float) (cy + Math.sin(t) * outer);
            idx++;
        }
        for (int i = segs; i >= 0; i--) {
            double t = startRad + (endRad - startRad) * (i / (double) segs);
            xs[idx] = (float) (cx + Math.cos(t) * inner);
            ys[idx] = (float) (cy + Math.sin(t) * inner);
            idx++;
        }
        canvas.fillPolygon(xs, ys, idx);
    }

    private ArberColor deriveColor(ArberColor base, Node node, int index) {
        if (base == null) return ArberColor.TRANSPARENT;
        float shift = (index % 6) * 0.06f;
        float factor = 0.9f - (node.depth * 0.05f);
        return ColorRegistry.adjustBrightness(base, Math.max(0.4, 1.0 - shift) * Math.max(0.4, factor));
    }

    private ArberColor deriveChildBase(ArberColor base, int childIndex, int childCount) {
        if (base == null) return ArberColor.TRANSPARENT;
        double factor = 0.85 - (childIndex / (double) Math.max(1, childCount)) * 0.25;
        return ColorRegistry.adjustBrightness(base, Math.max(0.35, factor));
    }
}
