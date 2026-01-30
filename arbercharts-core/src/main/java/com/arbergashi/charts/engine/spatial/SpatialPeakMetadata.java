package com.arbergashi.charts.engine.spatial;

/**
 * Captures peak anchors detected during spatial optimization.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class SpatialPeakMetadata {
    private final double[] x;
    private final double[] y;
    private final double[] z;
    private int count;

    public SpatialPeakMetadata(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        this.x = new double[capacity];
        this.y = new double[capacity];
        this.z = new double[capacity];
    }

    public int getCount() {
        return count;
    }

    public double[] getX() {
        return x;
    }

    public double[] getY() {
        return y;
    }

    public double[] getZ() {
        return z;
    }

    public void reset() {
        count = 0;
    }

    public void add(double xValue, double yValue, double zValue) {
        if (count >= x.length) return;
        x[count] = xValue;
        y[count] = yValue;
        z[count] = zValue;
        count++;
    }
}
