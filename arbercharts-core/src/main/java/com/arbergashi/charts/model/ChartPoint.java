package com.arbergashi.charts.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Mutable chart point for rendering and modeling.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class ChartPoint implements Comparable<ChartPoint>, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private double x;
    private double y;
    private double weight;
    private double min;
    private double max;
    private String label;

    public ChartPoint(double x, double y) {
        this(x, y, 0.0, y, y, "");
    }

    public ChartPoint(double x, double y, double weight, double min, double max, String label) {
        this.x = x;
        this.y = y;
        this.weight = weight;
        this.min = min;
        this.max = max;
        this.label = label == null ? "" : label;
    }

    public static ChartPoint of(double x, double y) {
        return new ChartPoint(x, y);
    }

    public static ChartPoint ofRange(double x, double y, double min, double max) {
        return new ChartPoint(x, y, 0.0, min, max, "");
    }

    public static ChartPoint ofOHLC(double x, double open, double high, double low, double close) {
        return new ChartPoint(x, close, open, low, high, "");
    }

    public double getX() {
        return x;
    }

    public ChartPoint setX(double x) {
        this.x = x;
        return this;
    }

    public double getY() {
        return y;
    }

    public ChartPoint setY(double y) {
        this.y = y;
        return this;
    }

    public double getWeight() {
        return weight;
    }

    public ChartPoint setWeight(double weight) {
        this.weight = weight;
        return this;
    }

    public double getMin() {
        return min;
    }

    public ChartPoint setMin(double min) {
        this.min = min;
        return this;
    }

    public double getMax() {
        return max;
    }

    public ChartPoint setMax(double max) {
        this.max = max;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public ChartPoint setLabel(String label) {
        this.label = label == null ? "" : label;
        return this;
    }

    @Override
    public int compareTo(ChartPoint other) {
        int cmp = Double.compare(this.x, other.x);
        if (cmp != 0) {
            return cmp;
        }
        return Double.compare(this.y, other.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChartPoint)) {
            return false;
        }
        ChartPoint that = (ChartPoint) o;
        return Double.compare(x, that.x) == 0
                && Double.compare(y, that.y) == 0
                && Double.compare(weight, that.weight) == 0
                && Double.compare(min, that.min) == 0
                && Double.compare(max, that.max) == 0
                && Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, weight, min, max, label);
    }

    @Override
    public String toString() {
        return "ChartPoint{" +
                "x=" + x +
                ", y=" + y +
                ", weight=" + weight +
                ", min=" + min +
                ", max=" + max +
                ", label='" + label + '\'' +
                '}';
    }
}
