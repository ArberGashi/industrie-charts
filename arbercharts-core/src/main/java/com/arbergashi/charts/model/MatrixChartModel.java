package com.arbergashi.charts.model;

import java.util.List;
/**
 * An extension of ChartModel for matrix-based data, such as that required
 * by Chord Diagrams or adjacency matrices.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * @see ChartModel
 */
public interface MatrixChartModel extends ChartModel {

    /**
     * Returns the data as a 2D matrix.
     * The matrix represents the flow or relationship between entities.
     *
     * @return A 2D double array where matrix[i][j] is the value from entity i to j.
     */
    double[][] getMatrix();

    /**
     * Returns the labels for the entities (nodes) in the matrix.
     * The size of this list should match the dimensions of the matrix.
     *
     * @return A list of labels for each entity.
     */
    List<String> getEntityLabels();
}
