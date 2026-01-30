package com.arbergashi.charts.engine.spatial;

/**
 * 3D vector container for spatial projection.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class Vector3D {
    private double x;
    private double y;
    private double z;

    public Vector3D() {
    }

    public Vector3D(double x, double y, double z) {
        setX(x);
        setY(y);
        setZ(z);
    }

    public double getX() {
        return x;
    }

    public Vector3D setX(double x) {
        validateComponent(x, "x");
        this.x = x;
        return this;
    }

    public double getY() {
        return y;
    }

    public Vector3D setY(double y) {
        validateComponent(y, "y");
        this.y = y;
        return this;
    }

    public double getZ() {
        return z;
    }

    public Vector3D setZ(double z) {
        validateComponent(z, "z");
        this.z = z;
        return this;
    }

    public double getDotProduct(Vector3D other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vector3D setCrossProduct(Vector3D other) {
        double cx = y * other.z - z * other.y;
        double cy = z * other.x - x * other.z;
        double cz = x * other.y - y * other.x;
        this.x = cx;
        this.y = cy;
        this.z = cz;
        return this;
    }

    private static void validateComponent(double v, String label) {
        if (!Double.isFinite(v)) {
            throw new IllegalArgumentException("Vector3D " + label + " must be finite");
        }
    }
}
