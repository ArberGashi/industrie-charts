package com.arbergashi.charts.core.geometry;

import com.arbergashi.charts.api.types.ArberPoint;

/**
 * Core-safe rectangle (double precision).
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public record ArberRect(double x, double y, double width, double height) {
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double minX() {
        return x;
    }

    public double minY() {
        return y;
    }

    public double getMinX() {
        return x;
    }

    public double getMinY() {
        return y;
    }

    public double maxX() {
        return x + width;
    }

    public double maxY() {
        return y + height;
    }

    public double getCenterX() {
        return x + width * 0.5;
    }

    public double getCenterY() {
        return y + height * 0.5;
    }

    public double centerX() {
        return getCenterX();
    }

    public double centerY() {
        return getCenterY();
    }

    public double getMaxX() {
        return x + width;
    }

    public double getMaxY() {
        return y + height;
    }

    public boolean contains(ArberPoint point) {
        if (point == null) return false;
        double px = point.x();
        double py = point.y();
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
}
