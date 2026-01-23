package com.arbergashi.charts.model;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation for a hierarchical chart model.
 * It uses a generic Node class to build the tree structure.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class DefaultHierarchicalChartModel implements HierarchicalChartModel<DefaultHierarchicalChartModel.DefaultNode> {

    private final DefaultNode root;
    private final AtomicLong updateStamp = new AtomicLong(0);
    private String name;
    private Color color;

    public DefaultHierarchicalChartModel(DefaultNode root) {
        this.root = root;
    }

    @Override
    public DefaultNode getRootNode() {
        return root;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        updateStamp.incrementAndGet();
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color color) {
        this.color = color;
        updateStamp.incrementAndGet();
    }

    @Override
    public long getUpdateStamp() {
        return updateStamp.get();
    }

    public int getPointCount() {
        return 0;
    }

    // --- Legacy/Unsupported methods from ChartModel ---
    // These are not applicable to hierarchical data but must be implemented.

    public double getX(int index) {
        return 0;
    }

    public double getY(int index) {
        return 0;
    }

    public void addPoint(ChartPoint point) {
        throw new UnsupportedOperationException("Hierarchical models do not support direct point addition.");
    }

    public void addPoints(List<ChartPoint> points) {
        throw new UnsupportedOperationException("Hierarchical models do not support direct point addition.");
    }

    public void setPoints(List<ChartPoint> points) {
        throw new UnsupportedOperationException("Hierarchical models do not support direct point addition.");
    }

    public void clear() {
        throw new UnsupportedOperationException("Hierarchical models cannot be cleared this way.");
    }

    @Override
    public double[] getXData() {
        return new double[0];
    }

    @Override
    public double[] getYData() {
        return new double[0];
    }

    @Override
    public void addChangeListener(ChartModelListener listener) {
        // Listeners could be implemented if the model becomes mutable
    }

    @Override
    public void removeChangeListener(ChartModelListener listener) {
        // Listeners could be implemented if the model becomes mutable
    }

    public static class DefaultNode implements HierarchicalChartModel.Node<DefaultNode> {
        private final String label;
        private final List<Node<DefaultNode>> children = new ArrayList<>();
        private double value;

        public DefaultNode(String label, double value) {
            this.label = label;
            this.value = value;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public double getValue() {
            // If value is 0, calculate from children recursively
            if (this.value == 0 && !children.isEmpty()) {
                return children.stream().mapToDouble(Node::getValue).sum();
            }
            return value;
        }

        @Override
        public List<Node<DefaultNode>> getChildren() {
            return Collections.unmodifiableList(children);
        }

        public void addChild(DefaultNode child) {
            this.children.add(child);
        }
    }
}
