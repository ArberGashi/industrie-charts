package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.ChartModel;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StatisticalDeepTests {

    @Test
    public void qqPlotEdgeCasesSymmetric() {
        com.arbergashi.charts.render.statistical.QQPlotRenderer r = new com.arbergashi.charts.render.statistical.QQPlotRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {-1.0, 0.0, 1.0};
            final double[] ys = {-2.0, 0.0, 2.0};

            @Override
            public String getName() {
                return "qq_sym";
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
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 320, 240);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void boxplotZeroIqrHitTest() {
        com.arbergashi.charts.render.statistical.BoxPlotRenderer r = new com.arbergashi.charts.render.statistical.BoxPlotRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0};
            final double[] med = {2.0};
            final double[] q1 = {2.0};
            final double[] q3 = {2.0};

            @Override
            public String getName() {
                return "box_iqr0";
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
                return med[index];
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 300, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(600, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            // Ask for a point near the median and expect a possible hit or stable behaviour (no exception)
            Point2D probe = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
            var hit = r.getPointAt(probe, model, ctx);
            assertNotNull(hit);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void histogramSparseBinsNoCrash() {
        com.arbergashi.charts.render.statistical.HistogramRenderer r = new com.arbergashi.charts.render.statistical.HistogramRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 100, 200};

            @Override
            public String getName() {
                return "hist_sparse";
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
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 600, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(800, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

}
