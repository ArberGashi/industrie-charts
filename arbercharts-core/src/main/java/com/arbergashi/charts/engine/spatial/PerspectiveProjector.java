package com.arbergashi.charts.engine.spatial;

/**
 * Perspective projector: scales by depth (z).
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
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
        int laneCount = scratch.getLaneCount();
        if (laneCount <= 0) laneCount = 1;
        var species = VectorIntrinsics.getPreferredSpecies();
        var vScale = jdk.incubator.vector.DoubleVector.broadcast(species, scale);
        var vCenterX = jdk.incubator.vector.DoubleVector.broadcast(species, centerX);
        var vCenterY = jdk.incubator.vector.DoubleVector.broadcast(species, centerY);
        var vZBias = jdk.incubator.vector.DoubleVector.broadcast(species, zBias);
        var vEps = jdk.incubator.vector.DoubleVector.broadcast(species, 1e-12);

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
            var vz = jdk.incubator.vector.DoubleVector.fromArray(species, bufZ, 0).add(vZBias);

            var maskZero = vz.abs().compare(jdk.incubator.vector.VectorOperators.LT, vEps);
            var vFactor = vScale.div(vz);
            vFactor = vFactor.blend(vScale, maskZero);

            var rx = vCenterX.add(vx.mul(vFactor));
            var ry = vCenterY.sub(vy.mul(vFactor));

            rx.intoArray(outX, 0);
            ry.intoArray(outY, 0);
            vz.sub(vZBias).intoArray(outZ, 0);

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
            var vz = jdk.incubator.vector.DoubleVector.fromArray(species, bufZ, 0, mask).add(vZBias);

            var maskZero = vz.abs().compare(jdk.incubator.vector.VectorOperators.LT, vEps).and(mask);
            var vFactor = vScale.div(vz);
            vFactor = vFactor.blend(vScale, maskZero);

            var rx = vCenterX.add(vx.mul(vFactor));
            var ry = vCenterY.sub(vy.mul(vFactor));

            rx.intoArray(outX, 0, mask);
            ry.intoArray(outY, 0, mask);
            vz.sub(vZBias).intoArray(outZ, 0, mask);

            for (int k = 0; k < remaining; k++) {
                int idx = j + k * 3;
                outputCoords[idx] = outX[k];
                outputCoords[idx + 1] = outY[k];
                outputCoords[idx + 2] = outZ[k];
            }
        }
    }
}
