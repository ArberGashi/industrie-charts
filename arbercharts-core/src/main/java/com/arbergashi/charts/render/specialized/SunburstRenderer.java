package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.LabelCache;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Sunburst renderer: hierarchical ring visualization optimized for Swing.
 * Expects ChartPoint.label() to contain paths like "root/child/grandchild" and weight in y.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
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

    private final transient Arc2D.Double arc = new Arc2D.Double();
    private final transient LabelCache labelCache = new LabelCache();
    private final transient Path2D.Double ringPath = new Path2D.Double();
    private final transient Node root = new Node("root", "", 0);
    private transient Node[] nodePool;
    private transient int nodePoolSize;

    public SunburstRenderer() {
        super("sunburst");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        Rectangle2D bounds = context.plotBounds();
        double cx = bounds.getCenterX();
        double cy = bounds.getCenterY();
        double radius = Math.min(bounds.getWidth(), bounds.getHeight()) * 0.45;

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

        Object prevAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object prevStroke = g2.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double start = 0.0;
        int i = 0;
        var theme = resolveTheme(context);
        for (Node child : root.children.values()) {
            double angle = child.value / root.value * 360.0;
            Color base = seriesOrBase(model, context, i);
            drawSegment(g2, child, base, i, root.value, start, angle, cx, cy, radius, maxDepth, context);

            // draw label at mid-angle for top level
            double mid = Math.toRadians(start + angle / 2.0);
            double lx = cx + Math.cos(mid) * (radius * 0.62);
            double ly = cy + Math.sin(mid) * (radius * 0.62);
            labelCache.drawLabel(g2, child.name, g2.getFont(), themeAxisLabel(context), (float) lx, (float) ly);

            start += angle;
            i++;
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, prevAA);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, prevStroke);
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

    private void drawSegment(Graphics2D g2, Node node, Color baseColor, int index, double rootTotal,
                             double start, double sweep, double cx, double cy, double radius,
                             int maxDepth, PlotContext context) {
        if (node.value <= 0) return;
        if (node.depth <= 0) return;

        double inner = radius * ((node.depth - 1) / (double) maxDepth);
        double outer = radius * (node.depth / (double) maxDepth);
        Shape segShape = buildRing(cx, cy, inner, outer, start, sweep);

        Color segColor = deriveColor(baseColor, node, index);
        g2.setColor(segColor);
        g2.fill(segShape);

        if (!node.children.isEmpty()) {
            double childStart = start;
            int childIndex = 0;
            for (Node child : node.children.values()) {
                double childSweep = sweep * (child.value / node.value);
                Color childBase = deriveChildBase(segColor, childIndex, node.children.size());
                drawSegment(g2, child, childBase, childIndex, rootTotal, childStart, childSweep, cx, cy, radius, maxDepth, context);
                childStart += childSweep;
                childIndex++;
            }
        }
    }

    private Shape buildRing(double cx, double cy, double inner, double outer, double start, double sweep) {
        if (inner <= 1.0) {
            arc.setFrame(cx - outer, cy - outer, outer * 2, outer * 2);
            arc.setAngleStart(start);
            arc.setAngleExtent(sweep);
            arc.setArcType(Arc2D.PIE);
            return arc;
        }

        ringPath.reset();
        arc.setFrame(cx - outer, cy - outer, outer * 2, outer * 2);
        arc.setAngleStart(start);
        arc.setAngleExtent(sweep);
        arc.setArcType(Arc2D.OPEN);
        ringPath.append(arc, false);

        arc.setFrame(cx - inner, cy - inner, inner * 2, inner * 2);
        arc.setAngleStart(start + sweep);
        arc.setAngleExtent(-sweep);
        arc.setArcType(Arc2D.OPEN);
        ringPath.append(arc, true);
        ringPath.closePath();
        return ringPath;
    }

    private Color deriveColor(Color base, Node node, int index) {
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        float hueShift = (index % 6) * 0.02f;
        float hue = (hsb[0] + hueShift) % 1.0f;
        float sat = Math.min(1.0f, hsb[1] * (0.9f - node.depth * 0.03f));
        float bri = Math.min(1.0f, hsb[2] * (0.9f + node.depth * 0.05f));
        int rgb = Color.HSBtoRGB(hue, sat, bri);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return RendererAllocationCache.getColor(this, "sunburst." + node.path, r, g, b);
    }

    private Color deriveChildBase(Color parent, int index, int total) {
        float[] hsb = Color.RGBtoHSB(parent.getRed(), parent.getGreen(), parent.getBlue(), null);
        float shift = (index / (float) Math.max(1, total)) * 0.04f;
        float hue = (hsb[0] + shift) % 1.0f;
        float sat = Math.min(1.0f, hsb[1] * 0.95f);
        float bri = Math.min(1.0f, hsb[2] * 0.98f);
        int rgb = Color.HSBtoRGB(hue, sat, bri);
        return new Color(rgb);
    }
}
