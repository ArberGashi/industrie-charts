package com.arbergashi.charts.render.grid;

import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;

/**
 * Reusable equilateral geometry for ternary grids and renderers.
 * Component A is at the top, B at the lower left, and C at the lower right.
 */
public final class TernaryGeometry {
    private double centerX;
    private double apexY;
    private double baseY;
    private double halfSide;

    /**
     * Fits a centered equilateral triangle inside the plot with six percent padding.
     * @param bounds plot bounds in canvas units
     * @return this geometry
     */
    public TernaryGeometry setBounds(ArberRect bounds) {
        double width = Math.max(0.0, bounds.width());
        double height = Math.max(0.0, bounds.height());
        double padding = Math.min(width, height) * 0.06;
        double side = Math.min(width - 2.0 * padding, (height - 2.0 * padding) * 2.0 / Math.sqrt(3.0));
        double triangleHeight = side * Math.sqrt(3.0) * 0.5;
        centerX = bounds.centerX();
        apexY = bounds.centerY() - triangleHeight * 0.5;
        baseY = bounds.centerY() + triangleHeight * 0.5;
        halfSide = side * 0.5;
        return this;
    }

    /** @return horizontal coordinate of component A */
    public double ax() { return centerX; }
    /** @return vertical coordinate of component A */
    public double ay() { return apexY; }
    /** @return horizontal coordinate of component B */
    public double bx() { return centerX - halfSide; }
    /** @return vertical coordinate of component B */
    public double by() { return baseY; }
    /** @return horizontal coordinate of component C */
    public double cx() { return centerX + halfSide; }
    /** @return vertical coordinate of component C */
    public double cy() { return baseY; }

    /**
     * Maps non-negative finite components to a normalized composition.
     * Invalid or empty compositions leave the output unchanged.
     * @param a component A
     * @param b component B
     * @param c component C
     * @param out reusable result
     * @return whether a valid composition was mapped
     */
    public boolean map(double a, double b, double c, ArberPoint out) {
        double max = Math.max(a, Math.max(b, c));
        if (!(a >= 0.0 && b >= 0.0 && c >= 0.0 && max > 0.0 && Double.isFinite(max))) {
            return false;
        }
        a /= max;
        b /= max;
        c /= max;
        double sum = a + b + c;
        out.setLocation(centerX + (c - b) / sum * halfSide,
                apexY + (b + c) / sum * (baseY - apexY));
        return true;
    }
}
