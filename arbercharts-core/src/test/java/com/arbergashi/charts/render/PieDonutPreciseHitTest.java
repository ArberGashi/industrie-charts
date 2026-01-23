package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.ChartModel;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PieDonutPreciseHitTest {

    @Test
    public void pieHitReturnsCorrectIndex() {
        com.arbergashi.charts.render.circular.PieRenderer r = new com.arbergashi.charts.render.circular.PieRenderer();
        ChartModel m = new ChartModel() {
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
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 200, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, m, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        var g2 = bi.createGraphics();
        try {
            r.render(g2, m, ctx);
            double total = 2.0;
            double value = 1.0;
            double angle = 360.0 * (value / total); // 180
            double startAngle = 90.0;
            double midAngleRad = Math.toRadians(startAngle - (angle / 2.0));
            double diameter = Math.min(bounds.width, bounds.height) * 0.8;
            double outerR = diameter * 0.5;
            double probeR = outerR * 0.75; // inside the slice
            double cx = bounds.getCenterX();
            double cy = bounds.getCenterY();
            Point2D probe = new Point2D.Double(cx + Math.cos(midAngleRad) * probeR, cy - Math.sin(midAngleRad) * probeR);

            var hit = r.getPointAt(probe, m, ctx);
            assertTrue(hit.isPresent());
            assertEquals(0, hit.get());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void donutHitAvoidsCenterHoleAndReturnsIndex() {
        com.arbergashi.charts.render.circular.DonutRenderer r = new com.arbergashi.charts.render.circular.DonutRenderer();
        ChartModel m = new ChartModel() {
            final double[] xs = {0, 0};
            final double[] ys = {1, 1};
            final double[] w = {1, 1};
            final String[] labels = {"A", "B"};

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
            public String getLabel(int index) {
                return labels[index];
            }

            @Override
            public void addChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 300, 300);
        PlotContext ctx = new DefaultPlotContext(bounds, m, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        BufferedImage bi = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        var g2 = bi.createGraphics();
        try {
            r.render(g2, m, ctx);
            double total = 2.0;
            double value = 1.0;
            double angle = 360.0 * (value / total);
            double startAngle = 90.0;
            double midAngleRad = Math.toRadians(startAngle - (angle / 2.0));
            double diameter = Math.min(bounds.width, bounds.height) * 0.8;
            double outerR = diameter * 0.5;
            double innerHoleR = outerR * 0.4; // donut hole radius
            double probeR = innerHoleR + (outerR - innerHoleR) * 0.5; // mid between hole and outer
            double cx = bounds.getCenterX();
            double cy = bounds.getCenterY();
            Point2D probe = new Point2D.Double(cx + Math.cos(midAngleRad) * probeR, cy - Math.sin(midAngleRad) * probeR);

            var hit = r.getPointAt(probe, m, ctx);
            assertTrue(hit.isPresent());
            assertEquals(0, hit.get());
        } finally {
            g2.dispose();
        }
    }
}
