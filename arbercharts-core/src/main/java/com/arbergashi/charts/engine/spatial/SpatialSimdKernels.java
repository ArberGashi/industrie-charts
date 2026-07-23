package com.arbergashi.charts.engine.spatial;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * SIMD kernels for spatial math backed by the incubating Vector API.
 *
 * <p>This is the only class in the spatial engine that references
 * {@code jdk.incubator.vector} types. It must never be loaded directly;
 * all access goes through {@link VectorIntrinsics}, which probes module
 * availability once and falls back to scalar code paths when the
 * {@code jdk.incubator.vector} module is not resolved at runtime.</p>
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 2.0.1
 * @author Arber Gashi
 */
final class SpatialSimdKernels {
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;

    private SpatialSimdKernels() {
    }

    static VectorSpecies<Double> preferredSpecies() {
        return SPECIES;
    }

    static int preferredLaneCount() {
        return SPECIES.length();
    }

    /**
     * Applies a 4x4 matrix to a batch of xyz triples (full range, masked tail).
     */
    static void transformBatch(double[] a, double[] inputCoords, double[] outputCoords,
                               int pointCount, int laneCount, SpatialScratchBuffer scratch, double eps) {
        VectorSpecies<Double> species = SPECIES;
        var v00 = DoubleVector.broadcast(species, a[0]);
        var v01 = DoubleVector.broadcast(species, a[1]);
        var v02 = DoubleVector.broadcast(species, a[2]);
        var v03 = DoubleVector.broadcast(species, a[3]);
        var v10 = DoubleVector.broadcast(species, a[4]);
        var v11 = DoubleVector.broadcast(species, a[5]);
        var v12 = DoubleVector.broadcast(species, a[6]);
        var v13 = DoubleVector.broadcast(species, a[7]);
        var v20 = DoubleVector.broadcast(species, a[8]);
        var v21 = DoubleVector.broadcast(species, a[9]);
        var v22 = DoubleVector.broadcast(species, a[10]);
        var v23 = DoubleVector.broadcast(species, a[11]);
        var v30 = DoubleVector.broadcast(species, a[12]);
        var v31 = DoubleVector.broadcast(species, a[13]);
        var v32 = DoubleVector.broadcast(species, a[14]);
        var v33 = DoubleVector.broadcast(species, a[15]);

        double[] bufX = scratch.getScratchX();
        double[] bufY = scratch.getScratchY();
        double[] bufZ = scratch.getScratchZ();
        double[] outX = scratch.getScratchOutX();
        double[] outY = scratch.getScratchOutY();
        double[] outZ = scratch.getScratchOutZ();
        double[] outW = scratch.getScratchW();

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
            var vx = DoubleVector.fromArray(species, bufX, 0);
            var vy = DoubleVector.fromArray(species, bufY, 0);
            var vz = DoubleVector.fromArray(species, bufZ, 0);

            var tx = vx.fma(v00, vy.fma(v01, vz.fma(v02, v03)));
            var ty = vx.fma(v10, vy.fma(v11, vz.fma(v12, v13)));
            var tz = vx.fma(v20, vy.fma(v21, vz.fma(v22, v23)));
            var tw = vx.fma(v30, vy.fma(v31, vz.fma(v32, v33)));

            tx.intoArray(outX, 0);
            ty.intoArray(outY, 0);
            tz.intoArray(outZ, 0);
            tw.intoArray(outW, 0);

            for (int k = 0; k < laneCount; k++) {
                int idx = j + k * 3;
                double w = outW[k];
                double ox = outX[k];
                double oy = outY[k];
                double oz = outZ[k];
                if (Math.abs(w) > eps && Math.abs(w - 1.0) > eps) {
                    double inv = 1.0 / w;
                    ox *= inv;
                    oy *= inv;
                    oz *= inv;
                }
                outputCoords[idx] = ox;
                outputCoords[idx + 1] = oy;
                outputCoords[idx + 2] = oz;
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
            var vx = DoubleVector.fromArray(species, bufX, 0, mask);
            var vy = DoubleVector.fromArray(species, bufY, 0, mask);
            var vz = DoubleVector.fromArray(species, bufZ, 0, mask);

            var tx = vx.fma(v00, vy.fma(v01, vz.fma(v02, v03)));
            var ty = vx.fma(v10, vy.fma(v11, vz.fma(v12, v13)));
            var tz = vx.fma(v20, vy.fma(v21, vz.fma(v22, v23)));
            var tw = vx.fma(v30, vy.fma(v31, vz.fma(v32, v33)));

            tx.intoArray(outX, 0, mask);
            ty.intoArray(outY, 0, mask);
            tz.intoArray(outZ, 0, mask);
            tw.intoArray(outW, 0, mask);

            for (int k = 0; k < remaining; k++) {
                int idx = j + k * 3;
                double w = outW[k];
                double ox = outX[k];
                double oy = outY[k];
                double oz = outZ[k];
                if (Math.abs(w) > eps && Math.abs(w - 1.0) > eps) {
                    double inv = 1.0 / w;
                    ox *= inv;
                    oy *= inv;
                    oz *= inv;
                }
                outputCoords[idx] = ox;
                outputCoords[idx + 1] = oy;
                outputCoords[idx + 2] = oz;
            }
        }
    }

