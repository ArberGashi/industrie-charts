package com.arbergashi.charts.engine.spatial;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpatialCoreTestSuite {
    private static final double EPS = 1e-9;

    @Test
    void precisionAudit_orthographic_and_perspective() {
        OrthographicProjector ortho = new OrthographicProjector()
                .setScale(1.25)
                .setCenterX(120.0)
                .setCenterY(80.0);
        double[] orthoDelta = SpatialValidator.getCalculatedPrecisionAudit(ortho, 2048);
        assertTrue(orthoDelta[0] <= EPS && orthoDelta[1] <= EPS,
                "Ortho deltas too large: " + orthoDelta[0] + ", " + orthoDelta[1]);

        PerspectiveProjector persp = new PerspectiveProjector()
                .setScale(2.0)
                .setCenterX(320.0)
                .setCenterY(240.0)
                .setZBias(1.0);
        double[] perspDelta = SpatialValidator.getCalculatedPrecisionAudit(persp, 2048);
        assertTrue(perspDelta[0] <= EPS && perspDelta[1] <= EPS,
                "Perspective deltas too large: " + perspDelta[0] + ", " + perspDelta[1]);
    }

    @Test
    void batchTailHandling_matches_scalar_path() {
        int lane = Math.max(1, VectorIntrinsics.getLaneCount());
        int[] counts = new int[]{lane - 1, lane + 1, lane * 2 + 3};
        OrthographicProjector projector = new OrthographicProjector()
                .setScale(1.0)
                .setCenterX(0.0)
                .setCenterY(0.0);

        Random rnd = new Random(42);
        for (int count : counts) {
            SpatialBuffer buffer = new SpatialBuffer(count);
            double[] in = buffer.getInputCoords();
            double[] out = buffer.getOutputCoords();
            for (int i = 0, j = 0; i < count; i++, j += 3) {
                in[j] = rnd.nextDouble() * 1000.0 - 500.0;
                in[j + 1] = rnd.nextDouble() * 1000.0 - 500.0;
                in[j + 2] = rnd.nextDouble() * 1000.0 - 500.0;
            }
            projector.getCalculatedProjectionBatch(in, out, count);
            for (int i = 0, j = 0; i < count; i++, j += 3) {
                Vector3D single = projector.getCalculatedProjection(
                        new Vector3D(in[j], in[j + 1], in[j + 2]));
                double dx = Math.abs(out[j] - single.getX());
                double dy = Math.abs(out[j + 1] - single.getY());
                assertTrue(dx <= EPS && dy <= EPS, "Tail mismatch at " + i);
            }
        }
    }

    @Test
    void matrixBatch_identity_matches_input() {
        int lane = Math.max(1, VectorIntrinsics.getLaneCount());
        int count = lane * 2 + 1;
        SpatialBuffer buffer = new SpatialBuffer(count);
        double[] in = buffer.getInputCoords();
        double[] out = buffer.getOutputCoords();
        for (int i = 0, j = 0; i < count; i++, j += 3) {
            in[j] = i * 0.1;
            in[j + 1] = i * -0.2;
            in[j + 2] = i * 0.3;
        }
        Matrix4x4 m = new Matrix4x4().setIdentity();
        m.getCalculatedTransformBatch(in, out, count, buffer.getScratch());
        for (int i = 0, j = 0; i < count; i++, j += 3) {
            assertTrue(Math.abs(out[j] - in[j]) <= EPS, "x mismatch at " + i);
            assertTrue(Math.abs(out[j + 1] - in[j + 1]) <= EPS, "y mismatch at " + i);
            assertTrue(Math.abs(out[j + 2] - in[j + 2]) <= EPS, "z mismatch at " + i);
        }
    }

    @Test
    void allocationAudit_batch_path_is_zero_gc() {
        var bean = ManagementFactory.getThreadMXBean();
        if (!(bean instanceof com.sun.management.ThreadMXBean tm)) {
            Assumptions.assumeTrue(false, "ThreadMXBean allocations not supported");
            return;
        }
        tm.setThreadAllocatedMemoryEnabled(true);
        long threadId = Thread.currentThread().threadId();

        int count = Math.max(1, VectorIntrinsics.getLaneCount()) * 8;
        SpatialBuffer buffer = new SpatialBuffer(count);
        double[] in = buffer.getInputCoords();
        double[] out = buffer.getOutputCoords();
        for (int i = 0, j = 0; i < count; i++, j += 3) {
            in[j] = i * 0.01;
            in[j + 1] = i * -0.02;
            in[j + 2] = i * 0.03;
        }

        Matrix4x4 m = new Matrix4x4().setIdentity();
        OrthographicProjector projector = new OrthographicProjector()
                .setScale(1.0)
                .setCenterX(0.0)
                .setCenterY(0.0);

        for (int i = 0; i < 200; i++) {
            m.getCalculatedTransformBatch(in, out, count, buffer.getScratch());
            projector.getCalculatedProjectionBatch(in, out, count);
        }

        long before = tm.getThreadAllocatedBytes(threadId);
        for (int i = 0; i < 1000; i++) {
            m.getCalculatedTransformBatch(in, out, count, buffer.getScratch());
            projector.getCalculatedProjectionBatch(in, out, count);
        }
        long after = tm.getThreadAllocatedBytes(threadId);
        long delta = after - before;

        assertTrue(delta < 4096, "Allocation detected in SIMD batch path: " + delta + " bytes");
    }
}
