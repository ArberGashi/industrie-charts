package com.arbergashi.charts.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in a hierarchical data structure, used for charts like Sunburst or Treemaps.
 *
 * @param name     The name of the data point (e.g., "Users", "System").
 * @param value    The quantitative value of this node. For parent nodes, this is often the sum of its children.
 * @param children A list of child nodes.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public record HierarchicalDataPoint(
        String name,
        double value,
        List<HierarchicalDataPoint> children
) {
    /**
     * Creates a hierarchical data point, ensuring the children list is never null.
     */
    public HierarchicalDataPoint {
        if (children == null) {
            children = new ArrayList<>();
        }
    }

    /**
     * Convenience constructor for a leaf node (a node with no children).
     *
     * @param name  The name of the leaf node.
     * @param value The value of the leaf node.
     */
    public HierarchicalDataPoint(String name, double value) {
        this(name, value, new ArrayList<>());
    }

    /**
     * Checks if this node is a leaf (has no children).
     *
     * @return true if this node has no children, false otherwise.
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }
}
