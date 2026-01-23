package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.specialized.DelaunayRenderer;
import com.arbergashi.charts.render.specialized.HexbinRenderer;
import com.arbergashi.charts.render.specialized.SpectrogramRenderer;
import com.arbergashi.charts.render.specialized.VoronoiRenderer;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpecializedMoreRenderersTest {

    @Test
    public void voronoiRendererSmoke() {
        ChartModel model = new ChartModel() {
            final double[] xs = {10.0, 20.0, 30.0, 40.0, 50.0};
            final double[] ys = {10.0, 25.0, 20.0, 30.0, 15.0};

            @Override
            public String getName() {
                return "voronoi_test";
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

        VoronoiRenderer r = new VoronoiRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            assertEquals("voronoi", r.getName());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void hexbinRendererHitTest() {
        ChartModel model = new ChartModel() {
            final double[] xs = {1.0, 1.5, 2.0};
            final double[] ys = {1.0, 1.1, 1.05};

            @Override
            public String getName() {
                return "hexbin_test";
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

        HexbinRenderer r = new HexbinRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 300, 150);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);

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
    public void delaunayRendererSmoke() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0, 10.0, 20.0, 30.0};
            final double[] ys = {0.0, 10.0, 5.0, 15.0};

            @Override
            public String getName() {
                return "delaunay_test";
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

        DelaunayRenderer r = new DelaunayRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            assertEquals("delaunay", r.getName());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void spectrogramRendererSmoke() {
        ChartModel model = new ChartModel() {
            final double[] xs = new double[200];
            final double[] ys = new double[200];

            {
                for (int i = 0; i < 200; i++) {
                    xs[i] = i;
                    ys[i] = Math.sin(i * 0.1) * 0.5 + 0.5;
                }
            }

            @Override
            public String getName() {
                return "spectrogram_test";
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

        SpectrogramRenderer r = new SpectrogramRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(0, 0, 320, 120);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            assertEquals("spectrogram", r.getName());
        } finally {
            g2.dispose();
        }
    }
}
