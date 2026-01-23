package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.ChartModel;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class PerformanceMicrobenchmarksTest {

    @Test
    public void pBufferIdentitySameThread() {
        HelperRenderer r = new HelperRenderer();
        double[] a1 = r.exposePB();
        double[] a2 = r.exposePB();
        assertSame(a1, a2, "pBuffer() must return same array instance per thread");
        assertEquals(2, a1.length);
        double[] b1 = r.exposePB4();
        assertEquals(4, b1.length);
    }

    @Test
    public void pBufferThreadLocalIndependence() throws Exception {
        HelperRenderer r = new HelperRenderer();
        double[] main = r.exposePB();
        AtomicReference<double[]> otherRef = new AtomicReference<>();
        Thread t = new Thread(() -> otherRef.set(r.exposePB()));
        t.start();
        t.join();
        assertNotNull(otherRef.get());
        assertNotSame(main, otherRef.get(), "Different threads must have distinct pBuffer instances");
    }

    @Test
    public void strokeCacheConsistency() {
        HelperRenderer r = new HelperRenderer();
        Stroke s1 = r.exposeCachedStroke(1.0f);
        Stroke s2 = r.exposeCachedStroke(1.0f);
        assertSame(s1, s2);
        Stroke s3 = r.exposeCachedStroke(2.0f);
        assertNotSame(s1, s3);
    }

    @Test
    public void pathCacheReusedButResetByRender() {
        HelperRenderer r = new HelperRenderer();
        Path2D.Double p = r.exposePathCache();
        // draw something (path initially empty)
        p.moveTo(1, 1);
        p.lineTo(2, 2);
        // Call render which resets path at frame start
        ChartModel model = new ChartModel() {
            final double[] xs = {0};

            @Override
            public String getName() {
                return "m";
            }

            @Override
            public int getPointCount() {
                return xs.length;
            }

            @Override
            public double[] getXData() {
                return xs;
            }

            @Override
            public double[] getYData() {
                return xs;
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 10, 10);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
        // After render, the path should be empty (reset)
        assertTrue(p.getBounds2D().isEmpty());
    }

    // Small helper renderer to expose protected helpers from BaseRenderer
    static final class HelperRenderer extends BaseRenderer {
        HelperRenderer() {
            super("helper");
        }

        @Override
        protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        }

        public double[] exposePB() {
            return pBuffer();
        }

        public double[] exposePB4() {
            return pBuffer4();
        }

        public Path2D.Double exposePathCache() {
            return getPathCache();
        }

        public Stroke exposeCachedStroke(float w) {
            return getCachedStroke(w);
        }
    }
}
