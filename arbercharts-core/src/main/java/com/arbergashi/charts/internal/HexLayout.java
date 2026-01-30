package com.arbergashi.charts.internal;
/**
 * Helper for hexagon geometry. Precomputes the six corner offsets for a hex
 * of a given radius. This class is immutable and safe to reuse between frames.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class HexLayout {
    private final double[] offsetsX = new double[6];
    private final double[] offsetsY = new double[6];
    private final double radius;

    private HexLayout(double radius) {
        this.radius = radius;
        // compute offsets (pointy-top hexagon)
        for (int k = 0; k < 6; k++) {
            double angle = Math.PI / 3.0 * k + Math.PI / 6.0; // start at 30 degrees for pointy
            offsetsX[k] = radius * Math.cos(angle);
            offsetsY[k] = radius * Math.sin(angle);
        }
    }

    public static HexLayout of(double radius) {
        return new HexLayout(radius);
    }

    public double[] getOffsetsX() {
        return offsetsX;
    }

    public double[] getOffsetsY() {
        return offsetsY;
    }

    public double getRadius() {
        return radius;
    }
}
