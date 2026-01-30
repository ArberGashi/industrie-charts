package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.statistical.ECDFRenderer;
import com.arbergashi.charts.render.statistical.ErrorBarRenderer;
import com.arbergashi.charts.render.statistical.QQPlotRenderer;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StatisticalRenderersTest {

    @Test
    public void qqPlotRendererSmoke() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0, 1.0, 2.0, 3.0, 4.0};
            final double[] ys = {1.0, 2.1, 1.8, 3.2, 2.5};

            @Override
            public String getName() {
                return "qq";
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

        QQPlotRenderer r = new QQPlotRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(10, 10, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            assertEquals("qqplot", r.getName());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void ecdfRendererSmoke() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0, 1.0, 2.0, 3.0};
            final double[] ys = {0.1, 0.2, 0.3, 0.8};

            @Override
            public String getName() {
                return "ecdf_test";
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

        ECDFRenderer r = new ECDFRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(10, 10, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            assertEquals("ecdf", r.getName());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void errorBarRendererHitTest() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0, 1.0, 2.0};
            final double[] ys = {5.0, 6.0, 7.0};
            final double[] highs = {6.0, 7.5, 8.0};
            final double[] lows = {4.0, 5.0, 6.0};

            @Override
            public String getName() {
                return "err";
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
            public double[] getHighData() {
                return highs;
            }

            @Override
            public double[] getLowData() {
                return lows;
            }

            @Override
            public String getLabel(int index) {
                return "" + index;
            }

            @Override
            public void setChangeListener(ChartModel.ChartModelListener listener) {
            }

            @Override
            public void removeChangeListener(ChartModel.ChartModelListener listener) {
            }
        };

        ErrorBarRenderer r = new ErrorBarRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(10, 10, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            // map the middle of the second bar and perform hit test
            double[] buf = new double[2];
            double midY = (model.getHighData()[1] + model.getLowData()[1]) / 2.0;
            ctx.mapToPixel(model.getXData()[1], midY, buf);
            Optional<Integer> hit = r.getPointAt(new Point2D.Double(buf[0], buf[1]), model, ctx);
            assertTrue(hit.isPresent());
            assertEquals(1, hit.get());
        } finally {
            g2.dispose();
        }
    }
}
