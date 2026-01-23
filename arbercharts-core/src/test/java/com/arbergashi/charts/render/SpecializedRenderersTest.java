package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.DefaultPlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.specialized.HeatmapRenderer;
import com.arbergashi.charts.render.specialized.LollipopRenderer;
import com.arbergashi.charts.render.specialized.SunburstRenderer;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class SpecializedRenderersTest {

    @Test
    public void heatmapRendererGridModeAndTooltip() {
        HeatmapRenderer r = new HeatmapRenderer();
        double[][] grid = new double[4][4];
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) grid[i][j] = i * 4 + j + 1;
        // set grid spanning x=[0..4), y=[0..4)
        r.setGridData(grid, 0.0, 4.0, 0.0, 4.0);

        ChartModel model = new ChartModel() {
            final double[] xs = {1.0, 2.0};
            final double[] ys = {1.0, 2.0};

            @Override
            public String getName() {
                return "heat_test";
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

        Rectangle2D.Double bounds = new Rectangle2D.Double(10, 10, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            // tooltip for first point should be non-null and formatted
            String tip = r.getTooltipText(0, model);
            assertNotNull(tip);
            assertTrue(tip.startsWith("("));
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void lollipopRendererBasicRender() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0, 1.0, 2.0};
            final double[] ys = {3.0, 2.0, 4.0};

            @Override
            public String getName() {
                return "lolly";
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

        LollipopRenderer r = new LollipopRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(10, 10, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            assertEquals("lollipop", r.getName());
        } finally {
            g2.dispose();
        }
    }

    @Test
    public void sunburstRendererBasicRender() {
        ChartModel model = new ChartModel() {
            final double[] xs = {0.0, 1.0, 2.0};
            final double[] ys = {4.0, 3.0, 5.0};
            final String[] labels = {"root/a", "root/b", "root/a/c"};

            @Override
            public String getName() {
                return "sun";
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

        SunburstRenderer r = new SunburstRenderer();
        Rectangle2D.Double bounds = new Rectangle2D.Double(10, 10, 400, 200);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        BufferedImage bi = new BufferedImage(512, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        try {
            r.render(g2, model, ctx);
            assertEquals("sunburst", r.getName());
        } finally {
            g2.dispose();
        }
    }
}
