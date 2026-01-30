package com.arbergashi.charts.engine.spatial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpatialBufferRingTest {

    @Test
    void writeRingWrapsAndPreservesOrder() {
        SpatialBuffer buffer = new SpatialBuffer(4);
        buffer.setRingEnabled(true);

        for (int i = 0; i < 6; i++) {
            buffer.writeRing(i, i * 10.0, -i);
        }

        assertEquals(4, buffer.getRingCount());
        assertEquals(2, buffer.getRingStart());

        double[] out = new double[3];
        buffer.getRingPoint(0, out);
        assertEquals(2.0, out[0], 1e-9);
        assertEquals(20.0, out[1], 1e-9);
        assertEquals(-2.0, out[2], 1e-9);

        buffer.getRingPoint(3, out);
        assertEquals(5.0, out[0], 1e-9);
        assertEquals(50.0, out[1], 1e-9);
        assertEquals(-5.0, out[2], 1e-9);
    }
}
