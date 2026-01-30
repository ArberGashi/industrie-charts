package com.arbergashi.charts.quality;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartRenderHints;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.BoxPlotOutlierModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.financial.CandlestickRenderer;
import com.arbergashi.charts.render.statistical.BoxPlotRenderer;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.render.standard.ScatterRenderer;
import com.arbergashi.charts.render.statistical.HistogramRenderer;
import com.arbergashi.charts.util.ChartEngine;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

class PerformanceBaselineReportTest {

    @Test
    void renderBaselineReport() {
        ChartTheme theme = ChartThemes.getLightTheme();
        Rectangle2D bounds = new Rectangle2D.Double(0, 0, 900, 600);

        ChartModel lineModel = buildWaveModel(2000);
        ChartModel scatterModel = buildScatterModel(2000);
        ChartModel histModel = buildHistogramModel(2000);
        ChartModel ohlcModel = buildOhlcModel(600);
        ChartModel boxModel = buildBoxPlotOutlierModel(200);

        List<Sample> samples = List.of(
                sample("line", new LineRenderer(), lineModel, bounds, theme),
                sample("scatter", new ScatterRenderer(), scatterModel, bounds, theme),
                sample("histogram", new HistogramRenderer(), histModel, bounds, theme),
                sample("candlestick", new CandlestickRenderer(), ohlcModel, bounds, theme),
                sample("boxplot", new BoxPlotRenderer(), boxModel, bounds, theme)
        );

        for (Sample s : samples) {
            long avgNs = renderAvgNanos(s, theme, 10, 30);
            double ms = avgNs / 1_000_000.0;
            System.out.println("PERF_BASELINE " + s.name + " avg_ms=" + String.format("%.3f", ms) + " frames=30");
        }
    }

    private static Sample sample(String name, BaseRenderer renderer, ChartModel model, Rectangle2D bounds, ChartTheme theme) {
        renderer.setTheme(theme);
        ChartRenderHints hints = new ChartRenderHints().setStrokeWidth(1.6f);
        PlotContext ctx = new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN, theme, hints);
        return new Sample(name, renderer, model, ctx, (int) bounds.getWidth(), (int) bounds.getHeight());
    }

    private static long renderAvgNanos(Sample s, ChartTheme theme, int warmup, int iterations) {
        BufferedImage img = new BufferedImage(s.width, s.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            ChartEngine.prepareGraphics(g2, false);
            for (int i = 0; i < warmup; i++) {
                g2.setColor(theme.getBackground());
                g2.fillRect(0, 0, s.width, s.height);
                s.renderer.render(g2, s.model, s.context);
            }
            long total = 0;
            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                g2.setColor(theme.getBackground());
                g2.fillRect(0, 0, s.width, s.height);
                s.renderer.render(g2, s.model, s.context);
                total += System.nanoTime() - start;
            }
            return total / Math.max(1, iterations);
        } finally {
            g2.dispose();
        }
    }

    private static ChartModel buildWaveModel(int points) {
        DefaultChartModel model = new DefaultChartModel("LinePerf");
        for (int i = 0; i < points; i++) {
            double x = i;
            double y = Math.sin(i * 0.02) * 18 + Math.cos(i * 0.05) * 6;
            model.setXY(x, y);
        }
        return model;
    }

    private static ChartModel buildScatterModel(int points) {
        DefaultChartModel model = new DefaultChartModel("ScatterPerf");
        for (int i = 0; i < points; i++) {
            double x = (i % 100) + (i * 0.01);
            double y = ((i * 11) % 80) - 20;
            model.setXY(x, y);
        }
        return model;
    }

    private static ChartModel buildHistogramModel(int points) {
        DefaultChartModel model = new DefaultChartModel("HistogramPerf");
        for (int i = 0; i < points; i++) {
            double x = Math.sin(i * 0.07) * 10 + Math.cos(i * 0.03) * 7;
            model.setXY(x, 0.0);
        }
        return model;
    }

    private static ChartModel buildOhlcModel(int points) {
        DefaultChartModel model = new DefaultChartModel("OhlcPerf");
        double price = 120.0;
        for (int i = 0; i < points; i++) {
            double open = price;
            double high = open + 2 + (i % 5);
            double low = open - 2 - (i % 4);
            double close = open + ((i % 2 == 0) ? 1.0 : -1.3);
            price = close;
            model.setOHLC(i, open, high, low, close);
        }
        return model;
    }

    private static ChartModel buildBoxPlotOutlierModel(int points) {
        BoxPlotPerfModel model = new BoxPlotPerfModel("BoxPlotPerf");
        for (int i = 0; i < points; i++) {
            double median = 25 + (i % 7);
            double iqr = 5 + (i % 3);
            double q1 = median - iqr / 2.0;
            double q3 = median + iqr / 2.0;
            double min = q1 - 1.5 * iqr;
            double max = q3 + 1.5 * iqr;
            double[] outliers = (i % 3 == 0) ? new double[]{max + 6.0, min - 4.0} : new double[]{max + 3.0};
            model.addBoxPlot(i, median, min, max, iqr, "p" + i, outliers);
        }
        return model;
    }

    private record Sample(String name, BaseRenderer renderer, ChartModel model, PlotContext context, int width, int height) {
    }

    private static final class BoxPlotPerfModel extends DefaultChartModel implements BoxPlotOutlierModel {
        private double[][] outliers = new double[64][];

        BoxPlotPerfModel(String name) {
            super(name);
        }

        void addBoxPlot(double x, double median, double min, double max, double iqr, String label, double[] outlierValues) {
            setPoint(x, median, min, max, iqr, label);
            int idx = getPointCount() - 1;
            ensureCapacity(idx + 1);
            outliers[idx] = outlierValues;
        }

        @Override
        public double[] getOutliers(int index) {
            if (index < 0 || index >= getPointCount()) return EMPTY_DOUBLE;
            double[] vals = outliers[index];
            return vals != null ? vals : EMPTY_DOUBLE;
        }

        private void ensureCapacity(int size) {
            if (outliers.length >= size) return;
            double[][] next = new double[Math.max(size, outliers.length * 2)][];
            System.arraycopy(outliers, 0, next, 0, outliers.length);
            outliers = next;
        }
    }
}
