package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.internal.HitTestUtils;
import com.arbergashi.charts.internal.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.util.ChartUtils;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UtilitiesAndRegistryTest {

    @Test
    public void plotContextRoundTrip() {
        Rectangle2D.Double bounds = new Rectangle2D.Double(10, 10, 400, 200);
        // Use an explicit numeric range
        PlotContext ctx = new DefaultPlotContext(bounds, (ChartModel) null, 0.0, 100.0, 0.0, 100.0);

        double[] buf = new double[2];
        double[] out = new double[2];
        for (double x : new double[]{0.0, 25.0, 50.0, 75.0, 100.0}) {
            for (double y : new double[]{0.0, 10.0, 50.0, 90.0, 100.0}) {
                ctx.mapToPixel(x, y, buf);
                ctx.mapToData(buf[0], buf[1], out);
                assertEquals(x, out[0], 1e-6);
                assertEquals(y, out[1], 1e-6);
            }
        }
    }

    @Test
    public void hitTestNearestPoint() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0, 1.0, 2.0};
            final double[] ys = {0.0, 2.0, 4.0};

            @Override
            public String getName() {
                return "hit";
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
                return ys;
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };

        PlotContext ctx = new DefaultPlotContext(new Rectangle2D.Double(0, 0, 300, 300), model, 0, 2, 0, 4);
        double[] buf = new double[2];
        ctx.mapToPixel(1.0, 2.0, buf);
        var hit = HitTestUtils.nearestPointIndex(new Point2D.Double(buf[0], buf[1]), model, ctx);
        assertTrue(hit.isPresent());
        assertEquals(1, hit.get());
    }

    @Test
    public void chartUtilsAndRegistrySanity() {
        // ChartUtils.calculateBestBarWidth
        double w1 = com.arbergashi.charts.util.ChartUtils.getCalculatedBestBarWidth(10, 200.0, 0.2f);
        double w2 = com.arbergashi.charts.util.ChartUtils.getCalculatedBestBarWidth(5, 200.0, 0.2f);
        assertTrue(w1 >= 1.0);
        assertTrue(w2 > w1);

        // mapToPolar returns a point inside bounds
        java.awt.geom.Point2D dest = new java.awt.geom.Point2D.Double();
        PlotContext ctx = new DefaultPlotContext(new Rectangle2D.Double(0, 0, 400, 200), (ChartModel) null, 0, 1, 0, 100.0);
        ChartUtils.mapToPolar(45.0, 50.0, ctx, dest);
        assertTrue(ctx.getPlotBounds().contains(dest));

        // Renderer registry has some known entries (registered by static inits)
        Map<String, ? extends com.arbergashi.charts.internal.RendererDescriptor> desc = RendererRegistry.descriptors();
        // Registry descriptors should be non-null (registry may be empty if no renderers have been loaded in this JVM test run)
        assertNotNull(desc);
    }
}
