package com.arbergashi.charts.engine.spatial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpatialBufferStabilityTest {

    @Test
    void ringBufferHandlesCapacityOne() {
        SpatialBuffer buffer = new SpatialBuffer(1);
        buffer.setRingEnabled(true);

        buffer.writeRing(1.0, 2.0, 3.0);
        assertEquals(1, buffer.getRingCount());
        assertEquals(0, buffer.getRingStart());
        assertEquals(1, buffer.getRingDeltaCount());

        double[] out = new double[3];
        buffer.getRingPoint(0, out);
        assertEquals(1.0, out[0], 1e-9);
        assertEquals(2.0, out[1], 1e-9);
        assertEquals(3.0, out[2], 1e-9);

        buffer.consumeRingDelta();
        assertEquals(0, buffer.getRingDeltaCount());

        buffer.writeRing(4.0, 5.0, 6.0);
        assertEquals(1, buffer.getRingCount());
        assertEquals(0, buffer.getRingStart());
        assertEquals(1, buffer.getRingDeltaCount());
        buffer.getRingPoint(0, out);
        assertEquals(4.0, out[0], 1e-9);
        assertEquals(5.0, out[1], 1e-9);
        assertEquals(6.0, out[2], 1e-9);
    }

    @Test
    void ringDeltaCapsAtCapacity() {
        SpatialBuffer buffer = new SpatialBuffer(4);
        buffer.setRingEnabled(true);

        for (int i = 0; i < 20; i++) {
            buffer.writeRing(i, i * 2.0, i * 3.0);
        }

        assertEquals(4, buffer.getRingCount());
        assertEquals(4, buffer.getRingDeltaCount());

        buffer.consumeRingDelta();
        assertEquals(0, buffer.getRingDeltaCount());

        buffer.writeRing(21.0, 22.0, 23.0);
        assertEquals(1, buffer.getRingDeltaCount());
    }

    @Test
    void ringDeltaStartTracksLatestWindow() {
        SpatialBuffer buffer = new SpatialBuffer(5);
        buffer.setRingEnabled(true);

        for (int i = 0; i < 7; i++) {
            buffer.writeRing(i, i + 10.0, i + 20.0);
        }

        assertEquals(5, buffer.getRingCount());
        int start = buffer.getRingDeltaStartLogical();
        assertTrue(start >= 0);
        assertEquals(5, buffer.getRingDeltaCount());

        double[] out = new double[3];
        buffer.getRingPoint(start, out);
        assertEquals(2.0, out[0], 1e-9);
        buffer.getRingPoint(4, out);
        assertEquals(6.0, out[0], 1e-9);
    }

    @Test
    void resetRingDeltaCursorForcesFullWindow() {
        SpatialBuffer buffer = new SpatialBuffer(3);
        buffer.setRingEnabled(true);

        buffer.writeRing(1, 1, 1);
        buffer.writeRing(2, 2, 2);
        buffer.consumeRingDelta();
        assertEquals(0, buffer.getRingDeltaCount());

        buffer.resetRingDeltaCursor();
        assertEquals(2, buffer.getRingDeltaCount());
    }
}
