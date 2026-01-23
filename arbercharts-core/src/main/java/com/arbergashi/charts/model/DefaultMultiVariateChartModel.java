package com.arbergashi.charts.model;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation for a multivariate chart model.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * @see MultiVariateChartModel
 */
public class DefaultMultiVariateChartModel implements MultiVariateChartModel {

    private final List<String> dimensionLabels;
    private final List<DefaultMultiVariatePoint> points;
    private final AtomicLong updateStamp = new AtomicLong(0);
    private String name;

    public DefaultMultiVariateChartModel(List<String> dimensionLabels, List<DefaultMultiVariatePoint> points) {
        this.dimensionLabels = dimensionLabels;
        this.points = points;
    }

    @Override
    public List<String> getDimensionLabels() {
        return Collections.unmodifiableList(dimensionLabels);
    }

    @Override
    public List<DefaultMultiVariatePoint> getMultiVariatePoints() {
        return Collections.unmodifiableList(points);
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
        return null;
    }

    @Override
    public void setColor(Color color) {
    }

    @Override
    public long getUpdateStamp() {
        return updateStamp.get();
    }

    // --- Legacy/Unsupported methods ---
    public int getPointCount() {
        return points.size();
    }

    public double getX(int index) {
        return 0;
    }

    public double getY(int index) {
        return 0;
    }

    public void addPoint(ChartPoint point) {
        throw new UnsupportedOperationException();
    }

    public void addPoints(List<ChartPoint> points) {
        throw new UnsupportedOperationException();
    }

    public void setPoints(List<ChartPoint> points) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public void addChangeListener(ChartModelListener listener) {
    }

    public void removeChangeListener(ChartModelListener listener) {
    }

    public static class DefaultMultiVariatePoint implements MultiVariatePoint {
        private final double[] values;

        public DefaultMultiVariatePoint(double[] values) {
            this.values = values;
        }

        @Override
        public double[] getValues() {
            return values;
        }
    }
}
