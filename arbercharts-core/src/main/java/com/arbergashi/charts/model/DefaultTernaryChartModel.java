package com.arbergashi.charts.model;
import com.arbergashi.charts.api.types.ArberColor;

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
    private ArberColor color;

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

    public DefaultTernaryChartModel setName(String name) {
        this.name = name;
        updateStamp.incrementAndGet();
        return this;
    }

    @Override
    public ArberColor getColor() {
        return color;
    }

    @Override
    public DefaultTernaryChartModel setColor(ArberColor color) {
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
    }

    public double getY(int index) {
        return 0;
    }

    public void setPoint(ChartPoint point) {
        throw new UnsupportedOperationException();
    }

    public DefaultTernaryChartModel setPoints(List<ChartPoint> points) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        data.clear();
        updateStamp.incrementAndGet();
    }

    public void setChangeListener(ChartModelListener listener) {
    }

    public void removeChangeListener(ChartModelListener listener) {
    }

    /**
     * A simple implementation of TernaryPoint.
     */
    public static final class DefaultTernaryPoint implements TernaryPoint {
        private double a;
        private double b;
        private double c;

        public DefaultTernaryPoint(double a, double b, double c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        @Override
        public double getA() {
            return a;
        }

        public DefaultTernaryPoint setA(double a) {
            this.a = a;
        return this;
        }

        @Override
        public double getB() {
            return b;
        }

        public DefaultTernaryPoint setB(double b) {
            this.b = b;
        return this;
        }

        @Override
        public double getC() {
            return c;
        }

        public DefaultTernaryPoint setC(double c) {
            this.c = c;
        return this;
        }
    }
}
