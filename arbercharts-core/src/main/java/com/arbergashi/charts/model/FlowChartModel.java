package com.arbergashi.charts.model;

import java.util.List;

/**
 * An extension of ChartModel for flow-based data, such as that required by
 * Sankey or Alluvial diagrams. It describes a set of nodes and the links
 * (edges) that flow between them.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public interface FlowChartModel extends ChartModel {

    /**
     * Returns the list of all nodes in the diagram.
     */
    List<? extends Node> getNodes();

    /**
     * Returns the list of all links connecting the nodes.
     */
    List<? extends Link> getLinks();

    /**
     * Represents a link (or edge) in the flow diagram.
     */
    interface Link {
        /**
         * Returns the source node ID.
         *
         * @return source node ID
         */
        String getSource();

        /**
         * Returns the target node ID.
         *
         * @return target node ID
         */
        String getTarget();

        /**
         * Returns the flow magnitude.
         *
         * @return flow value
         */
        double getValue();
    }

    /**
     * Represents a node in the flow diagram.
     */
    interface Node {
        /**
         * Returns the unique node ID.
         *
         * @return node ID
         */
        String getId();

        /**
         * Returns the display label for the node.
         *
         * @return node label
         */
        String getLabel();
    }
}
