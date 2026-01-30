package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.financial.CandlestickHollowRenderer;
import com.arbergashi.charts.render.financial.WaterfallRenderer;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FinancialMoreRenderersTest {

    @Test
    public void waterfallRendererPointHitTest() {
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

            // map the second point to pixel - pick midpoint between start and end of the bar
            double[] buf = new double[2];
            double running = 0.0;
            for (int k = 0; k < 1; k++) running += model.getY(k);
            double midY = running + model.getY(1) * 0.5;
            ctx.mapToPixel(model.getX(1), midY, buf);
            Optional<Integer> hit = r.getPointAt(new Point2D.Double(buf[0], buf[1]), model, ctx);
            assertTrue(hit.isPresent());
            // The nearest bar should be index 1
            assertEquals(1, hit.get());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void candlestickHollowPerPointRenderSmoke() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0, 1.0, 2.0, 3.0, 4.0};
            final double[] ys = {7.0, 6.0, 9.0, 8.0, 7.5};
            final double[] highs = {8.0, 7.0, 10.0, 9.0, 8.5};
            final double[] lows = {6.0, 5.5, 8.0, 7.5, 6.5};
            final double[] opens = {6.5, 6.5, 9.5, 8.5, 7.0};

            @Override
            public String getName() {
                return "candles_small";
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
            public double getMax(int index) {
                return highs[index];
            }

            @Override
            public double getMin(int index) {
                return lows[index];
            }

            @Override
            public double getWeight(int index) {
                return opens[index];
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };

        CandlestickHollowRenderer r = new CandlestickHollowRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(10, 10, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            // For small n this should take the per-point path (no exception)
            r.render(g2, model, ctx);
            assertEquals("candlestick_hollow", r.getName());
        } finally {
            g2.dispose();
        }
    }
}
