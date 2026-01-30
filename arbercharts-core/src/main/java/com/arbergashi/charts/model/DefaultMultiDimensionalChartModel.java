package com.arbergashi.charts.model;
import com.arbergashi.charts.api.types.ArberColor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
/**
 * Default implementation for a multi-dimensional chart model.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class DefaultMultiDimensionalChartModel implements MultiDimensionalChartModel {

    private final List<double[]> data;
    private final List<String> dimensionLabels;
    private final AtomicLong updateStamp = new AtomicLong(0);
    private String name;
    private ArberColor color;

    public DefaultMultiDimensionalChartModel(List<double[]> data, List<String> dimensionLabels) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty.");
        }
        if (dimensionLabels == null || dimensionLabels.size() != data.get(0).length) {
            throw new IllegalArgumentException("Number of labels must match the data dimension.");
        }
        this.data = data;
        this.dimensionLabels = dimensionLabels;
    }

    @Override
    public List<double[]> getMultiDimensionalData() {
        return Collections.unmodifiableList(data);
    }

    @Override
    public List<String> getDimensionLabels() {
        return Collections.unmodifiableList(dimensionLabels);
    }

    @Override
    public String getName() {
        return name;
    }

    public DefaultMultiDimensionalChartModel setName(String name) {
        this.name = name;
        updateStamp.incrementAndGet();
        return this;
    }

    @Override
    public ArberColor getColor() {
        return color;
    }

    @Override
    public DefaultMultiDimensionalChartModel setColor(ArberColor color) {
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
        return data.size();
    }

    public double getX(int index) {
        return 0;
    } // Not applicable

    public double getY(int index) {
        return 0;
    } // Not applicable


    public void setPoint(ChartPoint point) {
        throw new UnsupportedOperationException("This model does not support direct point addition.");
    }

    public DefaultMultiDimensionalChartModel setPoints(List<ChartPoint> points) {
        throw new UnsupportedOperationException("This model does not support direct point addition.");
    }

    public void clear() {
        data.clear();
        updateStamp.incrementAndGet();
    }

    public void setChangeListener(ChartModelListener listener) {
    }

    public void removeChangeListener(ChartModelListener listener) {
    }
}
