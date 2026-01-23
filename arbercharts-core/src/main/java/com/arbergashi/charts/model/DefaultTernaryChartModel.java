package com.arbergashi.charts.model;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation for a ternary chart model.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * @see TernaryChartModel
 */
public class DefaultTernaryChartModel implements TernaryChartModel {

    private final List<TernaryPoint> data;
    private final List<String> componentLabels;
    private final AtomicLong updateStamp = new AtomicLong(0);
    private String name;
    private Color color;

    public DefaultTernaryChartModel(List<TernaryPoint> data, List<String> componentLabels) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null.");
        }
        if (componentLabels == null || componentLabels.size() != 3) {
            throw new IllegalArgumentException("There must be exactly 3 component labels.");
        }
        this.data = data;
        this.componentLabels = componentLabels;
    }

    @Override
    public List<TernaryPoint> getTernaryData() {
        return Collections.unmodifiableList(data);
    }

    @Override
    public List<String> getComponentLabels() {
        return Collections.unmodifiableList(componentLabels);
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
        return data.size();
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
        data.clear();
        updateStamp.incrementAndGet();
    }

    public void addChangeListener(ChartModelListener listener) {
    }

    public void removeChangeListener(ChartModelListener listener) {
    }

    /**
     * A simple, immutable implementation of TernaryPoint.
     */
    public static record DefaultTernaryPoint(double a, double b, double c) implements TernaryPoint {
        @Override
        public double getA() {
            return a;
        }

        @Override
        public double getB() {
            return b;
        }

        @Override
        public double getC() {
            return c;
        }
    }
}
