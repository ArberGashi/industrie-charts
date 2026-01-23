package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.ChartModel;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class FinancialExtraEdgeTests {

    @Test
    public void candlestickGapSeriesNoCrash() {
        com.arbergashi.charts.render.financial.CandlestickRenderer r = new com.arbergashi.charts.render.financial.CandlestickRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 10, 20};
            final double[] opens = {10, 0, 5};
            final double[] closes = {12, 0, 5};
            final double[] highs = {13, 0, 6};
            final double[] lows = {9, 0, 4};

            @Override
            public String getName() {
                return "cand_gap";
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
            public void addChangeListener(ChartModel.ChartModelListener listener) {
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
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void volumeSeriesLargeZerosNoCrash() {
        com.arbergashi.charts.render.financial.VolumeRenderer r = new com.arbergashi.charts.render.financial.VolumeRenderer();
        final int n = 400;
        ChartModel model = new ChartModel() {
            final double[] xs = new double[n];
            final double[] ys = new double[n];

            {
                for (int i = 0; i < n; i++) {
                    xs[i] = i;
                    ys[i] = 0;
                }
            }

            @Override
            public String getName() {
                return "vol_zeros";
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
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 800, 300);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(800, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

}
