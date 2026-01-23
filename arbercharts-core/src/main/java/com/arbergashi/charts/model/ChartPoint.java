package com.arbergashi.charts.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Enterprise data point representation for the ArberGashi engine.
 * Optimized for JDK 25+: value-object semantics and fluent API.
 * Suitable for high-frequency trading, scientific data and IoT sensor streams.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-12-30
 */
public record ChartPoint(
        double x,
        double y,
        double weight,
        double min,
        double max,
        String label
) implements Serializable, Comparable<ChartPoint> {

    @Serial
    private static final long serialVersionUID = 4L;

    /**
     * Compact canonical constructor with validation for numerical stability.
     */
    public ChartPoint {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("X and Y coordinates must be finite numbers.");
        }

        /* Null-safe labeling: intern repeated labels to reduce memory usage */
        label = (label == null || label.isBlank()) ? "" : label.intern();
    }

    /* Secondary constructors (overloads) */

    public ChartPoint(double x, double y) {
        this(x, y, 0.0, y, y, "");
    }

    public ChartPoint(double x, double y, String label) {
        this(x, y, 0.0, y, y, label);
    }

    /* Fluent API transformations (immutable pattern) */

    public static ChartPoint of(double x, double y) {
        return new ChartPoint(x, y);
    }

    /**
     * Specialized for candlestick semantics: y==close, weight==open.
     */
    public static ChartPoint ofOHLC(double x, double open, double high, double low, double close) {
        return new ChartPoint(x, close, open, low, high, "");
    }

    /* Business logic helpers */

    /**
     * Specialized for error bars or box plots.
     */
    public static ChartPoint ofRange(double x, double y, double min, double max) {
        return new ChartPoint(x, y, 0.0, min, max, "");
    }

    /**
     * Returns a copy with the provided weight, original remains unchanged.
     */
    public ChartPoint withWeight(double weight) {
        return new ChartPoint(x, y, weight, min, max, label);
    }

    /**
     * Returns a copy with a new label.
     */
    public ChartPoint withLabel(String label) {
        return new ChartPoint(x, y, weight, min, max, label);
    }

    /* Static factory methods (modern style) */

    /**
     * Checks whether the point lies within its min/max range.
     */
    public boolean isWithinRange() {
        return y >= min && y <= max;
    }

    /**
     * Computes the span between max and min (e.g., volatility).
     */
    public double delta() {
        return max - min;
    }

    @Override
    public int compareTo(ChartPoint other) {
        int cmp = Double.compare(this.x, other.x);
        return (cmp != 0) ? cmp : Double.compare(this.y, other.y);
    }
}