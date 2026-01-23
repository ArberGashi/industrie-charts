package com.arbergashi.charts.quality;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.ChartPoint;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.financial.CandlestickRenderer;
import com.arbergashi.charts.render.specialized.HeatmapRenderer;
import com.arbergashi.charts.render.standard.AreaRenderer;
import com.arbergashi.charts.render.standard.BarRenderer;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.render.standard.ScatterRenderer;
import com.arbergashi.charts.render.statistical.HistogramRenderer;
import com.arbergashi.charts.render.circular.PieRenderer;
import com.arbergashi.charts.util.ChartEngine;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

class VisualRegressionReportTest {

    @Test
    void renderHashesForBaselineReview() {
        ChartTheme theme = ChartThemes.defaultLight();
        Rectangle2D bounds = new Rectangle2D.Double(0, 0, 900, 600);

        List<Sample> samples = new ArrayList<>();
        samples.add(sample("line", new LineRenderer(), buildWaveModel("Line", 400), bounds, theme));
        samples.add(sample("area", new AreaRenderer(), buildWaveModel("Area", 400), bounds, theme));
        samples.add(sample("bar", new BarRenderer(), buildBarModel("Bar", 40), bounds, theme));
        samples.add(sample("scatter", new ScatterRenderer(), buildScatterModel("Scatter", 250), bounds, theme));
        samples.add(sample("candlestick", new CandlestickRenderer(), buildOhlcModel(120), bounds, theme));
        samples.add(sample("histogram", new HistogramRenderer(), buildHistogramModel(500), bounds, theme));
        samples.add(sample("pie", new PieRenderer(), buildPieModel(7), bounds, theme));
        samples.add(sample("heatmap", buildHeatmapRenderer(), new DefaultChartModel("Heatmap"), bounds, theme));

        for (Sample s : samples) {
            String hash = renderAndHash(s, theme);
            System.out.println("VISUAL_HASH " + s.name + " " + hash + " " + s.width + "x" + s.height);
        }
    }

    private static Sample sample(String name, BaseRenderer renderer, ChartModel model, Rectangle2D bounds, ChartTheme theme) {
        renderer.setTheme(theme);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN, theme);
        return new Sample(name, renderer, model, ctx, (int) bounds.getWidth(), (int) bounds.getHeight());
    }

    private static String renderAndHash(Sample s, ChartTheme theme) {
        BufferedImage img = new BufferedImage(s.width, s.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            ChartEngine.prepareGraphics(g2, false);
            g2.setColor(theme.getBackground());
            g2.fillRect(0, 0, s.width, s.height);
            s.renderer.render(g2, s.model, s.context);
        } finally {
            g2.dispose();
        }
        return hashImage(img);
    }

    private static String hashImage(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] pixels = img.getRGB(0, 0, w, h, null, 0, w);
        byte[] bytes = new byte[pixels.length * 4];
        int idx = 0;
        for (int p : pixels) {
            bytes[idx++] = (byte) (p >>> 24);
            bytes[idx++] = (byte) (p >>> 16);
            bytes[idx++] = (byte) (p >>> 8);
            bytes[idx++] = (byte) p;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Hashing failed: " + e.getMessage(), e);
        }
    }

    private static ChartModel buildWaveModel(String name, int points) {
        DefaultChartModel model = new DefaultChartModel(name);
        for (int i = 0; i < points; i++) {
            double x = i;
            double y = Math.sin(i * 0.08) * 12 + Math.cos(i * 0.03) * 5;
            model.addXY(x, y);
        }
        return model;
    }

    private static ChartModel buildBarModel(String name, int points) {
        DefaultChartModel model = new DefaultChartModel(name);
        for (int i = 0; i < points; i++) {
            double x = i;
            double y = (i % 2 == 0 ? 1 : -1) * (3 + (i % 7));
            model.addXY(x, y);
        }
        return model;
    }

    private static ChartModel buildScatterModel(String name, int points) {
        DefaultChartModel model = new DefaultChartModel(name);
        for (int i = 0; i < points; i++) {
            double x = (i % 25) + (i * 0.03);
            double y = ((i * 7) % 40) - 10;
            model.addXY(x, y);
        }
        return model;
    }

    private static ChartModel buildHistogramModel(int points) {
        DefaultChartModel model = new DefaultChartModel("Histogram");
        for (int i = 0; i < points; i++) {
            double x = Math.sin(i * 0.14) * 8 + Math.cos(i * 0.07) * 5;
            model.addXY(x, 0.0);
        }
        return model;
    }

    private static ChartModel buildPieModel(int slices) {
        DefaultChartModel model = new DefaultChartModel("Pie");
        for (int i = 0; i < slices; i++) {
            double weight = 3 + (i % 5);
            String label = "S" + (i + 1);
            model.addPoint(new ChartPoint(i, weight, weight, weight, weight, label));
        }
        return model;
    }

    private static ChartModel buildOhlcModel(int points) {
        DefaultChartModel model = new DefaultChartModel("OHLC");
        double price = 100.0;
        for (int i = 0; i < points; i++) {
            double open = price;
            double high = open + 2 + (i % 4);
            double low = open - 2 - (i % 3);
            double close = open + ((i % 2 == 0) ? 1.2 : -1.1);
            price = close;
            model.addOHLC(i, open, high, low, close);
        }
        return model;
    }

    private static HeatmapRenderer buildHeatmapRenderer() {
        HeatmapRenderer renderer = new HeatmapRenderer();
        int size = 32;
        double[][] grid = new double[size][size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x - size / 2.0;
                double dy = y - size / 2.0;
                grid[y][x] = Math.exp(-(dx * dx + dy * dy) / 140.0);
            }
        }
        renderer.setGridData(grid, 0, size, 0, size);
        return renderer;
    }

    private record Sample(String name, BaseRenderer renderer, ChartModel model, PlotContext context, int width, int height) {
    }
}
