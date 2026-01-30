package com.arbergashi.charts.engine.spatial;

/**
 * 4x4 transformation matrix for spatial projection.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class Matrix4x4 {
    private static final double EPS = 1e-12;
    private static final int SIZE = 16;

    private final double[] elements = new double[SIZE];

    public Matrix4x4() {
    }

    public Matrix4x4 setIdentity() {
        elements[0] = 1;  elements[1] = 0;  elements[2] = 0;  elements[3] = 0;
        elements[4] = 0;  elements[5] = 1;  elements[6] = 0;  elements[7] = 0;
        elements[8] = 0;  elements[9] = 0;  elements[10] = 1; elements[11] = 0;
        elements[12] = 0; elements[13] = 0; elements[14] = 0; elements[15] = 1;
        return this;
    }

    public Matrix4x4 setTranslation(double tx, double ty, double tz) {
        elements[3] = tx;
        elements[7] = ty;
        elements[11] = tz;
        return this;
    }

    public Matrix4x4 setScale(double sx, double sy, double sz) {
        elements[0] = sx;
        elements[5] = sy;
        elements[10] = sz;
        return this;
    }

    public Matrix4x4 setRotationX(double radians) {
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        elements[5] = c;   elements[6] = -s;
        elements[9] = s;   elements[10] = c;
        return this;
    }

    public Matrix4x4 setRotationY(double radians) {
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        elements[0] = c;   elements[2] = s;
        elements[8] = -s;  elements[10] = c;
        return this;
    }

    public Matrix4x4 setRotationZ(double radians) {
        double c = Math.cos(radians);
        double s = Math.sin(radians);
        elements[0] = c;   elements[1] = -s;
        elements[4] = s;   elements[5] = c;
        return this;
    }

    public Matrix4x4 setValues(double[] values) {
        if (values == null || values.length != SIZE) {
            throw new IllegalArgumentException("Expected 16-element array for matrix values");
        }
        System.arraycopy(values, 0, elements, 0, SIZE);
        return this;
    }

    /**
     * Sets this matrix to the product of this * other.
     *
     * @param other right-hand matrix
     * @return this matrix
     */
    public Matrix4x4 setProduct(Matrix4x4 other) {
        double[] a = elements;
        double[] b = other.elements;

        double r00 = a[0] * b[0] + a[1] * b[4] + a[2] * b[8] + a[3] * b[12];
        double r01 = a[0] * b[1] + a[1] * b[5] + a[2] * b[9] + a[3] * b[13];
        double r02 = a[0] * b[2] + a[1] * b[6] + a[2] * b[10] + a[3] * b[14];
        double r03 = a[0] * b[3] + a[1] * b[7] + a[2] * b[11] + a[3] * b[15];

        double r10 = a[4] * b[0] + a[5] * b[4] + a[6] * b[8] + a[7] * b[12];
        double r11 = a[4] * b[1] + a[5] * b[5] + a[6] * b[9] + a[7] * b[13];
        double r12 = a[4] * b[2] + a[5] * b[6] + a[6] * b[10] + a[7] * b[14];
        double r13 = a[4] * b[3] + a[5] * b[7] + a[6] * b[11] + a[7] * b[15];

        double r20 = a[8] * b[0] + a[9] * b[4] + a[10] * b[8] + a[11] * b[12];
        double r21 = a[8] * b[1] + a[9] * b[5] + a[10] * b[9] + a[11] * b[13];
        double r22 = a[8] * b[2] + a[9] * b[6] + a[10] * b[10] + a[11] * b[14];
        double r23 = a[8] * b[3] + a[9] * b[7] + a[10] * b[11] + a[11] * b[15];

        double r30 = a[12] * b[0] + a[13] * b[4] + a[14] * b[8] + a[15] * b[12];
        double r31 = a[12] * b[1] + a[13] * b[5] + a[14] * b[9] + a[15] * b[13];
        double r32 = a[12] * b[2] + a[13] * b[6] + a[14] * b[10] + a[15] * b[14];
        double r33 = a[12] * b[3] + a[13] * b[7] + a[14] * b[11] + a[15] * b[15];

        a[0] = r00; a[1] = r01; a[2] = r02; a[3] = r03;
        a[4] = r10; a[5] = r11; a[6] = r12; a[7] = r13;
        a[8] = r20; a[9] = r21; a[10] = r22; a[11] = r23;
        a[12] = r30; a[13] = r31; a[14] = r32; a[15] = r33;
        return this;
    }

    /**
     * Applies this matrix to a 3D point (homogeneous w=1).
     *
     * @param point input point
     * @return transformed point
     */
    public Vector3D getCalculatedTransform(Vector3D point) {
        double x = point.getX();
        double y = point.getY();
        double z = point.getZ();
        double[] a = elements;
        double tx = a[0] * x + a[1] * y + a[2] * z + a[3];
        double ty = a[4] * x + a[5] * y + a[6] * z + a[7];
        double tz = a[8] * x + a[9] * y + a[10] * z + a[11];
        double tw = a[12] * x + a[13] * y + a[14] * z + a[15];
        if (Math.abs(tw) > EPS && Math.abs(tw - 1.0) > EPS) {
            double inv = 1.0 / tw;
            tx *= inv;
            ty *= inv;
            tz *= inv;
        }
        return new Vector3D(tx, ty, tz);
    }

    /**
     * Applies this matrix to a batch of 3D points packed as xyz triples.
     *
     * @param inputCoords  flat xyz array (x1,y1,z1,x2,y2,z2,...)
     * @param outputCoords flat xyz array for results
     * @param pointCount   number of points
     * @return this matrix
     */
    public Matrix4x4 getCalculatedTransformBatch(double[] inputCoords, double[] outputCoords, int pointCount) {
        return getCalculatedTransformBatch(inputCoords, outputCoords, pointCount, null);
    }

    /**
     * Applies this matrix to a batch of 3D points using an optional scratch buffer for SIMD.
     *
     * @param inputCoords  flat xyz array (x1,y1,z1,x2,y2,z2,...)
     * @param outputCoords flat xyz array for results
     * @param pointCount   number of points
     * @param scratch      optional scratch buffer for SIMD path
     * @return this matrix
     */
    public Matrix4x4 getCalculatedTransformBatch(double[] inputCoords, double[] outputCoords, int pointCount, SpatialScratchBuffer scratch) {
        if (inputCoords == null || outputCoords == null) {
            throw new IllegalArgumentException("input/output buffers required");
        }
        int inLen = inputCoords.length;
        int outLen = outputCoords.length;
        int required = pointCount * 3;
        if (inLen < required || outLen < required) {
            throw new IllegalArgumentException("buffers too small for pointCount");
        }
        double[] a = elements;
        int laneCount = VectorIntrinsics.getLaneCount();
        if (laneCount <= 0) laneCount = 1;
        int i = 0;
        int j = 0;
        int limit = pointCount - (pointCount % laneCount);
        if (scratch != null && scratch.isAlignedForLaneCount(laneCount)) {
            var species = VectorIntrinsics.getPreferredSpecies();
            var v00 = jdk.incubator.vector.DoubleVector.broadcast(species, a[0]);
            var v01 = jdk.incubator.vector.DoubleVector.broadcast(species, a[1]);
            var v02 = jdk.incubator.vector.DoubleVector.broadcast(species, a[2]);
            var v03 = jdk.incubator.vector.DoubleVector.broadcast(species, a[3]);
            var v10 = jdk.incubator.vector.DoubleVector.broadcast(species, a[4]);
            var v11 = jdk.incubator.vector.DoubleVector.broadcast(species, a[5]);
            var v12 = jdk.incubator.vector.DoubleVector.broadcast(species, a[6]);
            var v13 = jdk.incubator.vector.DoubleVector.broadcast(species, a[7]);
            var v20 = jdk.incubator.vector.DoubleVector.broadcast(species, a[8]);
            var v21 = jdk.incubator.vector.DoubleVector.broadcast(species, a[9]);
            var v22 = jdk.incubator.vector.DoubleVector.broadcast(species, a[10]);
            var v23 = jdk.incubator.vector.DoubleVector.broadcast(species, a[11]);
            var v30 = jdk.incubator.vector.DoubleVector.broadcast(species, a[12]);
            var v31 = jdk.incubator.vector.DoubleVector.broadcast(species, a[13]);
            var v32 = jdk.incubator.vector.DoubleVector.broadcast(species, a[14]);
            var v33 = jdk.incubator.vector.DoubleVector.broadcast(species, a[15]);

            double[] bufX = scratch.getScratchX();
            double[] bufY = scratch.getScratchY();
            double[] bufZ = scratch.getScratchZ();
            double[] outX = scratch.getScratchOutX();
            double[] outY = scratch.getScratchOutY();
            double[] outZ = scratch.getScratchOutZ();
            double[] outW = scratch.getScratchW();

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
                    if (Math.abs(w) > EPS && Math.abs(w - 1.0) > EPS) {
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
                var vx = jdk.incubator.vector.DoubleVector.fromArray(species, bufX, 0, mask);
                var vy = jdk.incubator.vector.DoubleVector.fromArray(species, bufY, 0, mask);
                var vz = jdk.incubator.vector.DoubleVector.fromArray(species, bufZ, 0, mask);

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
                    if (Math.abs(w) > EPS && Math.abs(w - 1.0) > EPS) {
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
        } else {
            for (; i < limit; i += laneCount, j += laneCount * 3) {
                for (int k = 0; k < laneCount; k++) {
                    int idx = j + k * 3;
                    double x = inputCoords[idx];
                    double y = inputCoords[idx + 1];
                    double z = inputCoords[idx + 2];
                    double tx = a[0] * x + a[1] * y + a[2] * z + a[3];
                    double ty = a[4] * x + a[5] * y + a[6] * z + a[7];
                    double tz = a[8] * x + a[9] * y + a[10] * z + a[11];
                    double tw = a[12] * x + a[13] * y + a[14] * z + a[15];
                    if (Math.abs(tw) > EPS && Math.abs(tw - 1.0) > EPS) {
                        double inv = 1.0 / tw;
                        tx *= inv;
                        ty *= inv;
                        tz *= inv;
                    }
                    outputCoords[idx] = tx;
                    outputCoords[idx + 1] = ty;
                    outputCoords[idx + 2] = tz;
                }
            }
        }
        for (; i < pointCount; i++, j += 3) {
            double x = inputCoords[j];
            double y = inputCoords[j + 1];
            double z = inputCoords[j + 2];
            double tx = a[0] * x + a[1] * y + a[2] * z + a[3];
            double ty = a[4] * x + a[5] * y + a[6] * z + a[7];
            double tz = a[8] * x + a[9] * y + a[10] * z + a[11];
            double tw = a[12] * x + a[13] * y + a[14] * z + a[15];
            if (Math.abs(tw) > EPS && Math.abs(tw - 1.0) > EPS) {
                double inv = 1.0 / tw;
                tx *= inv;
                ty *= inv;
                tz *= inv;
            }
            outputCoords[j] = tx;
            outputCoords[j + 1] = ty;
            outputCoords[j + 2] = tz;
        }
        return this;
    }

    public double getM00() { return elements[0]; }
    public Matrix4x4 setM00(double v) { elements[0] = v; return this; }
    public double getM01() { return elements[1]; }
    public Matrix4x4 setM01(double v) { elements[1] = v; return this; }
    public double getM02() { return elements[2]; }
    public Matrix4x4 setM02(double v) { elements[2] = v; return this; }
    public double getM03() { return elements[3]; }
    public Matrix4x4 setM03(double v) { elements[3] = v; return this; }

    public double getM10() { return elements[4]; }
    public Matrix4x4 setM10(double v) { elements[4] = v; return this; }
    public double getM11() { return elements[5]; }
    public Matrix4x4 setM11(double v) { elements[5] = v; return this; }
    public double getM12() { return elements[6]; }
    public Matrix4x4 setM12(double v) { elements[6] = v; return this; }
    public double getM13() { return elements[7]; }
    public Matrix4x4 setM13(double v) { elements[7] = v; return this; }

    public double getM20() { return elements[8]; }
    public Matrix4x4 setM20(double v) { elements[8] = v; return this; }
    public double getM21() { return elements[9]; }
    public Matrix4x4 setM21(double v) { elements[9] = v; return this; }
    public double getM22() { return elements[10]; }
    public Matrix4x4 setM22(double v) { elements[10] = v; return this; }
    public double getM23() { return elements[11]; }
    public Matrix4x4 setM23(double v) { elements[11] = v; return this; }

    public double getM30() { return elements[12]; }
    public Matrix4x4 setM30(double v) { elements[12] = v; return this; }
    public double getM31() { return elements[13]; }
    public Matrix4x4 setM31(double v) { elements[13] = v; return this; }
    public double getM32() { return elements[14]; }
    public Matrix4x4 setM32(double v) { elements[14] = v; return this; }
    public double getM33() { return elements[15]; }
    public Matrix4x4 setM33(double v) { elements[15] = v; return this; }
}
