package com.arbergashi.charts.core.geometry;

/**
 * Core-safe size (double precision).
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public record ArberSize(double width, double height) {
    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}
