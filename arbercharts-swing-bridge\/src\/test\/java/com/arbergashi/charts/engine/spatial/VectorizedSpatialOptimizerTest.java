package com.arbergashi.charts.engine.spatial;

import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.util.NiceScale;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VectorizedSpatialOptimizerTest {

    @Test
    void applyBoundsFilter_respectsMinPixelDistance() {
        SpatialBuffer buffer = new SpatialBuffer(8);
        double[] in = buffer.getInputCoords();
        // 3 points: (0,0), (1,1), (3,3)
        in[0] = 0.0; in[1] = 0.0; in[2] = 0.0;
        in[3] = 1.0; in[4] = 1.0; in[5] = 0.0;
        in[6] = 3.0; in[7] = 3.0; in[8] = 0.0;

        DefaultPlotContext ctx = new DefaultPlotContext(
                new Rectangle2D.Double(0, 0, 10, 10),
                0, 10, 0, 10, false,
                NiceScale.ScaleMode.LINEAR, NiceScale.ScaleMode.LINEAR
        );

        VectorizedSpatialOptimizer optimizer = new VectorizedSpatialOptimizer()
                .setMinPixelDistance(2.0);

        int kept = optimizer.applyBoundsFilter(buffer, 3, ctx, true);

        assertEquals(2, kept);
        assertEquals(0.0, in[0], 1e-9);
        assertEquals(0.0, in[1], 1e-9);
        assertEquals(3.0, in[3], 1e-9);
        assertEquals(3.0, in[4], 1e-9);
    }

    @Test
    void applyBoundsFilter_scalesByDepth() {
        SpatialBuffer buffer = new SpatialBuffer(8);
        double[] in = buffer.getInputCoords();
        // Two points close in XY, but far in Z for second point.
        in[0] = 0.0; in[1] = 0.0; in[2] = 0.0;
        in[3] = 0.5; in[4] = 0.5; in[5] = 10.0;

        DefaultPlotContext ctx = new DefaultPlotContext(
                new Rectangle2D.Double(-5, -5, 10, 10),
                -5, 5, -5, 5, false,
                NiceScale.ScaleMode.LINEAR, NiceScale.ScaleMode.LINEAR
        );

        VectorizedSpatialOptimizer optimizer = new VectorizedSpatialOptimizer()
                .setMinPixelDistance(1.0)
                .setDepthAttenuation(0.0, 10.0, 1.0);

        int kept = optimizer.applyBoundsFilter(buffer, 2, ctx, true);

        // First point kept; second filtered due to depth-scaled threshold.
        assertEquals(1, kept);
        assertEquals(0.0, in[0], 1e-9);
        assertEquals(0.0, in[1], 1e-9);
    }

    @Test
    void applyBoundsFilter_preservesLocalPeaks() {
        SpatialBuffer buffer = new SpatialBuffer(8);
        double[] in = buffer.getInputCoords();
        // Three points with a peak in the middle.
        in[0] = 0.0; in[1] = 0.0; in[2] = 0.0;
        in[3] = 1.0; in[4] = 10.0; in[5] = 0.0;
        in[6] = 2.0; in[7] = 0.0; in[8] = 0.0;

        DefaultPlotContext ctx = new DefaultPlotContext(
                new Rectangle2D.Double(0, 0, 10, 10),
                0, 10, 0, 10, false,
                NiceScale.ScaleMode.LINEAR, NiceScale.ScaleMode.LINEAR
        );

        VectorizedSpatialOptimizer optimizer = new VectorizedSpatialOptimizer()
                .setMinPixelDistance(2.0)
                .setPreservePeaks(true);

        int kept = optimizer.applyBoundsFilter(buffer, 3, ctx, true);

        assertEquals(3, kept);
        assertEquals(0.0, in[0], 1e-9);
        assertEquals(0.0, in[1], 1e-9);
        assertEquals(1.0, in[3], 1e-9);
        assertEquals(10.0, in[4], 1e-9);
        assertEquals(2.0, in[6], 1e-9);
        assertEquals(0.0, in[7], 1e-9);
    }

    @Test
    void applyBoundsFilterDelta_compactsRingWindow() {
        SpatialBuffer buffer = new SpatialBuffer(4);
        buffer.setRingEnabled(true);
        buffer.writeRing(0.0, 0.0, 0.0);
        buffer.writeRing(1.0, 1.0, 0.0);
        buffer.writeRing(2.0, 2.0, 0.0);
        buffer.writeRing(3.0, 3.0, 0.0);
        buffer.writeRing(4.0, 4.0, 0.0); // overwrites first

        DefaultPlotContext ctx = new DefaultPlotContext(
                new Rectangle2D.Double(0, 0, 10, 10),
                0, 10, 0, 10, false,
                NiceScale.ScaleMode.LINEAR, NiceScale.ScaleMode.LINEAR
        );

        VectorizedSpatialOptimizer optimizer = new VectorizedSpatialOptimizer();
        int kept = optimizer.applyBoundsFilterDelta(buffer, 0, 3, ctx, true);

        assertEquals(3, kept);
        double[] in = buffer.getInputCoords();
        assertEquals(1.0, in[0], 1e-9);
        assertEquals(1.0, in[1], 1e-9);
        assertEquals(2.0, in[3], 1e-9);
        assertEquals(2.0, in[4], 1e-9);
        assertEquals(3.0, in[6], 1e-9);
        assertEquals(3.0, in[7], 1e-9);
    }
}
