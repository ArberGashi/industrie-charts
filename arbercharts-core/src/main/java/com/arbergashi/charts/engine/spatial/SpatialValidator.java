package com.arbergashi.charts.engine.spatial;


/**
 * Internal validation utility for spatial projection consistency.
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class SpatialValidator {
    public static final double SIMD_PRECISION_THRESHOLD = 1e-9;

    private SpatialValidator() {
    }

    public static void validateUnitCube(SpatialProjector projector) {
        Vector3D[] cube = getUnitCube();
        for (Vector3D v : cube) {
            Vector3D p = projector.getCalculatedProjection(v);
            if (!Double.isFinite(p.getX()) || !Double.isFinite(p.getY())) {
                throw new IllegalStateException("Invalid projection result for " + v.getX() + "," + v.getY() + "," + v.getZ());
            }
        }
        validateBatch(projector, cube);
    }

    public static void validateTetrahedron(SpatialProjector projector) {
        Vector3D[] points = getTetrahedron();
        for (Vector3D v : points) {
            Vector3D p = projector.getCalculatedProjection(v);
            if (!Double.isFinite(p.getX()) || !Double.isFinite(p.getY())) {
                throw new IllegalStateException("Invalid projection result for " + v.getX() + "," + v.getY() + "," + v.getZ());
            }
        }
        validateBatch(projector, points);
    }

    public static long[] getCalculatedPerformanceAudit(int iterations) {
        int pointCount = 10_000;
        SpatialBuffer buffer = new SpatialBuffer(pointCount);
        double[] in = buffer.getInputCoords();
        double[] out = buffer.getOutputCoords();
        for (int i = 0, j = 0; i < pointCount; i++, j += 3) {
            in[j] = i * 0.001;
            in[j + 1] = i * 0.002;
            in[j + 2] = i * 0.003;
        }
        Matrix4x4 m = new Matrix4x4().setIdentity();

        long startScalar = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            m.getCalculatedTransformBatch(in, out, pointCount, null);
        }
        long scalarNanos = System.nanoTime() - startScalar;

        SpatialScratchBuffer scratch = buffer.getScratch();
        long startSimd = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            m.getCalculatedTransformBatch(in, out, pointCount, scratch);
        }
        long simdNanos = System.nanoTime() - startSimd;

        return new long[]{scalarNanos, simdNanos};
    }

    /**
     * Validates SIMD vs scalar precision and returns max absolute deltas.
     *
     * @param projector projector under test
     * @param pointCount number of points to test
     * @return {maxDeltaX, maxDeltaY}
     */
    public static double[] getCalculatedPrecisionAudit(SpatialProjector projector, int pointCount) {
        if (pointCount <= 0) throw new IllegalArgumentException("pointCount must be > 0");
        SpatialBuffer buffer = new SpatialBuffer(pointCount);
        double[] in = buffer.getInputCoords();
        double[] out = buffer.getOutputCoords();

        // Extreme/edge values mix
        double[] seeds = new double[]{
                -1e9, -1e6, -1e3, -1.0, -1e-6,
                0.0, 1e-12, 1e-6, 1.0, 1e3, 1e6, 1e9
        };
        int idx = 0;
        for (int i = 0; i < pointCount; i++) {
            double x = seeds[idx % seeds.length];
            double y = seeds[(idx + 3) % seeds.length];
            double z = seeds[(idx + 5) % seeds.length];
            in[i * 3] = x;
            in[i * 3 + 1] = y;
            in[i * 3 + 2] = z;
            idx++;
        }

        projector.getCalculatedProjectionBatch(in, out, pointCount);

        double maxDx = 0.0;
        double maxDy = 0.0;
        for (int i = 0, j = 0; i < pointCount; i++, j += 3) {
            Vector3D scalar = projector.getCalculatedProjection(new Vector3D(in[j], in[j + 1], in[j + 2]));
            double dx = Math.abs(out[j] - scalar.getX());
            double dy = Math.abs(out[j + 1] - scalar.getY());
            if (dx > maxDx) maxDx = dx;
            if (dy > maxDy) maxDy = dy;
        }
        return new double[]{maxDx, maxDy};
    }

    /**
     * Validates SIMD precision and throws if the threshold is exceeded.
     */
    public static void validatePrecisionGate(SpatialProjector projector, int pointCount) {
        double[] delta = getCalculatedPrecisionAudit(projector, pointCount);
        double max = Math.max(delta[0], delta[1]);
        if (max > SIMD_PRECISION_THRESHOLD) {
            throw new IllegalStateException("SIMD precision gate failed: maxDelta=" + max
                    + " threshold=" + SIMD_PRECISION_THRESHOLD);
        }
    }

    /**
     * Returns a structured validation report for CI logs.
     */
    public static String getFormattedValidationReport(String scenario, SpatialProjector projector, int pointCount) {
        double[] delta = getCalculatedPrecisionAudit(projector, pointCount);
        double max = Math.max(delta[0], delta[1]);
        String status = max <= SIMD_PRECISION_THRESHOLD ? "PASS" : "FAIL";
        return String.join("\n",
                "SpatialValidationReport",
                "scenario=" + scenario,
                "points=" + pointCount,
                "species=" + VectorIntrinsics.getPreferredSpecies().length(),
                "maxDeltaX=" + delta[0],
                "maxDeltaY=" + delta[1],
                "threshold=" + SIMD_PRECISION_THRESHOLD,
                "status=" + status
        );
    }

    private static void validateBatch(SpatialProjector projector, Vector3D[] points) {
        int count = points.length;
        SpatialBuffer buffer = new SpatialBuffer(count);
        double[] in = buffer.getInputCoords();
        double[] out = buffer.getOutputCoords();
        for (int i = 0, j = 0; i < count; i++, j += 3) {
            Vector3D v = points[i];
            in[j] = v.getX();
            in[j + 1] = v.getY();
            in[j + 2] = v.getZ();
        }
        SpatialScratchBuffer scratch = buffer.getScratch();
        projector.getCalculatedProjectionBatch(in, out, count);
        for (int i = 0, j = 0; i < count; i++, j += 3) {
            double x = out[j];
            double y = out[j + 1];
            if (!Double.isFinite(x) || !Double.isFinite(y)) {
                Vector3D v = points[i];
                throw new IllegalStateException("Invalid batch projection for " + v.getX() + "," + v.getY() + "," + v.getZ());
            }
            Vector3D single = projector.getCalculatedProjection(points[i]);
            if (Math.abs(single.getX() - x) > 1e-9 || Math.abs(single.getY() - y) > 1e-9) {
                Vector3D v = points[i];
                throw new IllegalStateException("Batch projection mismatch for " + v.getX() + "," + v.getY() + "," + v.getZ());
            }
        }
    }

    private static Vector3D[] getUnitCube() {
        return new Vector3D[]{
                new Vector3D(-1, -1, -1),
                new Vector3D(1, -1, -1),
                new Vector3D(1, 1, -1),
                new Vector3D(-1, 1, -1),
                new Vector3D(-1, -1, 1),
                new Vector3D(1, -1, 1),
                new Vector3D(1, 1, 1),
                new Vector3D(-1, 1, 1)
        };
    }

    private static Vector3D[] getTetrahedron() {
        return new Vector3D[]{
                new Vector3D(1, 1, 1),
                new Vector3D(-1, -1, 1),
                new Vector3D(-1, 1, -1),
                new Vector3D(1, -1, -1)
        };
    }
}
