package com.arbergashi.charts.engine.spatial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpatialPathBatchBuilderTest {
    @Test
    void builder_sets_move_flags_on_visibility_transitions() {
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder().setZMin(0.0);
        SpatialBuffer buffer = new SpatialBuffer(6);
        double[] in = buffer.getInputCoords();

        // z sequence: out, out, in, in, out, in
        in[0] = 0.0; in[1] = 0.0; in[2] = -1.0;
        in[3] = 1.0; in[4] = 1.0; in[5] = 0.0;
        in[6] = 2.0; in[7] = 2.0; in[8] = 0.1;
        in[9] = 3.0; in[10] = 3.0; in[11] = 0.2;
        in[12] = 4.0; in[13] = 4.0; in[14] = -0.5;
        in[15] = 5.0; in[16] = 5.0; in[17] = 0.3;

        builder.accept(buffer, 6);

        SpatialPathBatch batch = builder.getBatch();
        assertEquals(6, batch.getPointCount());
        assertTrue(builder.isMoveTo(0));
        assertFalse(builder.isMoveTo(1));
        assertFalse(builder.isMoveTo(2));
        assertFalse(builder.isMoveTo(3));
        assertTrue(builder.isMoveTo(4));
        assertFalse(builder.isMoveTo(5));
        assertEquals(1.0, batch.getXData()[0], 1e-9); // clip between first two points
        assertEquals(2.0, batch.getXData()[1], 1e-9); // first visible
        assertEquals(3.0, batch.getXData()[2], 1e-9); // second visible
        assertEquals(3.2857142857142856, batch.getXData()[3], 1e-9); // clip before drop
        assertEquals(4.625, batch.getXData()[4], 1e-9); // clip into last visible
        assertEquals(5.0, batch.getXData()[5], 1e-9); // last visible
    }

    @Test
    void builder_interpolates_clip_point_on_entry() {
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder().setZMin(0.0);
        SpatialBuffer buffer = new SpatialBuffer(2);
        double[] in = buffer.getInputCoords();
        in[0] = 0.0; in[1] = 0.0; in[2] = -2.0;
        in[3] = 10.0; in[4] = 10.0; in[5] = 2.0;

        builder.accept(buffer, 2);

        SpatialPathBatch batch = builder.getBatch();
        assertEquals(2, batch.getPointCount());
        assertTrue(builder.isMoveTo(0));
        assertFalse(builder.isMoveTo(1));
        assertEquals(5.0, batch.getXData()[0], 1e-9);
        assertEquals(5.0, batch.getYData()[0], 1e-9);
        assertEquals(10.0, batch.getXData()[1], 1e-9);
        assertEquals(10.0, batch.getYData()[1], 1e-9);
    }

    @Test
    void builder_clamps_points_in_clamp_mode() {
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder()
                .setZMin(0.0)
                .setClippingMode(SpatialPathBatchBuilder.ClippingMode.CLAMP);
        SpatialBuffer buffer = new SpatialBuffer(2);
        double[] in = buffer.getInputCoords();
        in[0] = 1.0; in[1] = 1.0; in[2] = -2.0;
        in[3] = 2.0; in[4] = 2.0; in[5] = -1.0;

        builder.accept(buffer, 2);

        SpatialPathBatch batch = builder.getBatch();
        assertEquals(2, batch.getPointCount());
        assertTrue(builder.isMoveTo(0));
        assertFalse(builder.isMoveTo(1));
        assertEquals(1.0, batch.getXData()[0], 1e-9);
        assertEquals(2.0, batch.getXData()[1], 1e-9);
    }

    @Test
    void builder_handles_points_exactly_on_clip_plane() {
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder().setZMin(0.0);
        SpatialBuffer buffer = new SpatialBuffer(3);
        double[] in = buffer.getInputCoords();
        in[0] = 0.0; in[1] = 0.0; in[2] = -1.0;
        in[3] = 1.0; in[4] = 1.0; in[5] = 0.0;
        in[6] = 2.0; in[7] = 2.0; in[8] = 1.0;

        builder.accept(buffer, 3);

        SpatialPathBatch batch = builder.getBatch();
        assertEquals(2, batch.getPointCount());
        assertTrue(builder.isMoveTo(0));
        assertFalse(builder.isMoveTo(1));
        assertEquals(1.0, batch.getXData()[0], 1e-9);
        assertEquals(1.0, batch.getYData()[0], 1e-9);
        assertEquals(2.0, batch.getXData()[1], 1e-9);
        assertEquals(2.0, batch.getYData()[1], 1e-9);
    }

    @Test
    void builder_skips_chunk_when_behind_camera() {
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder().setZMin(0.0);
        SpatialBuffer buffer = new SpatialBuffer(3);
        double[] in = buffer.getInputCoords();
        in[0] = 1.0; in[1] = 1.0; in[2] = -2.0;
        in[3] = 2.0; in[4] = 2.0; in[5] = -1.0;
        in[6] = 3.0; in[7] = 3.0; in[8] = -0.1;

        assertFalse(builder.isChunkPotentiallyVisible(buffer, 3));
        builder.accept(buffer, 3);
        assertEquals(0, builder.getBatch().getPointCount());
    }
}