    /**
     * Perspective-projects a batch of xyz triples (full range, masked tail).
     */
    static void projectPerspectiveBatch(double[] inputCoords, double[] outputCoords, int pointCount,
                                        double scale, double centerX, double centerY, double zBias,
                                        SpatialScratchBuffer scratch) {
        VectorSpecies<Double> species = SPECIES;
        int laneCount = scratch.getLaneCount();
        var vScale = DoubleVector.broadcast(species, scale);
        var vCenterX = DoubleVector.broadcast(species, centerX);
        var vCenterY = DoubleVector.broadcast(species, centerY);
        var vZBias = DoubleVector.broadcast(species, zBias);
        var vEps = DoubleVector.broadcast(species, 1e-12);

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
            var vx = DoubleVector.fromArray(species, bufX, 0);
            var vy = DoubleVector.fromArray(species, bufY, 0);
            var vz = DoubleVector.fromArray(species, bufZ, 0).add(vZBias);

            var maskZero = vz.abs().compare(VectorOperators.LT, vEps);
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
            var vx = DoubleVector.fromArray(species, bufX, 0, mask);
            var vy = DoubleVector.fromArray(species, bufY, 0, mask);
            var vz = DoubleVector.fromArray(species, bufZ, 0, mask).add(vZBias);

            var maskZero = vz.abs().compare(VectorOperators.LT, vEps).and(mask);
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

    /**
     * Orthographically projects a batch of xyz triples (full range, masked tail).
     */
    static void projectOrthographicBatch(double[] inputCoords, double[] outputCoords, int pointCount,
                                         double scale, double centerX, double centerY,
                                         SpatialScratchBuffer scratch) {
        VectorSpecies<Double> species = SPECIES;
        int laneCount = scratch.getLaneCount();
        var vScale = DoubleVector.broadcast(species, scale);
        var vCenterX = DoubleVector.broadcast(species, centerX);
        var vCenterY = DoubleVector.broadcast(species, centerY);

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
            var vx = DoubleVector.fromArray(species, bufX, 0);
            var vy = DoubleVector.fromArray(species, bufY, 0);
            var vz = DoubleVector.fromArray(species, bufZ, 0);

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
            var vx = DoubleVector.fromArray(species, bufX, 0, mask);
            var vy = DoubleVector.fromArray(species, bufY, 0, mask);
            var vz = DoubleVector.fromArray(species, bufZ, 0, mask);

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

    /**
     * Returns the nearest point index within the given squared radius, or -1 if none match.
     */
    static int nearestIndex(double[] coords, int count, double x, double y, double radiusSq,
                            SpatialScratchBuffer scratch) {
        VectorSpecies<Double> species = SPECIES;
        int laneCount = scratch.getLaneCount();
        if (laneCount <= 0) laneCount = 1;

        double bestDist = Double.MAX_VALUE;
        int bestIndex = -1;

        double[] bufX = scratch.getScratchX();
        double[] bufY = scratch.getScratchY();

        int i = 0;
        int j = 0;
        int limit = count - (count % laneCount);

        var vTargetX = DoubleVector.broadcast(species, x);
        var vTargetY = DoubleVector.broadcast(species, y);
        var vRadius = DoubleVector.broadcast(species, radiusSq);

        for (; i < limit; i += laneCount, j += laneCount * 3) {
            for (int k = 0; k < laneCount; k++) {
                int idx = j + k * 3;
                bufX[k] = coords[idx];
                bufY[k] = coords[idx + 1];
            }
            var vx = DoubleVector.fromArray(species, bufX, 0);
            var vy = DoubleVector.fromArray(species, bufY, 0);
            var dx = vx.sub(vTargetX);
            var dy = vy.sub(vTargetY);
            var dist = dx.mul(dx).add(dy.mul(dy));
            VectorMask<Double> mask = dist.compare(VectorOperators.LE, vRadius);

            for (int k = 0; k < laneCount; k++) {
                if (!mask.laneIsSet(k)) continue;
                double d = dist.lane(k);
                if (d < bestDist) {
                    bestDist = d;
                    bestIndex = i + k;
                }
            }
        }

        for (; i < count; i++, j += 3) {
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
     * Returns the nearest point index inside a 3D box centered at (x,y,z), or -1 if none match.
     */
    static int nearestIndexInBox(double[] coords, int count,
                                 double x, double y, double z,
                                 double halfX, double halfY, double halfZ,
                                 SpatialScratchBuffer scratch) {
        VectorSpecies<Double> species = SPECIES;
        int laneCount = scratch.getLaneCount();
        if (laneCount <= 0) laneCount = 1;

        double bestDist = Double.MAX_VALUE;
        int bestIndex = -1;

        double[] bufX = scratch.getScratchX();
        double[] bufY = scratch.getScratchY();
        double[] bufZ = scratch.getScratchZ();

        int i = 0;
        int j = 0;
        int limit = count - (count % laneCount);

        var vTargetX = DoubleVector.broadcast(species, x);
        var vTargetY = DoubleVector.broadcast(species, y);
        var vTargetZ = DoubleVector.broadcast(species, z);
        var vHalfX = DoubleVector.broadcast(species, halfX);
        var vHalfY = DoubleVector.broadcast(species, halfY);
        var vHalfZ = DoubleVector.broadcast(species, halfZ);

        for (; i < limit; i += laneCount, j += laneCount * 3) {
            for (int k = 0; k < laneCount; k++) {
                int idx = j + k * 3;
                bufX[k] = coords[idx];
                bufY[k] = coords[idx + 1];
                bufZ[k] = coords[idx + 2];
            }
            var vx = DoubleVector.fromArray(species, bufX, 0);
            var vy = DoubleVector.fromArray(species, bufY, 0);
            var vz = DoubleVector.fromArray(species, bufZ, 0);
            var dx = vx.sub(vTargetX).lanewise(VectorOperators.ABS);
            var dy = vy.sub(vTargetY).lanewise(VectorOperators.ABS);
            var dz = vz.sub(vTargetZ).lanewise(VectorOperators.ABS);
            VectorMask<Double> mask = dx.compare(VectorOperators.LE, vHalfX)
                    .and(dy.compare(VectorOperators.LE, vHalfY))
                    .and(dz.compare(VectorOperators.LE, vHalfZ));

            for (int k = 0; k < laneCount; k++) {
                if (!mask.laneIsSet(k)) continue;
                double tx = bufX[k] - x;
                double ty = bufY[k] - y;
                double tz = bufZ[k] - z;
                double d = tx * tx + ty * ty + tz * tz;
                if (d < bestDist) {
                    bestDist = d;
                    bestIndex = i + k;
                }
            }
        }

        for (; i < count; i++, j += 3) {
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

    /**
     * Computes per-lane bounds containment for a block of points staged in scratch arrays.
     *
     * <p>Writes {@code inBounds[k]} for {@code k < remaining}; lanes beyond
     * {@code remaining} are left untouched.</p>
     */
    static void boundsMask(double[] sx, double[] sy, int remaining,
                           double minX, double maxX, double minY, double maxY,
                           boolean[] inBounds) {
        VectorSpecies<Double> species = SPECIES;
        VectorMask<Double> inRange = species.indexInRange(0, remaining);
        DoubleVector vx = DoubleVector.fromArray(species, sx, 0);
        DoubleVector vy = DoubleVector.fromArray(species, sy, 0);

        VectorMask<Double> mask = vx.compare(VectorOperators.GE, minX)
                .and(vx.compare(VectorOperators.LE, maxX))
                .and(vy.compare(VectorOperators.GE, minY))
                .and(vy.compare(VectorOperators.LE, maxY))
                .and(inRange);

        for (int k = 0; k < remaining; k++) {
            inBounds[k] = mask.laneIsSet(k);
        }
    }
}
