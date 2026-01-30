package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.BoxPlotOutlierModel;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;


public class StatisticalEdgeCasesTest {

    @Test
    public void qqPlotIdenticalValuesNoCrash() {
        com.arbergashi.charts.render.statistical.QQPlotRenderer r = new com.arbergashi.charts.render.statistical.QQPlotRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {1.0, 1.0, 1.0, 1.0};

            @Override
            public String getName() {
                return "qq_identical";
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
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 400, 300);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void histogramZeroWidthBins() {
        com.arbergashi.charts.render.statistical.HistogramRenderer r = new com.arbergashi.charts.render.statistical.HistogramRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {5.0, 5.0, 5.0};

            @Override
            public String getName() {
                return "hist_zero";
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

    @Test
    public void boxplotNegativeValuesAndOutlierHandling() {
        com.arbergashi.charts.render.statistical.BoxPlotRenderer r = new com.arbergashi.charts.render.statistical.BoxPlotRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 1, 2};
            final double[] med = {0.0, -1.0, 1000.0}; // includes negative and extreme outlier

            @Override
            public String getName() {
                return "box_neg_outlier";
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
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 480, 300);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(480, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void violinTinySampleNoCrash() {
        com.arbergashi.charts.render.statistical.ViolinPlotRenderer r = new com.arbergashi.charts.render.statistical.ViolinPlotRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0};
            final double[] ys = {2.0};

            @Override
            public String getName() {
                return "violin_tiny";
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
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 200, 120);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(400, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void kdeTinySampleNoCrash() {
        com.arbergashi.charts.render.statistical.KDERenderer r = new com.arbergashi.charts.render.statistical.KDERenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {1.0, 2.0};

            @Override
            public String getName() {
                return "kde_tiny";
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

    @Test
    public void boxplotOutliersRenderNoCrash() {
        com.arbergashi.charts.render.statistical.BoxPlotRenderer r = new com.arbergashi.charts.render.statistical.BoxPlotRenderer();
        BoxPlotOutlierModel model = new BoxPlotOutlierModel() {
            final double[] xs = {0.0, 1.0};
            final double[] med = {10.0, 12.0};
            final double[] mins = {7.0, 9.0};
            final double[] maxs = {14.0, 15.0};
            final double[] iqrs = {4.0, 3.0};
            final double[][] outliers = {
                    {2.5, 18.0},
                    {5.5}
            };

            @Override
            public String getName() {
                return "box_outlier";
            }

            @Override
            public int getPointCount() {
                return xs.length;
            }

            @Override
            public double getX(int index) {
                return xs[index];
            }

            @Override
            public double getY(int index) {
                return med[index];
            }

            @Override
            public double getValue(int index, int component) {
                return switch (component) {
                    case 2 -> iqrs[index];
                    case 3 -> mins[index];
                    case 4 -> maxs[index];
                    default -> med[index];
                };
            }

            @Override
            public double[] getOutliers(int index) {
                return outliers[index];
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
        BufferedImage bi = new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

}
