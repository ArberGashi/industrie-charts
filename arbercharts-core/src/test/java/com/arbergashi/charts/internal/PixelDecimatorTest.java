package com.arbergashi.charts.internal;

import com.arbergashi.charts.api.PlotContext;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PixelDecimatorTest {

    @Test
    public void testEmptyData() {
        double[] x = new double[0];
        double[] y = new double[0];
        double[] outX = new double[10];
        double[] outY = new double[10];
        PlotContext ctx = createMockContext(100, 0, 100);

        int result = PixelDecimator.decimate(x, y, 0, ctx, outX, outY);
        assertEquals(0, result);
    }

    @Test
    public void testWidthZero() {
        double[] x = {1, 2, 3};
        double[] y = {4, 5, 6};
        double[] outX = new double[10];
        double[] outY = new double[10];
        PlotContext ctx = createMockContext(0, 0, 100);

        int result = PixelDecimator.decimate(x, y, 3, ctx, outX, outY);
        assertEquals(0, result);
    }

    @Test
    public void testDecimation() {
        // 1000 Punkte auf 100 Pixel Breite -> Dezimierung erwartet
        int count = 1000;
        double[] x = new double[count];
        double[] y = new double[count];
        for (int i = 0; i < count; i++) {
            x[i] = i;
            y[i] = Math.sin(i * 0.1);
        }

        double[] outX = new double[count * 2];
        double[] outY = new double[count * 2];
        PlotContext ctx = createMockContext(100, 0, 1000);

        int result = PixelDecimator.decimate(x, y, count, ctx, outX, outY);

        // Wir erwarten ca. 2 Punkte pro Pixel-Bucket. 
        // 100 Pixel * 2 = 200 Punkte (+/- 2 für Randeffekte)
        assertTrue(result <= 202, "Resulting points should be around 200, but was " + result);
        assertTrue(result > 180, "Resulting points should be significant");
    }

    @Test
    public void testBufferOverflowProtection() {
        int count = 1000;
        double[] x = new double[count];
        double[] y = new double[count];
        for (int i = 0; i < count; i++) {
            x[i] = i;
            y[i] = i;
        }

        // Zu kleiner Ausgabe-Buffer
        double[] outX = new double[10];
        double[] outY = new double[10];
        PlotContext ctx = createMockContext(100, 0, 1000);

        int result = PixelDecimator.decimate(x, y, count, ctx, outX, outY);
        assertTrue(result <= 10);
    }

    private PlotContext createMockContext(double width, double minX, double maxX) {
        return new PlotContext() {
            @Override
            public double minX() {
                return minX;
            }

            @Override
            public double maxX() {
                return maxX;
            }

            @Override
            public double minY() {
                return 0;
            }

            @Override
            public double maxY() {
                return 0;
            }

            @Override
            public Rectangle2D plotBounds() {
                return new Rectangle2D.Double(0, 0, width, 100);
            }

            @Override
            public void mapToPixel(double x, double y, double[] out) {
            }

            @Override
            public void mapToData(double pixelX, double pixelY, double[] dest) {
            }
        };
    }
}
