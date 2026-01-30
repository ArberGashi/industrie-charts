package com.arbergashi.charts.engine.spatial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VectorizedHitDetectorTest {

    @Test
    void findHitIndex_detectsNearestWithinRadius() {
        SpatialBuffer buffer = new SpatialBuffer(4);
        double[] in = buffer.getInputCoords();
        // Points: (0,0), (5,5), (10,10)
        in[0] = 0.0; in[1] = 0.0; in[2] = 0.0;
        in[3] = 5.0; in[4] = 5.0; in[5] = 0.0;
        in[6] = 10.0; in[7] = 10.0; in[8] = 0.0;

        VectorizedHitDetector detector = new VectorizedHitDetector();
        int hit = detector.findHitIndex(buffer, 3, 5.4, 5.2, 1.0);

        assertEquals(1, hit);
    }
}
