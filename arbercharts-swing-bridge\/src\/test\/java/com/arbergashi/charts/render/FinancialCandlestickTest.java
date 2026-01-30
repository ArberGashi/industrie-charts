package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.financial.CandlestickHollowRenderer;
import com.arbergashi.charts.render.financial.CandlestickRenderer;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FinancialCandlestickTest {

    private static ChartModel simpleOHLC() {
        return new ChartModel() {
            final double[] xs = {0.0, 1.0, 2.0};
            final double[] highs = {10.0, 8.0, 12.0};
            final double[] lows = {5.0, 3.0, 6.0};
            final double[] opens = {6.0, 7.0, 9.0};
            final double[] closes = {7.5, 6.5, 11.0};

            @Override
            public String getName() {
                return "candles";
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
                return closes;
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
            public double getY(int index) {
                return closes[index];
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
    public void candlestickRendererHitTest() {
        ChartModel model = simpleOHLC();
        CandlestickRenderer r = new CandlestickRenderer();

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
    public void candlestickHollowRendererDenseLodRender() {
        final int n = 3000; // trigger LOD path (n > Math.max(2000, bounds.width))
        ChartModel model = new ChartModel() {
            final double[] xs = new double[n];
            final double[] highs = new double[n];
            final double[] lows = new double[n];
            final double[] opens = new double[n];
            final double[] closes = new double[n];

            {
                for (int i = 0; i < n; i++) {
                    xs[i] = i;
                    double base = Math.sin(i * 0.01) * 10.0 + 50.0;
                    highs[i] = base + 2.0;
                    lows[i] = base - 2.0;
                    opens[i] = base + ((i % 2 == 0) ? 0.5 : -0.5);
                    closes[i] = base + ((i % 3 == 0) ? 1.0 : -1.0);
                }
            }

            @Override
            public String getName() {
                return "candles_dense";
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
                return closes;
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
            public double getY(int index) {
                return closes[index];
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
            // Rendering a dense dataset should complete (LOD path exercised); no exception means success.
            r.render(g2, model, ctx);
            assertEquals("candlestick_hollow", r.getName());
        } finally {
            g2.dispose();
        }
    }
}
