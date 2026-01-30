package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.ChartModel;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FinancialEdgeCasesTest {

    @Test
    public void candlestickDojiAndHit() {
        com.arbergashi.charts.render.financial.CandlestickRenderer r = new com.arbergashi.charts.render.financial.CandlestickRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {1.0};
            final double[] highs = {10.0};
            final double[] lows = {5.0};
            final double[] opens = {7.0};
            final double[] closes = {7.0};
            final double[] weights = {10.0};

            @Override
            public String getName() {
                return "cand_doji";
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
            public double getWeight(int index) {
                return weights[index];
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
            public double getValue(int index, int component) {
                if (component == 0) return opens[index];
                return closes[index];
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(800, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            double[] p = new double[2];
            ctx.mapToPixel(model.getX(0), model.getY(0), p);
            var opt = r.getPointAt(new Point2D.Double(p[0], p[1]), model, ctx);
            assertTrue(opt.isPresent());
            assertEquals(0, opt.get());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void volumeZeroBarsNoCrash() {
        com.arbergashi.charts.render.financial.VolumeRenderer r = new com.arbergashi.charts.render.financial.VolumeRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 1, 2};
            final double[] ys = {0, 0, 0};

            @Override
            public String getName() {
                return "vol_zero";
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
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 320, 120);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(640, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void heikinAshiTinySeriesNoCrash() {
        com.arbergashi.charts.render.financial.HeikinAshiRenderer r = new com.arbergashi.charts.render.financial.HeikinAshiRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0, 1.0};
            final double[] opens = {1.0, 1.0};
            final double[] closes = {1.0, 1.0};
            final double[] highs = {1.0, 1.0};
            final double[] lows = {1.0, 1.0};

            @Override
            public String getName() {
                return "heikin_small";
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
            public double[] getOpenData() {
                return opens;
            }

            @Override
            public double[] getHighData() {
                return highs;
            }

            @Override
            public double[] getLowData() {
                return lows;
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 320, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(640, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

}
