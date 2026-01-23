package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.internal.HitTestUtils;
import com.arbergashi.charts.internal.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EdgeCasesRenderersTest {

    @Test
    public void rendererRegistryFactorySanity() {
        Map<String, ? extends com.arbergashi.charts.internal.RendererDescriptor> desc = RendererRegistry.descriptors();
        assertNotNull(desc);
        // For each descriptor, try to create the renderer; creation may be null in some environments but must not throw
        for (String id : desc.keySet()) {
            try {
                var r = RendererRegistry.create(id);
                if (r != null) assertNotNull(r.getName());
            } catch (Throwable t) {
                fail("Factory creation for " + id + " threw: " + t);
            }
        }
    }

    @Test
    public void hitTestUtilsNegative() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0, 1.0};
            final double[] ys = {0.0, 1.0};

            @Override
            public String getName() {
                return "h2";
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
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        PlotContext ctx = new DefaultPlotContext(new Rectangle2D.Double(0, 0, 100, 100), model, 0, 1, 0, 1);
        var miss = HitTestUtils.nearestPointIndex(new Point2D.Double(5000, 5000), model, ctx);
        assertTrue(miss.isEmpty());
    }

    @Test
    public void candlestickDojiRenderAndHit() {
        com.arbergashi.charts.render.financial.CandlestickRenderer r = new com.arbergashi.charts.render.financial.CandlestickRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0};
            final double[] highs = {10.0};
            final double[] lows = {5.0};
            final double[] opens = {7.0};
            final double[] closes = {7.0};

            @Override
            public String getName() {
                return "doji";
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
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };

        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 200, 100);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            double[] buf = new double[2];
            ctx.mapToPixel(model.getX(0), model.getY(0), buf);
            var hit = r.getPointAt(new Point2D.Double(buf[0], buf[1]), model, ctx);
            assertTrue(hit.isPresent());
            assertEquals(0, hit.get());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void boxplotZeroIqrNoCrash() {
        com.arbergashi.charts.render.statistical.BoxPlotRenderer r = new com.arbergashi.charts.render.statistical.BoxPlotRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0};
            final double[] med = {2.0};

            @Override
            public String getName() {
                return "box_zero";
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
                return med;
            }

            @Override
            public double getValue(int index, int component) {
                if (component == 2) return 0.0;
                if (component == 3) return 2.0;
                if (component == 4) return 2.0;
                return med[index];
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 200, 120);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void hexbinDenseRenderSmoke() {
        com.arbergashi.charts.render.specialized.HexbinRenderer r = new com.arbergashi.charts.render.specialized.HexbinRenderer();
        final int n = 10000;
        ChartModel model = new ChartModel() {
            final double[] xs = new double[n];
            final double[] ys = new double[n];

            {
                for (int i = 0; i < n; i++) {
                    xs[i] = Math.random() * 10;
                    ys[i] = Math.random() * 5 + 1;
                }
            }

            @Override
            public String getName() {
                return "hex_dense";
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
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 500, 300);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }
}
