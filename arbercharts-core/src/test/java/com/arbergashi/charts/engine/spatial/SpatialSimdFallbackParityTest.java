package com.arbergashi.charts.engine.spatial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that SIMD kernels and scalar fallbacks produce identical results,
 * so the engine behaves the same with and without {@code --add-modules
 * jdk.incubator.vector} at runtime.
 */
class SpatialSimdFallbackParityTest {
    private static final int POINT_COUNT = 259; // odd size to exercise tail handling
    private static final double TOLERANCE = 1e-9;

    private static double[] samplePoints() {
        double[] coords = new double[POINT_COUNT * 3];
        for (int i = 0; i < POINT_COUNT; i++) {
            coords[i * 3] = Math.sin(i * 0.37) * 100.0;
            coords[i * 3 + 1] = Math.cos(i * 0.23) * 80.0;
            coords[i * 3 + 2] = Math.sin(i * 0.11) * 5.0 + 6.0;
        }
        return coords;
    }

    @Test
    void laneCountIsAtLeastOne() {
        assertTrue(VectorIntrinsics.getLaneCount() >= 1);
    }

    @Test
    void matrixBatchMatchesScalarPath() {
        Matrix4x4 matrix = new Matrix4x4()
                .setIdentity()
                .setRotationY(0.35)
                .setTranslation(3.0, -2.0, 0.5);

        double[] in = samplePoints();
        double[] outSimd = new double[in.length];
        double[] outScalar = new double[in.length];

        SpatialScratchBuffer scratch = new SpatialScratchBuffer(VectorIntrinsics.getLaneCount());
        matrix.getCalculatedTransformBatch(in, outSimd, POINT_COUNT, scratch);
        matrix.getCalculatedTransformBatch(in, outScalar, POINT_COUNT, null);

        for (int i = 0; i < in.length; i++) {
            assertEquals(outScalar[i], outSimd[i], TOLERANCE, "coord " + i);
        }
    }

    @Test
    void perspectiveBatchMatchesPerPointProjection() {
        PerspectiveProjector projector = new PerspectiveProjector()
                .setScale(120.0)
                .setCenterX(400.0)
                .setCenterY(300.0)
                .setZBias(2.5);

        double[] in = samplePoints();
        double[] outBatch = new double[in.length];
        projector.getCalculatedProjectionBatch(in, outBatch, POINT_COUNT);

        double[] single = new double[3];
        for (int i = 0; i < POINT_COUNT; i++) {
            projector.getCalculatedProjection(in[i * 3], in[i * 3 + 1], in[i * 3 + 2], single);
            assertEquals(single[0], outBatch[i * 3], TOLERANCE, "x at " + i);
            assertEquals(single[1], outBatch[i * 3 + 1], TOLERANCE, "y at " + i);
            assertEquals(single[2], outBatch[i * 3 + 2], TOLERANCE, "z at " + i);
        }
    }

    @Test
    void orthographicBatchMatchesPerPointProjection() {
        OrthographicProjector projector = new OrthographicProjector()
                .setScale(2.5)
                .setCenterX(150.0)
                .setCenterY(90.0);

        double[] in = samplePoints();
        double[] outBatch = new double[in.length];
        projector.getCalculatedProjectionBatch(in, outBatch, POINT_COUNT);

        double[] single = new double[3];
        for (int i = 0; i < POINT_COUNT; i++) {
            projector.getCalculatedProjection(in[i * 3], in[i * 3 + 1], in[i * 3 + 2], single);
            assertEquals(single[0], outBatch[i * 3], TOLERANCE, "x at " + i);
            assertEquals(single[1], outBatch[i * 3 + 1], TOLERANCE, "y at " + i);
            assertEquals(single[2], outBatch[i * 3 + 2], TOLERANCE, "z at " + i);
        }
    }

    @Test
    void nearestIndexMatchesBruteForce() {
        double[] points = samplePoints();
        SpatialBuffer buffer = new SpatialBuffer(POINT_COUNT);
        System.arraycopy(points, 0, buffer.getInputCoords(), 0, points.length);

        VectorizedHitDetector detector = new VectorizedHitDetector();
        double targetX = 12.0;
        double targetY = -20.0;
        double radius = 55.0;

        int hit = detector.getNearestIndex(buffer, POINT_COUNT, targetX, targetY, radius, false);

        double radiusSq = radius * radius;
        double bestDist = Double.MAX_VALUE;
        int expected = -1;
        for (int i = 0; i < POINT_COUNT; i++) {
            double dx = points[i * 3] - targetX;
            double dy = points[i * 3 + 1] - targetY;
            double d = dx * dx + dy * dy;
            if (d <= radiusSq && d < bestDist) {
                bestDist = d;
                expected = i;
            }
        }
        assertEquals(expected, hit);
    }

    @Test
    void nearestIndexInBoxMatchesBruteForce() {
        double[] points = samplePoints();
        SpatialBuffer buffer = new SpatialBuffer(POINT_COUNT);
        System.arraycopy(points, 0, buffer.getInputCoords(), 0, points.length);

        VectorizedHitDetector detector = new VectorizedHitDetector();
        double targetX = 5.0;
        double targetY = 10.0;
        double targetZ = 6.0;
        double halfX = 40.0;
        double halfY = 35.0;
        double halfZ = 4.0;

        int hit = detector.getNearestIndexInBox(buffer, POINT_COUNT, targetX, targetY, targetZ,
                halfX, halfY, halfZ, false);

        double bestDist = Double.MAX_VALUE;
        int expected = -1;
        for (int i = 0; i < POINT_COUNT; i++) {
            double dx = points[i * 3] - targetX;
            double dy = points[i * 3 + 1] - targetY;
            double dz = points[i * 3 + 2] - targetZ;
            if (Math.abs(dx) > halfX || Math.abs(dy) > halfY || Math.abs(dz) > halfZ) {
                continue;
            }
            double d = dx * dx + dy * dy + dz * dz;
            if (d < bestDist) {
                bestDist = d;
                expected = i;
            }
        }
        assertEquals(expected, hit);
    }
}
