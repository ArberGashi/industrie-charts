package com.arbergashi.charts.model;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation for a matrix-based chart model.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class DefaultMatrixChartModel implements MatrixChartModel {

    private final double[][] matrix;
    private final List<String> labels;
    private final AtomicLong updateStamp = new AtomicLong(0);
    private String name;
    private Color color;

    public DefaultMatrixChartModel(double[][] matrix, List<String> labels) {
        if (matrix == null || matrix.length == 0 || matrix.length != matrix[0].length) {
            throw new IllegalArgumentException("Matrix must be a non-empty square matrix.");
        }
        if (labels == null || labels.size() != matrix.length) {
            throw new IllegalArgumentException("Number of labels must match the matrix dimension.");
        }
        this.matrix = matrix;
        this.labels = labels;
    }

    @Override
    public double[][] getMatrix() {
        return matrix;
    }

    @Override
    public List<String> getEntityLabels() {
        return Collections.unmodifiableList(labels);
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

    // --- Legacy/Unsupported methods ---

    public int getPointCount() {
        return matrix != null ? matrix.length : 0;
    }

    public double getX(int index) {
        return 0;
    }

    public double getY(int index) {
        return 0;
    }


    public void addPoint(ChartPoint point) {
        throw new UnsupportedOperationException("Matrix models do not support direct point addition.");
    }

    public void addPoints(List<ChartPoint> points) {
        throw new UnsupportedOperationException("Matrix models do not support direct point addition.");
    }

    public void setPoints(List<ChartPoint> points) {
        throw new UnsupportedOperationException("Matrix models do not support direct point addition.");
    }

    public void clear() {
        throw new UnsupportedOperationException("Matrix models cannot be cleared this way.");
    }

    public void addChangeListener(ChartModelListener listener) {
    }

    public void removeChangeListener(ChartModelListener listener) {
    }

    @Override
    public double[] getXData() {
        return new double[0];
    }

    @Override
    public double[] getYData() {
        return new double[0];
    }
}
