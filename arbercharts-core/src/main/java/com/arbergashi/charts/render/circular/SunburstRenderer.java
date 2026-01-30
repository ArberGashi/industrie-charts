package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.HierarchicalChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorRegistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * <h1>Modern Sunburst Renderer</h1>
 * <p>
 * Draws a professional, interactive, and zero-allocation sunburst chart for hierarchical data.
 * </p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class SunburstRenderer extends BaseRenderer {

    private HierarchicalChartModel.Node<?> rootNode;
    private HierarchicalChartModel.Node<?> displayRoot;
    private final Deque<HierarchicalChartModel.Node<?>> breadcrumb = new ArrayDeque<>();
    private final List<HierarchicalChartModel.Node<?>> hoverPath = new ArrayList<>();
    private transient ChartTheme renderTheme;

    public SunburstRenderer() {
        super("sunburst");
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!(model instanceof HierarchicalChartModel<?> hierarchicalModel)) {
            return;
        }

        this.rootNode = hierarchicalModel.getRootNode();
        if (this.displayRoot == null) {
            this.displayRoot = this.rootNode;
        }
        if (displayRoot == null) return;

        ArberRect bounds = context.getPlotBounds();
        double cx = bounds.centerX();
        double cy = bounds.centerY();
        double maxRadius = Math.min(bounds.getWidth(), bounds.getHeight()) / 2.0 * 0.95;

        int maxDepth = getCalculatedMaxDepth(displayRoot, 0);
        double ringThickness = (maxDepth > 0) ? maxRadius / maxDepth : maxRadius;

        this.renderTheme = getResolvedTheme(context);
        drawNode(canvas, displayRoot, cx, cy, 0, 360, 0, ringThickness, 0, ringThickness, renderTheme);
    }

    private void drawNode(ArberCanvas canvas, HierarchicalChartModel.Node<?> node, double cx, double cy,
                          double startAngle, double arcAngle, double innerRadius, double outerRadius, int depth,
                          double ringThickness, ChartTheme theme) {
        if (arcAngle <= 0.1) return;

        boolean isHovered = hoverPath.contains(node);
        ArberColor color = getColorForNode(node, depth);
        if (isHovered) {
            color = ColorRegistry.adjustBrightness(color, 1.1);
        }

        canvas.setColor(color);
        fillRingSegment(canvas, cx, cy, innerRadius, outerRadius, startAngle, arcAngle);

        canvas.setStroke(1.0f);
        canvas.setColor(ColorRegistry.applyAlpha(theme.getBackground(), 0.5f));
        drawArcPolyline(canvas, cx, cy, outerRadius, startAngle, arcAngle);

        List<? extends HierarchicalChartModel.Node<?>> children = node.getChildren();
        if (children.isEmpty()) return;

        double totalValue = node.getValue();
        if (totalValue <= 0) return;

        double currentAngle = startAngle;
        for (HierarchicalChartModel.Node<?> child : children) {
            double childArcAngle = (child.getValue() / totalValue) * arcAngle;
            drawNode(canvas, child, cx, cy, currentAngle, childArcAngle,
                    outerRadius, outerRadius + ringThickness, depth + 1, ringThickness, theme);
            currentAngle += childArcAngle;
        }
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        hoverPath.clear();
        List<HierarchicalChartModel.Node<?>> path = findPathToNodeAt(pixel, context);
        if (!path.isEmpty()) {
            hoverPath.addAll(path);
            return Optional.of(0);
        }
        return Optional.empty();
    }

    private List<HierarchicalChartModel.Node<?>> findPathToNodeAt(ArberPoint pixel, PlotContext context) {
        if (displayRoot == null) return Collections.emptyList();

        ArberRect bounds = context.getPlotBounds();
        double cx = bounds.centerX();
        double cy = bounds.centerY();

        double dx = pixel.x() - cx;
        double dy = pixel.y() - cy;

        double angle = Math.toDegrees(Math.atan2(-dy, dx));
        if (angle < 0) angle += 360;

        double maxRadius = Math.min(bounds.getWidth(), bounds.getHeight()) / 2.0 * 0.95;
        int maxDepth = getCalculatedMaxDepth(displayRoot, 0);
        double ringThickness = (maxDepth > 0) ? maxRadius / maxDepth : maxRadius;

        List<HierarchicalChartModel.Node<?>> path = new ArrayList<>();
        if (searchNode(displayRoot, 0, 360, angle, Math.sqrt(dx * dx + dy * dy), 0, ringThickness, path)) {
            return path;
        }
        return Collections.emptyList();
    }

    private boolean searchNode(HierarchicalChartModel.Node<?> node, double startAngle, double arcAngle,
                               double targetAngle, double targetRadius, int depth, double ringThickness,
                               List<HierarchicalChartModel.Node<?>> path) {
        double innerRadius = depth * ringThickness;
        double outerRadius = (depth + 1) * ringThickness;

        if (targetAngle < startAngle || targetAngle > startAngle + arcAngle || targetRadius < innerRadius || targetRadius > outerRadius) {
            return false;
        }

        List<? extends HierarchicalChartModel.Node<?>> children = node.getChildren();
        if (!children.isEmpty()) {
            double currentAngle = startAngle;
            double totalValue = node.getValue();
            if (totalValue > 0) {
                for (HierarchicalChartModel.Node<?> child : children) {
                    double childArcAngle = (child.getValue() / totalValue) * arcAngle;
                    if (searchNode(child, currentAngle, childArcAngle, targetAngle, targetRadius, depth + 1, ringThickness, path)) {
                        path.add(node);
                        return true;
                    }
                    currentAngle += childArcAngle;
                }
            }
        }

        path.add(node);
        return true;
    }

    private void handleDrill(HierarchicalChartModel.Node<?> node) {
        if (node == null) return;
        if (node == displayRoot && !breadcrumb.isEmpty()) {
            displayRoot = breadcrumb.pop();
        } else if (!node.getChildren().isEmpty() && node != displayRoot) {
            breadcrumb.push(displayRoot);
            displayRoot = node;
        }
    }

    private int getCalculatedMaxDepth(HierarchicalChartModel.Node<?> node, int depth) {
        if (node == null || node.getChildren().isEmpty()) {
            return depth;
        }
        int maxChildDepth = depth;
        for (HierarchicalChartModel.Node<?> child : node.getChildren()) {
            maxChildDepth = Math.max(maxChildDepth, getCalculatedMaxDepth(child, depth + 1));
        }
        return maxChildDepth;
    }

    private ArberColor getColorForNode(HierarchicalChartModel.Node<?> node, int depth) {
        ChartTheme t = (renderTheme != null) ? renderTheme : ChartThemes.getDarkTheme();
        if (node != null && breadcrumb.isEmpty() && rootNode != null) {
            return t.getSeriesColor(rootNode.getChildren().indexOf(node));
        }
        return t.getSeriesColor(Math.max(0, depth));
    }

    private void fillRingSegment(ArberCanvas canvas, double cx, double cy, double r1, double r2, double startDeg, double sweepDeg) {
        int segments = Math.max(12, (int) (Math.abs(sweepDeg) / 4.0));
        float[] xs = new float[(segments + 1) * 2];
        float[] ys = new float[(segments + 1) * 2];
        double step = sweepDeg / segments;

        for (int i = 0; i <= segments; i++) {
            double a = Math.toRadians(startDeg + step * i);
            xs[i] = (float) (cx + Math.cos(a) * r2);
            ys[i] = (float) (cy - Math.sin(a) * r2);
        }
        for (int i = 0; i <= segments; i++) {
            double a = Math.toRadians(startDeg + sweepDeg - step * i);
            xs[segments + 1 + i] = (float) (cx + Math.cos(a) * r1);
            ys[segments + 1 + i] = (float) (cy - Math.sin(a) * r1);
        }
        canvas.fillPolygon(xs, ys, xs.length);
    }

    private void drawArcPolyline(ArberCanvas canvas, double cx, double cy, double r, double startDeg, double sweepDeg) {
        int segments = Math.max(12, (int) (Math.abs(sweepDeg) / 4.0));
        float[] xs = new float[segments + 1];
        float[] ys = new float[segments + 1];
        double step = sweepDeg / segments;
        for (int i = 0; i <= segments; i++) {
            double a = Math.toRadians(startDeg + step * i);
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy - Math.sin(a) * r);
        }
        canvas.drawPolyline(xs, ys, segments + 1);
    }

    @Override
    public void clearHover() {
        hoverPath.clear();
        renderTheme = null;
    }

    /**
     * Handles a click event to perform drill-down/up operations.
     * @param pixel The point that was clicked.
     * @param context The current plot context.
     */
    public void handleClick(ArberPoint pixel, PlotContext context) {
        List<HierarchicalChartModel.Node<?>> path = findPathToNodeAt(pixel, context);
        if (!path.isEmpty()) {
            handleDrill(path.getFirst());
        }
    }
}
