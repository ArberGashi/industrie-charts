package com.arbergashi.charts.core.geometry;

/**
 * Core-safe insets (double precision).
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public record ArberInsets(double top, double left, double bottom, double right) {
    public double getTop() {
        return top;
    }

    public double getLeft() {
        return left;
    }

    public double getBottom() {
        return bottom;
    }

    public double getRight() {
        return right;
    }
}
