package com.arbergashi.charts.model;

/**
 * An extension of ChartModel for hierarchical data structures, such as
 * those required by Sunburst or Treemap charts.
 *
 * @param <T> The type of the node used in the hierarchy.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
 */
public interface HierarchicalChartModel<T> extends ChartModel {

    /**
     * Returns the root node of the hierarchy.
     *
     * @return The root node.
     */
    Node<T> getRootNode();

    /**
     * Represents a node in the hierarchical data structure.
     * Each node has a name, a value, and can have children.
     */
    interface Node<T> {
        String getLabel();

        double getValue();

        java.util.List<Node<T>> getChildren();
    }
}
