package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.ChartModel;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class AnalysisEdgeCasesTest {

    @Test
    public void regressionLineZeroVarianceNoCrash() {
        com.arbergashi.charts.render.analysis.RegressionLineRenderer r = new com.arbergashi.charts.render.analysis.RegressionLineRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {1.0, 1.0, 1.0};
            final double[] ys = {2.0, 2.0, 2.0};

            @Override
            public String getName() {
                return "reg_zero";
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
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 300, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(300, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void outlierDetectionEmptyNoCrash() {
        com.arbergashi.charts.render.analysis.OutlierDetectionRenderer r = new com.arbergashi.charts.render.analysis.OutlierDetectionRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {};

            @Override
            public String getName() {
                return "out_empty";
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
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 200, 120);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(200, 120, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void movingAverageTinySeriesHandlesWindow() {
        com.arbergashi.charts.render.analysis.MovingAverageRenderer r = new com.arbergashi.charts.render.analysis.MovingAverageRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 1};
            final double[] ys = {1, 2};

            @Override
            public String getName() {
                return "ma_small";
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
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 300, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(300, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void thresholdBoundaryCrossing() {
        com.arbergashi.charts.render.analysis.ThresholdRenderer r = new com.arbergashi.charts.render.analysis.ThresholdRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 1, 2};
            final double[] ys = {5, 10, 5};

            @Override
            public String getName() {
                return "th";
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
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 300, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(300, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void minMaxMarkerAllEqualNoCrash() {
        com.arbergashi.charts.render.analysis.MinMaxMarkerRenderer r = new com.arbergashi.charts.render.analysis.MinMaxMarkerRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 1, 2};
            final double[] ys = {3, 3, 3};

            @Override
            public String getName() {
                return "mm";
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
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 200, 120);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(200, 120, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }
}
