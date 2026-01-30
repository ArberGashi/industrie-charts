package com.arbergashi.charts.engine.spatial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpatialPathBatchTest {
    private static final double EPS = 1e-9;

    @Test
    void ensureCapacity_preserves_data() {
        SpatialPathBatch batch = new SpatialPathBatch(2);
        SpatialBuffer buffer = new SpatialBuffer(2);
        double[] in = buffer.getInputCoords();
        in[0] = 1.0; in[1] = 2.0; in[2] = 3.0;
        in[3] = 4.0; in[4] = 5.0; in[5] = 6.0;
        batch.setFromBuffer(buffer, 2);

        batch.ensureCapacity(6);
        assertEquals(2, batch.getPointCount());
        assertEquals(1.0, batch.getXData()[0], EPS);
        assertEquals(2.0, batch.getYData()[0], EPS);
        assertEquals(4.0, batch.getXData()[1], EPS);
        assertEquals(5.0, batch.getYData()[1], EPS);
    }

    @Test
    void setFromBuffer_maps_xy_correctly() {
        SpatialPathBatch batch = new SpatialPathBatch(3);
        SpatialBuffer buffer = new SpatialBuffer(3);
        double[] in = buffer.getInputCoords();
        in[0] = 1.5; in[1] = -2.5; in[2] = 0.1;
        in[3] = 2.5; in[4] = -3.5; in[5] = 0.2;
        in[6] = 3.5; in[7] = -4.5; in[8] = 0.3;

        batch.setFromBuffer(buffer, 3);
        assertEquals(3, batch.getPointCount());
        assertEquals(1.5, batch.getXData()[0], EPS);
        assertEquals(-2.5, batch.getYData()[0], EPS);
        assertEquals(2.5, batch.getXData()[1], EPS);
        assertEquals(-3.5, batch.getYData()[1], EPS);
        assertEquals(3.5, batch.getXData()[2], EPS);
        assertEquals(-4.5, batch.getYData()[2], EPS);
    }

    @Test
    void setFromBufferClipped_marks_visibility_by_z() {
        SpatialPathBatch batch = new SpatialPathBatch(4);
        SpatialBuffer buffer = new SpatialBuffer(4);
        double[] in = buffer.getInputCoords();
        in[0] = 1.0; in[1] = 1.0; in[2] = -0.5;
        in[3] = 2.0; in[4] = 2.0; in[5] = 0.0;
        in[6] = 3.0; in[7] = 3.0; in[8] = 0.5;
        in[9] = 4.0; in[10] = 4.0; in[11] = 1.5;

        batch.setFromBufferClipped(buffer, 4, 0.0);

        assertFalse(batch.isVisible(0));
        assertFalse(batch.isVisible(1));
        assertTrue(batch.isVisible(2));
        assertTrue(batch.isVisible(3));
    }
}
