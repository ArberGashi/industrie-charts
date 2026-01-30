package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Line2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class BaseRendererTest {

    @Test
    public void pixelBuffersAndCaches() {
        DummyRenderer r = new DummyRenderer();
        assertEquals(2, r.exposePBuffer().length);
        double[] a = r.exposePBuffer4();
        double[] b = r.exposePBuffer4();
        assertSame(a, b);
        assertEquals(4, b.length);

        Stroke s1 = r.exposeCachedStroke(1.5f);
        Stroke s2 = r.exposeCachedStroke(1.5f);
        assertSame(s1, s2);

        Font f1 = r.exposeCachedFont(10f, Font.BOLD);
        Font f2 = r.exposeCachedFont(10f, Font.BOLD);
        assertSame(f1, f2);

        Line2D l = r.exposeLine(1, 2, 3, 4);
        assertEquals(1.0, l.getX1(), 1e-6);
        assertEquals(3.0, l.getX2(), 1e-6);
        assertEquals(4.0, l.getY2(), 1e-6);
    }

    private static final class DummyRenderer extends BaseRenderer {
        DummyRenderer() {
            super("dummy");
        }

        @Override
        protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
            // no-op
        }

        public double[] exposePBuffer() {
            return pBuffer();
        }

        public double[] exposePBuffer4() {
            return pBuffer4();
        }

        public Stroke exposeCachedStroke(float w) {
            return getCachedStroke(w);
        }

        public Font exposeCachedFont(float baseSize, int style) {
            return getCachedFont(baseSize, style);
        }

        public Line2D exposeLine(double x1, double y1, double x2, double y2) {
            return getLine(x1, y1, x2, y2);
        }
    }
}