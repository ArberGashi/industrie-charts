package com.arbergashi.charts.engine.spatial;

/**
 * Perspective projector: scales by depth (z).
 *
 * @since 1.7.0
 * @author Arber Gashi
 */
public final class PerspectiveProjector implements SpatialProjector {
    private double scale = 1.0;
    private double centerX;
    private double centerY;
    private double zBias = 1.0;
    private final SpatialScratchBuffer scratch = new SpatialScratchBuffer(VectorIntrinsics.getLaneCount());

    public double getScale() {
        return scale;
    }

    public PerspectiveProjector setScale(double scale) {
        this.scale = scale;
        return this;
    }

    public double getCenterX() {
        return centerX;
    }

    public PerspectiveProjector setCenterX(double centerX) {
        this.centerX = centerX;
        return this;
    }

    public double getCenterY() {
        return centerY;
    }

    public PerspectiveProjector setCenterY(double centerY) {
        this.centerY = centerY;
        return this;
    }

    public double getZBias() {
        return zBias;
    }

    public PerspectiveProjector setZBias(double zBias) {
        this.zBias = zBias;
        return this;
    }

    @Override
    public Vector3D getCalculatedProjection(Vector3D point) {
        double z = point.getZ() + zBias;
        double factor = (Math.abs(z) < 1e-12) ? scale : (scale / z);
        double x = centerX + point.getX() * factor;
        double y = centerY - point.getY() * factor;
        return new Vector3D(x, y, point.getZ());
    }

    @Override
    public void getCalculatedProjection(double x, double y, double z, double[] out) {
        double zz = z + zBias;
        double factor = (Math.abs(zz) < 1e-12) ? scale : (scale / zz);
        out[0] = centerX + x * factor;
        out[1] = centerY - y * factor;
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
            SpatialSimdKernels.projectPerspectiveBatch(inputCoords, outputCoords, pointCount,
                    scale, centerX, centerY, zBias, scratch);
            return;
        }
        for (int i = 0, j = 0; i < pointCount; i++, j += 3) {
            double z = inputCoords[j + 2];
            double zz = z + zBias;
            double factor = (Math.abs(zz) < 1e-12) ? scale : (scale / zz);
            outputCoords[j] = centerX + inputCoords[j] * factor;
            outputCoords[j + 1] = centerY - inputCoords[j + 1] * factor;
            outputCoords[j + 2] = z;
        }
    }
}
