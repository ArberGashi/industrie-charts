package com.arbergashi.charts.engine.spatial;

/**
 * Projects 3D coordinates into a 2D space representation.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 1.7.0
 */
public sealed interface SpatialProjector permits OrthographicProjector, PerspectiveProjector {
    /**
     * Returns the projected coordinate for the given 3D point.
     *
     * @param point input vector
     * @return projected vector
     */
    Vector3D getCalculatedProjection(Vector3D point);

    /**
     * Projects a single 3D point without allocating a new Vector3D.
     *
     * @param x   input x
     * @param y   input y
     * @param z   input z
     * @param out output array of length at least 3 (x,y,z)
     */
    default void getCalculatedProjection(double x, double y, double z, double[] out) {
        Vector3D projected = getCalculatedProjection(new Vector3D(x, y, z));
        out[0] = projected.getX();
        out[1] = projected.getY();
        out[2] = projected.getZ();
    }

    /**
     * Projects a batch of 3D points packed as xyz triples.
     *
     * @param inputCoords  flat xyz array (x1,y1,z1,x2,y2,z2,...)
     * @param outputCoords flat xyz array for results
     * @param pointCount   number of points
     */
    void getCalculatedProjectionBatch(double[] inputCoords, double[] outputCoords, int pointCount);
}
