package com.arbergashi.charts.engine.spatial;

/**
 * SIMD-aware hit detector for spatial buffers.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class VectorizedHitDetector {
    private static final VectorizedHitDetector SHARED = new VectorizedHitDetector();

    /**
     * Returns a shared hit detector instance for reuse across layers.
     *
     * @return shared detector
     * @since 1.7.0
     */
    public static VectorizedHitDetector getShared() {
        return SHARED;
    }
    /**
     * Legacy-friendly alias for hit detection.
     *
     * @param buffer spatial buffer
     * @param count number of points
     * @param x target x
     * @param y target y
     * @param radius hit radius
     * @return nearest point index or -1
     */
    public int findHitIndex(SpatialBuffer buffer, int count, double x, double y, double radius) {
        return getNearestIndex(buffer, count, x, y, radius, false);
    }

    /**
     * Returns the nearest point index within the given radius, or -1 if none match.
     *
     * @param buffer spatial buffer
     * @param count number of points
     * @param x target x
     * @param y target y
     * @param radius hit radius
     * @param useOutputCoords true to read from outputCoords instead of inputCoords
     * @return nearest point index or -1
     */
    public int getNearestIndex(SpatialBuffer buffer, int count, double x, double y, double radius, boolean useOutputCoords) {
        if (buffer == null || count <= 0 || radius <= 0.0) return -1;
        double[] coords = useOutputCoords ? buffer.getOutputCoords() : buffer.getInputCoords();
        if (coords == null) return -1;
        int required = count * 3;
        if (coords.length < required) return -1;

        double radiusSq = radius * radius;
        if (VectorIntrinsics.isAvailable()) {
            return SpatialSimdKernels.nearestIndex(coords, count, x, y, radiusSq, buffer.getScratch());
        }

        double bestDist = Double.MAX_VALUE;
        int bestIndex = -1;
        for (int i = 0, j = 0; i < count; i++, j += 3) {
            double dx = coords[j] - x;
            double dy = coords[j + 1] - y;
            double d = dx * dx + dy * dy;
            if (d <= radiusSq && d < bestDist) {
                bestDist = d;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    /**
     * Returns the nearest point index within the given radius, preferring peak anchors when available.
     *
     * <p>If a peak anchor is within the radius, the search is re-centered on that peak to find
     * the closest actual buffer point.</p>
     *
     * @param buffer spatial buffer
     * @param count number of points
     * @param x target x
     * @param y target y
     * @param radius hit radius
     * @param useOutputCoords true to read from outputCoords instead of inputCoords
     * @param peaks peak metadata (may be null)
     * @return nearest point index or -1
     * @since 1.7.0
     */
    public int getNearestIndexPreferPeaks(SpatialBuffer buffer, int count, double x, double y, double radius,
                                          boolean useOutputCoords, SpatialPeakMetadata peaks) {
        if (peaks == null || peaks.getCount() == 0) {
            return getNearestIndex(buffer, count, x, y, radius, useOutputCoords);
        }
        double radiusSq = radius * radius;
        int peakIndex = getNearestPeakIndex(peaks, x, y, radiusSq);
        if (peakIndex >= 0) {
            double px = peaks.getX()[peakIndex];
            double py = peaks.getY()[peakIndex];
            int hit = getNearestIndex(buffer, count, px, py, radius, useOutputCoords);
            if (hit >= 0) {
                return hit;
            }
        }
        return getNearestIndex(buffer, count, x, y, radius, useOutputCoords);
    }

    /**
     * Returns the nearest point index within the given radius, preferring peak anchors from a provider.
     *
     * @param buffer spatial buffer
     * @param count number of points
     * @param x target x
     * @param y target y
     * @param radius hit radius
     * @param useOutputCoords true to read from outputCoords instead of inputCoords
     * @param peakProvider peak provider (may be null)
     * @return nearest point index or -1
     * @since 1.7.0
     */
    public int getNearestIndexPreferPeaks(SpatialBuffer buffer, int count, double x, double y, double radius,
                                          boolean useOutputCoords, SpatialPeakProvider peakProvider) {
        SpatialPeakMetadata peaks = (peakProvider != null) ? peakProvider.getSpatialPeakMetadata() : null;
        return getNearestIndexPreferPeaks(buffer, count, x, y, radius, useOutputCoords, peaks);
    }

    private int getNearestPeakIndex(SpatialPeakMetadata peaks, double x, double y, double radiusSq) {
        double[] xs = peaks.getX();
        double[] ys = peaks.getY();
        int count = peaks.getCount();
        double best = Double.MAX_VALUE;
        int bestIndex = -1;
        for (int i = 0; i < count; i++) {
            double dx = xs[i] - x;
            double dy = ys[i] - y;
            double d = dx * dx + dy * dy;
            if (d <= radiusSq && d < best) {
                best = d;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    /**
     * Returns the nearest point index inside a 3D box centered at (x,y,z).
     *
     * @param buffer spatial buffer
     * @param count number of points
     * @param x target x
     * @param y target y
     * @param z target z
     * @param halfX half width of box
     * @param halfY half height of box
     * @param halfZ half depth of box
     * @param useOutputCoords true to read from outputCoords instead of inputCoords
     * @return nearest point index or -1
     */
    public int getNearestIndexInBox(SpatialBuffer buffer, int count,
                                    double x, double y, double z,
                                    double halfX, double halfY, double halfZ,
                                    boolean useOutputCoords) {
        if (buffer == null || count <= 0) return -1;
        if (halfX <= 0.0 || halfY <= 0.0 || halfZ <= 0.0) return -1;
        double[] coords = useOutputCoords ? buffer.getOutputCoords() : buffer.getInputCoords();
        if (coords == null) return -1;
        int required = count * 3;
        if (coords.length < required) return -1;

        if (VectorIntrinsics.isAvailable()) {
            return SpatialSimdKernels.nearestIndexInBox(coords, count, x, y, z,
                    halfX, halfY, halfZ, buffer.getScratch());
        }

        double bestDist = Double.MAX_VALUE;
        int bestIndex = -1;
        for (int i = 0, j = 0; i < count; i++, j += 3) {
            double dx = coords[j] - x;
            double dy = coords[j + 1] - y;
            double dz = coords[j + 2] - z;
            if (Math.abs(dx) > halfX || Math.abs(dy) > halfY || Math.abs(dz) > halfZ) {
                continue;
            }
            double d = dx * dx + dy * dy + dz * dz;
            if (d < bestDist) {
                bestDist = d;
                bestIndex = i;
            }
        }
        return bestIndex;
    }
}
