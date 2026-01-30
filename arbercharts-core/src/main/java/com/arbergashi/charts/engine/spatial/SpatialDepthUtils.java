package com.arbergashi.charts.engine.spatial;

/**
 * Depth utility helpers for spatial buffers.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class SpatialDepthUtils {
    private SpatialDepthUtils() {
    }

    /**
     * Calculates {minZ, maxZ, avgZ} for the first {@code count} points.
     *
     * @param buffer spatial buffer
     * @param count  number of points to inspect
     * @param out    output array of length at least 3
     * @return out array
     */
    public static double[] getCalculatedZBounds(SpatialBuffer buffer, int count, double[] out) {
        if (out == null || out.length < 3) {
            throw new IllegalArgumentException("out must have length >= 3");
        }
        if (buffer == null || count <= 0) {
            out[0] = Double.NaN;
            out[1] = Double.NaN;
            out[2] = Double.NaN;
            return out;
        }
        double[] in = buffer.getInputCoords();
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double sum = 0.0;
        int limit = Math.min(count, buffer.getPointCapacity());
        for (int i = 0, j = 2; i < limit; i++, j += 3) {
            double z = in[j];
            if (z < min) min = z;
            if (z > max) max = z;
            sum += z;
        }
        out[0] = min;
        out[1] = max;
        out[2] = (limit > 0) ? (sum / limit) : Double.NaN;
        return out;
    }

    /**
     * Calculates the average Z for the first {@code count} points.
     */
    public static double getCalculatedZCenter(SpatialBuffer buffer, int count) {
        double[] tmp = new double[3];
        getCalculatedZBounds(buffer, count, tmp);
        return tmp[2];
    }
}
