package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.internal.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class RendererRegistrySmokeTest {

    @Test
    public void instantiateAndRenderAllSimple() {
        Map<String, ? extends com.arbergashi.charts.internal.RendererDescriptor> desc = RendererRegistry.descriptors();
        assertNotNull(desc);
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 1, 2};
            final double[] ys = {1, 2, 3};

            @Override
            public String getName() {
                return "r";
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
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 300, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(300, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            for (String id : desc.keySet()) {
                try {
                    var r = RendererRegistry.getRenderer(id);
                    if (r == null) continue;
                    // Try to render with simple model; many renderers expect specialized models and will draw an error message but must not throw
                    r.render(g2, model, ctx);
                } catch (Throwable t) {
                    fail("Renderer " + id + " threw: " + t);
                }
            }
        } finally {
            g2.dispose();
        }
    }
}
