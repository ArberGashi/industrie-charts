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
        int laneCount = scratch.getLaneCount();
        if (laneCount <= 0) laneCount = 1;
        var species = VectorIntrinsics.getPreferredSpecies();
        var vScale = jdk.incubator.vector.DoubleVector.broadcast(species, scale);
        var vCenterX = jdk.incubator.vector.DoubleVector.broadcast(species, centerX);
        var vCenterY = jdk.incubator.vector.DoubleVector.broadcast(species, centerY);

        double[] bufX = scratch.getScratchX();
        double[] bufY = scratch.getScratchY();
        double[] bufZ = scratch.getScratchZ();
        double[] outX = scratch.getScratchOutX();
        double[] outY = scratch.getScratchOutY();
        double[] outZ = scratch.getScratchOutZ();

        int i = 0;
        int j = 0;
        int limit = pointCount - (pointCount % laneCount);
        for (; i < limit; i += laneCount, j += laneCount * 3) {
            for (int k = 0; k < laneCount; k++) {
                int idx = j + k * 3;
                bufX[k] = inputCoords[idx];
                bufY[k] = inputCoords[idx + 1];
                bufZ[k] = inputCoords[idx + 2];
            }
            var vx = jdk.incubator.vector.DoubleVector.fromArray(species, bufX, 0);
            var vy = jdk.incubator.vector.DoubleVector.fromArray(species, bufY, 0);
            var vz = jdk.incubator.vector.DoubleVector.fromArray(species, bufZ, 0);

            var rx = vCenterX.add(vx.mul(vScale));
            var ry = vCenterY.sub(vy.mul(vScale));

            rx.intoArray(outX, 0);
            ry.intoArray(outY, 0);
            vz.intoArray(outZ, 0);

            for (int k = 0; k < laneCount; k++) {
                int idx = j + k * 3;
                outputCoords[idx] = outX[k];
                outputCoords[idx + 1] = outY[k];
                outputCoords[idx + 2] = outZ[k];
            }
        }
        if (i < pointCount) {
            int remaining = pointCount - i;
            var mask = species.indexInRange(0, remaining);
            for (int k = 0; k < remaining; k++) {
                int idx = j + k * 3;
                bufX[k] = inputCoords[idx];
                bufY[k] = inputCoords[idx + 1];
                bufZ[k] = inputCoords[idx + 2];
            }
            var vx = jdk.incubator.vector.DoubleVector.fromArray(species, bufX, 0, mask);
            var vy = jdk.incubator.vector.DoubleVector.fromArray(species, bufY, 0, mask);
            var vz = jdk.incubator.vector.DoubleVector.fromArray(species, bufZ, 0, mask);

            var rx = vCenterX.add(vx.mul(vScale));
            var ry = vCenterY.sub(vy.mul(vScale));

            rx.intoArray(outX, 0, mask);
            ry.intoArray(outY, 0, mask);
            vz.intoArray(outZ, 0, mask);

            for (int k = 0; k < remaining; k++) {
                int idx = j + k * 3;
                outputCoords[idx] = outX[k];
                outputCoords[idx + 1] = outY[k];
                outputCoords[idx + 2] = outZ[k];
            }
        }
    }
}
