package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.financial.HighLowRenderer;
import com.arbergashi.charts.render.financial.WaterfallRenderer;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class FinancialRenderersTest {

    private static ChartModel simpleOHLC() {
        return new ChartModel() {
            final double[] xs = {0.0, 1.0, 2.0};
            final double[] highs = {10.0, 8.0, 12.0};
            final double[] lows = {5.0, 3.0, 6.0};
            final double[] mids = {7.5, 5.5, 9.0};

            @Override
            public String getName() {
                return "test";
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
                return mids;
            }

            @Override
            public double getMax(int index) {
                return highs[index];
            }

            @Override
            public double getMin(int index) {
                return lows[index];
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
    }

    @Test
    public void highLowRendererHitTest() {
        ChartModel model = simpleOHLC();
        HighLowRenderer r = new HighLowRenderer();

        Rectangle2D.Double bounds = new Rectangle2D.Double(10, 10, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);

            // map the second point to pixel and query getPointAt
            double[] buf = new double[2];
            ctx.mapToPixel(model.getX(1), model.getY(1), buf);
            Optional<Integer> hit = r.getPointAt(new Point2D.Double(buf[0], buf[1]), model, ctx);
            assertTrue(hit.isPresent());
            assertEquals(1, hit.get());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void waterfallRendererRenderAndShape() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0, 1.0, 2.0, 3.0};
            final double[] ys = {5.0, -3.0, 8.0, 2.0};

            @Override
            public String getName() {
                return "wf";
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

        WaterfallRenderer r = new WaterfallRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(10, 10, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            assertNotNull(r.getRenderedShape(model, ctx));
            // Expect rendered shape covers plot bounds
            assertEquals(bounds, r.getRenderedShape(model, ctx));
        } finally {
            g2.dispose();
        }
    }
}
