package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.HierarchicalChartModel;
import com.arbergashi.charts.model.MatrixChartModel;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CircularEdgeCasesTest {

    @Test
    public void pieHitTestBasic() {
        com.arbergashi.charts.render.circular.PieRenderer r = new com.arbergashi.charts.render.circular.PieRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 0};
            final double[] ys = {1, 1};
            final double[] w = {1, 1};
            final String[] labels = {"A", "B"};

            @Override
            public String getName() {
                return "pie";
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
            public double getWeight(int index) {
                return w[index];
            }

            @Override
            public String getLabel(int index) {
                return labels[index];
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 240, 240);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            double diameter = Math.min(bounds.getWidth(), bounds.getHeight()) * 0.8;
            double cx = bounds.getCenterX();
            double cy = bounds.getCenterY();
            double outerR = diameter * 0.5;
            Point2D p = new Point2D.Double(cx + outerR * 0.5, cy);
            var hit = r.getPointAt(p, model, ctx);
            assertTrue(hit.isPresent());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void donutHoleRejectsCenter() {
        com.arbergashi.charts.render.circular.DonutRenderer r = new com.arbergashi.charts.render.circular.DonutRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0};
            final double[] ys = {1};
            final double[] w = {1};

            @Override
            public String getName() {
                return "don";
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
            public double getWeight(int index) {
                return w[index];
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 240, 240);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            double diameter = Math.min(bounds.getWidth(), bounds.getHeight()) * 0.8;
            double cx = bounds.getCenterX();
            double cy = bounds.getCenterY();
            Point2D center = new Point2D.Double(cx, cy);
            var hit = r.getPointAt(center, model, ctx);
            assertTrue(hit.isEmpty());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void semiDonutHitTest() {
        com.arbergashi.charts.render.circular.SemiDonutRenderer r = new com.arbergashi.charts.render.circular.SemiDonutRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0, 1};
            final double[] ys = {1, 1};
            final double[] w = {1, 1};
            final String[] labels = {"L1", "L2"};

            @Override
            public String getName() {
                return "semi";
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
            public double getWeight(int index) {
                return w[index];
            }

            @Override
            public String getLabel(int index) {
                return labels[index];
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
            r.render(g2, model, ctx);
            double diameter = Math.min(bounds.getWidth(), bounds.getHeight()) * 0.95;
            double cx = bounds.getCenterX();
            double cy = bounds.getCenterY() + diameter * 0.10; // same formula as renderer
            double outerR = diameter * 0.5;
            double innerR = outerR * 0.60; // match INNER_FACTOR in renderer
            double radial = (innerR + outerR) * 0.5; // mid between inner and outer
            Point2D probe = new Point2D.Double(cx, cy - radial);
            var hit = r.getPointAt(probe, model, ctx);
            assertTrue(hit.isPresent(), "probe should hit a semi-donut segment but did not");
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void chordDiagramSmallMatrix() {
        com.arbergashi.charts.render.circular.ChordDiagramRenderer r = new com.arbergashi.charts.render.circular.ChordDiagramRenderer();
        MatrixChartModel model = new MatrixChartModel() {
            @Override
            public String getName() {
                return "chord";
            }

            @Override
            public int getPointCount() {
                return 2;
            }

            @Override
            public double[] getXData() {
                return new double[0];
            }

            @Override
            public double[] getYData() {
                return new double[0];
            }

            @Override
            public double[][] getMatrix() {
                return new double[][]{{0, 1}, {1, 0}};
            }

            @Override
            public java.util.List<String> getEntityLabels() {
                return java.util.List.of("A", "B");
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 300, 300);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void sunburstHierarchicalSmall() {
        com.arbergashi.charts.render.circular.SunburstRenderer r = new com.arbergashi.charts.render.circular.SunburstRenderer();
        HierarchicalChartModel<Object> model = new HierarchicalChartModel<>() {
            final HierarchicalChartModel.Node<Object> child = new HierarchicalChartModel.Node<>() {
                @Override
                public String getLabel() {
                    return "c";
                }

                @Override
                public double getValue() {
                    return 1.0;
                }

                @Override
                public java.util.List<HierarchicalChartModel.Node<Object>> getChildren() {
                    return java.util.List.of();
                }
            };
            final HierarchicalChartModel.Node<Object> root = new HierarchicalChartModel.Node<>() {
                @Override
                public String getLabel() {
                    return "r";
                }

                @Override
                public double getValue() {
                    return 1.0;
                }

                @Override
                public java.util.List<HierarchicalChartModel.Node<Object>> getChildren() {
                    return java.util.List.of(child);
                }
            };

            @Override
            public String getName() {
                return "sun";
            }

            @Override
            public int getPointCount() {
                return 1;
            }

            @Override
            public double[] getXData() {
                return new double[0];
            }

            @Override
            public double[] getYData() {
                return new double[0];
            }

            @Override
            public HierarchicalChartModel.Node<Object> getRootNode() {
                return root;
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 320, 320);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(320, 320, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void radarTinySeries() {
        com.arbergashi.charts.render.circular.RadarRenderer r = new com.arbergashi.charts.render.circular.RadarRenderer();
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0, 1.0, 2.0};
            final double[] ys = {1.0, 2.0, 1.5};

            @Override
            public String getName() {
                return "rad";
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
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 320, 320);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(320, 320, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
        } finally {
            g2.dispose();
        }
    }
}
