package com.arbergashi.charts.api.types;

/**
 * Core-safe point (double precision).
  * @author Arber Gashi
  * @version 2.0.0
  * @since 2026-01-30
 */
public final class ArberPoint {
    private double x;
    private double y;

    public ArberPoint() {
        this(0.0, 0.0);
    }

    public ArberPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setLocation(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double distanceSq(ArberPoint other) {
        if (other == null) return Double.POSITIVE_INFINITY;
        double dx = x - other.x;
        double dy = y - other.y;
        return dx * dx + dy * dy;
    }

    public double distanceSq(double ox, double oy) {
        double dx = x - ox;
        double dy = y - oy;
        return dx * dx + dy * dy;
    }
}
