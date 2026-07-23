package com.arbergashi.charts.engine.spatial;

/**
 * Orthographic projector: depth does not affect scale.
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class OrthographicProjector implements SpatialProjector {
    private double scale = 1.0;
    private double centerX;
    private double centerY;
    private final SpatialScratchBuffer scratch = new SpatialScratchBuffer(VectorIntrinsics.getLaneCount());

    public double getScale() {
        return scale;
    }

    public OrthographicProjector setScale(double scale) {
        this.scale = scale;
        return this;
    }

    public double getCenterX() {
        return centerX;
    }

    public OrthographicProjector setCenterX(double centerX) {
        this.centerX = centerX;
        return this;
    }

    public double getCenterY() {
        return centerY;
    }

    public OrthographicProjector setCenterY(double centerY) {
        this.centerY = centerY;
        return this;
    }

    @Override
    public Vector3D getCalculatedProjection(Vector3D point) {
        double x = centerX + point.getX() * scale;
        double y = centerY - point.getY() * scale;
        return new Vector3D(x, y, point.getZ());
    }

    @Override
    public void getCalculatedProjection(double x, double y, double z, double[] out) {
        out[0] = centerX + x * scale;
        out[1] = centerY - y * scale;
        out[2] = z;
    }

    @Override
    public void getCalculatedProjectionBatch(double[] inputCoords, double[] outputCoords, int pointCount) {
        if (inputCoords == null || outputCoords == null) {
            throw new IllegalArgumentException("input/output buffers required");
        }
        int required = pointCount * 3;
        if (inputCoords.length < required || outputCoords.length < required) {
            throw new IllegalArgumentException("buffers too small for pointCount");
        }
        if (VectorIntrinsics.isAvailable()) {
            SpatialSimdKernels.projectOrthographicBatch(inputCoords, outputCoords, pointCount,
                    scale, centerX, centerY, scratch);
            return;
        }
        for (int i = 0, j = 0; i < pointCount; i++, j += 3) {
            outputCoords[j] = centerX + inputCoords[j] * scale;
            outputCoords[j + 1] = centerY - inputCoords[j + 1] * scale;
            outputCoords[j + 2] = inputCoords[j + 2];
        }
    }
}
