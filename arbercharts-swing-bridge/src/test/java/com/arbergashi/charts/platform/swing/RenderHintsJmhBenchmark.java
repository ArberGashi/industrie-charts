package com.arbergashi.charts.platform.swing;

import com.arbergashi.charts.api.AxisConfig;
import com.arbergashi.charts.api.ChartRenderHints;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.bridge.swing.AwtCanvasAdapter;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.BoxPlotOutlierModel;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.grid.DefaultGridLayer;
import com.arbergashi.charts.render.grid.GridLayer;
import com.arbergashi.charts.render.statistical.BoxPlotRenderer;
import com.arbergashi.charts.render.statistical.HistogramRenderer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

@State(Scope.Thread)
public class RenderHintsJmhBenchmark {

    @Param({"true", "false"})
    public boolean aaEnabled;

    private BufferedImage image;
    private Graphics2D g2;
    private ArberCanvas canvas;
    private ArberCanvas noopCanvas;
    private PlotContext histogramContextAaOn;
    private PlotContext histogramContextAaOff;
    private PlotContext boxPlotContextAaOn;
    private PlotContext boxPlotContextAaOff;
    private HistogramRenderer histogramRenderer;
    private BoxPlotRenderer boxPlotRenderer;
    private DefaultChartModel histogramModel;
    private BoxPlotPerfModel boxPlotModel;

    private ArberChartPanel axisPanel;
    private Rectangle2D axisBounds;
    private GridLayer gridLayer;
    private PlotContext gridContext;
    private DefaultChartModel gridModel;

    @Setup(Level.Trial)
    public void setup() {
        image = new BufferedImage(800, 500, BufferedImage.TYPE_INT_ARGB);
        g2 = image.createGraphics();
        canvas = new AwtCanvasAdapter(g2);
        noopCanvas = new NoOpCanvas();

        ChartRenderHints aaOn = new ChartRenderHints().setAntialiasing(true).setStrokeWidth(1.6f);
        ChartRenderHints aaOff = new ChartRenderHints().setAntialiasing(false).setStrokeWidth(1.6f);
        ArberRect bounds = new ArberRect(0, 0, 800, 500);

        histogramRenderer = new HistogramRenderer();
        histogramModel = new DefaultChartModel("hist");
        for (int i = 0; i < 2000; i++) {
            double x = Math.sin(i * 0.07) * 10 + Math.cos(i * 0.03) * 7;
            histogramModel.setXY(x, 0.0);
        }
        histogramContextAaOn = new DefaultPlotContext(bounds, histogramModel, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                ChartThemes.getDarkTheme(), aaOn);
        histogramContextAaOff = new DefaultPlotContext(bounds, histogramModel, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                ChartThemes.getDarkTheme(), aaOff);

        boxPlotRenderer = new BoxPlotRenderer();
        boxPlotModel = new BoxPlotPerfModel("box");
        for (int i = 0; i < 300; i++) {
            double median = 25 + (i % 7);
            double iqr = 5 + (i % 3);
            double min = median - 2.0 * iqr;
            double max = median + 2.0 * iqr;
            double[] outliers = (i % 2 == 0) ? new double[]{max + 5.0, min - 4.0} : new double[]{max + 3.0};
            boxPlotModel.addBoxPlot(i, median, min, max, iqr, "p" + i, outliers);
        }
        boxPlotContextAaOn = new DefaultPlotContext(bounds, boxPlotModel, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                ChartThemes.getDarkTheme(), aaOn);
        boxPlotContextAaOff = new DefaultPlotContext(bounds, boxPlotModel, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                ChartThemes.getDarkTheme(), aaOff);

        axisBounds = new Rectangle2D.Double(0, 0, 900, 300);
        axisPanel = new ArberChartPanel(new StubModel(), new StubRenderer());
        AxisConfig xCfg = new AxisConfig().setUnitsPerPixel(0.04);
        AxisConfig yCfg = new AxisConfig().setUnitsPerPixel(0.01);
        axisPanel.setXAxisConfig(xCfg);
        axisPanel.setYAxisConfig(yCfg);

        gridLayer = new DefaultGridLayer();
        gridModel = new DefaultChartModel("grid");
        for (int i = 0; i < 600; i++) {
            gridModel.setXY(i, Math.sin(i * 0.15) * 50.0);
        }
        gridContext = new DefaultPlotContext(bounds, gridModel, 0, 600, -60, 60,
                ChartThemes.getDarkTheme(), aaOn);

        histogramRenderer.render(canvas, histogramModel, histogramContextAaOn);
        boxPlotRenderer.render(canvas, boxPlotModel, boxPlotContextAaOn);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (g2 != null) {
            g2.dispose();
        }
    }

    @Benchmark
    public void histogramRenderWithTouchedBins() {
        PlotContext ctx = aaEnabled ? histogramContextAaOn : histogramContextAaOff;
        histogramRenderer.render(canvas, histogramModel, ctx);
    }

    @Benchmark
    public void boxPlotOutlierRendering() {
        PlotContext ctx = aaEnabled ? boxPlotContextAaOn : boxPlotContextAaOff;
        boxPlotRenderer.render(canvas, boxPlotModel, ctx);
    }

    @Benchmark
    public void histogramRenderNoOp() {
        PlotContext ctx = aaEnabled ? histogramContextAaOn : histogramContextAaOff;
        histogramRenderer.render(noopCanvas, histogramModel, ctx);
    }

    @Benchmark
    public void boxPlotOutlierRenderingNoOp() {
        PlotContext ctx = aaEnabled ? boxPlotContextAaOn : boxPlotContextAaOff;
        boxPlotRenderer.render(noopCanvas, boxPlotModel, ctx);
    }

    @Benchmark
    public void axisScalingFixedScale() {
        axisPanel.applyAxisOverrides(axisBounds);
    }

    @Benchmark
    public void gridRenderingExtreme() {
        gridLayer.renderGrid(canvas, gridContext);
    }

    @Benchmark
    public void gridRenderingExtremeNoOp() {
        gridLayer.renderGrid(noopCanvas, gridContext);
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

    private static final class StubModel implements ChartModel {
        @Override
        public String getName() {
            return "stub";
        }

        @Override
        public int getPointCount() {
            return 1;
        }

        @Override
        public void setChangeListener(ChartModel.ChartModelListener listener) {
        }

        @Override
        public void removeChangeListener(ChartModel.ChartModelListener listener) {
        }
    }

    private static final class StubRenderer extends BaseRenderer {
        StubRenderer() {
            super("stub");
        }

        @Override
        protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        }
    }

    private static final class NoOpCanvas implements ArberCanvas {
        @Override public void setColor(com.arbergashi.charts.api.types.ArberColor color) { }
        @Override public void setStroke(float width) { }
        @Override public void moveTo(float x, float y) { }
        @Override public void lineTo(float x, float y) { }
        @Override public void drawPolyline(float[] xs, float[] ys, int count) { }
        @Override public void drawRect(float x, float y, float w, float h) { }
        @Override public void fillRect(float x, float y, float w, float h) { }
        @Override public void fillPolygon(float[] xs, float[] ys, int count) { }
        @Override public void drawVoxelField(com.arbergashi.charts.core.rendering.VoxelBuffer buffer) { }
        @Override public com.arbergashi.charts.core.rendering.ArberMatrix getTransform() { return com.arbergashi.charts.core.rendering.ArberMatrices.identity(); }
        @Override public void setClip(com.arbergashi.charts.core.geometry.ArberRect clip) { }
        @Override public com.arbergashi.charts.core.geometry.ArberRect getClip() { return null; }
    }
}
