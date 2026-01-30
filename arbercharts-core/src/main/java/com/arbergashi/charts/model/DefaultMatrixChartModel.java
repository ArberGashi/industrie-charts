package com.arbergashi.charts.model;
import com.arbergashi.charts.api.types.ArberColor;

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
    private ArberColor color;

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

    public DefaultMatrixChartModel setName(String name) {
        this.name = name;
        updateStamp.incrementAndGet();
        return this;
    }

    @Override
    public ArberColor getColor() {
        return color;
    }

    @Override
    public DefaultMatrixChartModel setColor(ArberColor color) {
        this.color = color;
        updateStamp.incrementAndGet();
        return this;
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


    public void setPoint(ChartPoint point) {
        throw new UnsupportedOperationException("Matrix models do not support direct point addition.");
    }

    public DefaultMatrixChartModel setPoints(List<ChartPoint> points) {
        throw new UnsupportedOperationException("Matrix models do not support direct point addition.");
    }

    public void clear() {
        throw new UnsupportedOperationException("Matrix models cannot be cleared this way.");
    }

    public void setChangeListener(ChartModelListener listener) {
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
