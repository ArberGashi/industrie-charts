package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.HierarchicalChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
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
 * <h2>Features:</h2>
 * <ul>
 *     <li><b>Interactive Drill-Down:</b> Click segments to zoom in/out of the hierarchy.</li>
 *     <li><b>Path Highlighting:</b> Highlights the entire path to a segment on hover.</li>
 *     <li><b>Smart Labels:</b> Rotates and fits labels within segments.</li>
 *     <li><b>Dynamic Layout:</b> Ring thickness adapts to hierarchy depth.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
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
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        if (!(model instanceof HierarchicalChartModel<?> hierarchicalModel)) {
            drawErrorMessage(g2, context);
            return;
        }

        this.rootNode = hierarchicalModel.getRootNode();
        if (this.displayRoot == null) {
            this.displayRoot = this.rootNode;
        }
        if (displayRoot == null) return;

        Rectangle bounds = context.plotBounds().getBounds();
        double cx = bounds.getCenterX();
        double cy = bounds.getCenterY();
        double maxRadius = Math.min(bounds.getWidth(), bounds.getHeight()) / 2.0 * 0.95;
        
        int maxDepth = calculateMaxDepth(displayRoot, 0);
        double ringThickness = (maxDepth > 0) ? maxRadius / maxDepth : maxRadius;

        this.renderTheme = resolveTheme(context);
        drawNode(g2, displayRoot, cx, cy, 0, 360, 0, ringThickness, 0, ringThickness, renderTheme);
    }

    private void drawNode(Graphics2D g2, HierarchicalChartModel.Node<?> node, double cx, double cy,
                          double startAngle, double arcAngle, double innerRadius, double outerRadius, int depth, double ringThickness,
                          ChartTheme theme) {

        if (arcAngle <= 0.1) return;

        boolean isHovered = hoverPath.contains(node);
        Color color = getColorForNode(node, depth);
        if (isHovered) {
            color = color.brighter();
        }
        
        g2.setColor(color);
        Arc2D arc = getArc(cx - outerRadius, cy - outerRadius, outerRadius * 2, outerRadius * 2, startAngle, arcAngle, Arc2D.PIE);
        g2.fill(arc);
        
        g2.setStroke(getCachedStroke(1.0f));
        g2.setColor(ColorUtils.withAlpha(theme.getBackground(), 0.5f));
        g2.draw(arc);

        drawLabel(g2, node.getLabel(), color, cx, cy, startAngle + arcAngle / 2.0, (innerRadius + outerRadius) / 2.0, outerRadius - innerRadius);

        List<? extends HierarchicalChartModel.Node<?>> children = node.getChildren();
        if (children.isEmpty()) return;

        double totalValue = node.getValue();
        if (totalValue <= 0) return;

        double currentAngle = startAngle;
        for (HierarchicalChartModel.Node<?> child : children) {
            double childArcAngle = (child.getValue() / totalValue) * arcAngle;
            drawNode(g2, child, cx, cy, currentAngle, childArcAngle, outerRadius, outerRadius + ringThickness, depth + 1, ringThickness, theme);
            currentAngle += childArcAngle;
        }
    }
    
    private void drawLabel(Graphics2D g2, String text, Color segmentColor, double cx, double cy, double angleDeg, double radius, double thickness) {
        if (text == null || text.isEmpty()) return;
        
        g2.setFont(getCachedFont(10f, Font.PLAIN));
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) > thickness - ChartScale.scale(8)) return;

        AffineTransform old = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(Math.toRadians(-angleDeg));
        g2.translate(radius, 0);
        
        if (angleDeg > 90 && angleDeg < 270) {
            g2.rotate(Math.PI);
        }
        
        g2.setColor(ColorUtils.getContrastColor(segmentColor));
        g2.drawString(text, -fm.stringWidth(text) / 2.0f, fm.getAscent() / 2.0f);
        
        g2.setTransform(old);
    }

    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        hoverPath.clear();
        List<HierarchicalChartModel.Node<?>> path = findPathToNodeAt(pixel, context);
        if (!path.isEmpty()) {
            hoverPath.addAll(path);
            return Optional.of(0); // Return a dummy index to trigger repaint
        }
        return Optional.empty();
    }
    
    private List<HierarchicalChartModel.Node<?>> findPathToNodeAt(Point2D pixel, PlotContext context) {
        if (displayRoot == null) return Collections.emptyList();
        
        Rectangle bounds = context.plotBounds().getBounds();
        double cx = bounds.getCenterX();
        double cy = bounds.getCenterY();
        
        double dx = pixel.getX() - cx;
        double dy = pixel.getY() - cy;
        
        double angle = Math.toDegrees(Math.atan2(-dy, dx));
        if (angle < 0) angle += 360;
        
        double maxRadius = Math.min(bounds.getWidth(), bounds.getHeight()) / 2.0 * 0.95;
        int maxDepth = calculateMaxDepth(displayRoot, 0);
        double ringThickness = (maxDepth > 0) ? maxRadius / maxDepth : maxRadius;
        
        List<HierarchicalChartModel.Node<?>> path = new ArrayList<>();
        if (searchNode(displayRoot, 0, 360, angle, Math.sqrt(dx * dx + dy * dy), 0, ringThickness, path)) {
            return path;
        }
        return Collections.emptyList();
    }
    
    private boolean searchNode(HierarchicalChartModel.Node<?> node, double startAngle, double arcAngle, double targetAngle, double targetRadius, int depth, double ringThickness, List<HierarchicalChartModel.Node<?>> path) {
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
                        path.add(node); // Add parent to path on the way back up
                        return true;
                    }
                    currentAngle += childArcAngle;
                }
            }
        }
        
        // This is the deepest node that was hit
        path.add(node);
        return true;
    }
    
    private void handleDrill(HierarchicalChartModel.Node<?> node) {
        if (node == null) return;
        if (node == displayRoot && !breadcrumb.isEmpty()) {
            displayRoot = breadcrumb.pop(); // Drill up
        } else if (!node.getChildren().isEmpty() && node != displayRoot) {
            breadcrumb.push(displayRoot); // Save current state
            displayRoot = node; // Drill down
        }
    }

    private int calculateMaxDepth(HierarchicalChartModel.Node<?> node, int depth) {
        if (node == null || node.getChildren().isEmpty()) {
            return depth;
        }
        int maxChildDepth = depth;
        for (HierarchicalChartModel.Node<?> child : node.getChildren()) {
            maxChildDepth = Math.max(maxChildDepth, calculateMaxDepth(child, depth + 1));
        }
        return maxChildDepth;
    }

    private Color getColorForNode(HierarchicalChartModel.Node<?> node, int depth) {
        ChartTheme t = (renderTheme != null) ? renderTheme : ChartThemes.defaultDark();
        if (node != null && breadcrumb.isEmpty() && rootNode != null) { // Top level
            return t.getSeriesColor(rootNode.getChildren().indexOf(node));
        }
        return t.getSeriesColor(Math.max(0, depth));
    }


    private void drawErrorMessage(Graphics2D g2, PlotContext context) {
        ChartTheme theme = (renderTheme != null) ? renderTheme : ChartThemes.defaultDark();
        g2.setColor(theme.getAccentColor());
        Rectangle bounds = context.plotBounds().getBounds();
        g2.drawString("SunburstRenderer requires a HierarchicalChartModel", bounds.x + 10, bounds.y + 20);
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
    public void handleClick(Point2D pixel, PlotContext context) {
        List<HierarchicalChartModel.Node<?>> path = findPathToNodeAt(pixel, context);
        if (!path.isEmpty()) {
            handleDrill(path.getFirst()); // Deepest node in path is the one clicked
        }
    }
}
