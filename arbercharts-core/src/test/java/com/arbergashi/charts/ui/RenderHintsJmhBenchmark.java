package com.arbergashi.charts.ui;

import com.arbergashi.charts.api.AxisConfig;
import com.arbergashi.charts.api.ChartRenderHints;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.BoxPlotOutlierModel;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.BaseRenderer;
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
import java.awt.geom.AffineTransform;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.GraphicsConfiguration;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;
import java.awt.font.GlyphVector;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;

@State(Scope.Thread)
public class RenderHintsJmhBenchmark {

    @Param({"true", "false"})
    public boolean aaEnabled;

    private BufferedImage image;
    private Graphics2D g2;
    private PlotContext histogramContextAaOn;
    private PlotContext histogramContextAaOff;
    private PlotContext boxPlotContextAaOn;
    private PlotContext boxPlotContextAaOff;
    private HistogramRenderer histogramRenderer;
    private BoxPlotRenderer boxPlotRenderer;
    private DefaultChartModel histogramModel;
    private BoxPlotPerfModel boxPlotModel;
    private Graphics2D noopGraphics;

    private ArberChartPanel axisPanel;
    private Rectangle2D axisBounds;

    @Setup(Level.Trial)
    public void setup() {
        image = new BufferedImage(800, 500, BufferedImage.TYPE_INT_ARGB);
        g2 = image.createGraphics();

        ChartRenderHints aaOn = new ChartRenderHints().setAntialiasing(true).setStrokeWidth(1.6f);
        ChartRenderHints aaOff = new ChartRenderHints().setAntialiasing(false).setStrokeWidth(1.6f);
        Rectangle2D bounds = new Rectangle2D.Double(0, 0, 800, 500);

        histogramRenderer = new HistogramRenderer();
        histogramModel = new DefaultChartModel("hist");
        for (int i = 0; i < 2000; i++) {
            double x = Math.sin(i * 0.07) * 10 + Math.cos(i * 0.03) * 7;
            histogramModel.addXY(x, 0.0);
        }
        histogramContextAaOn = new DefaultPlotContext(bounds, histogramModel, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                ChartThemes.defaultDark(), aaOn);
        histogramContextAaOff = new DefaultPlotContext(bounds, histogramModel, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                ChartThemes.defaultDark(), aaOff);

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
                ChartThemes.defaultDark(), aaOn);
        boxPlotContextAaOff = new DefaultPlotContext(bounds, boxPlotModel, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                ChartThemes.defaultDark(), aaOff);

        axisBounds = new Rectangle2D.Double(0, 0, 900, 300);
        axisPanel = new ArberChartPanel(new StubModel(), new StubRenderer());
        AxisConfig xCfg = new AxisConfig().setUnitsPerPixel(0.04);
        AxisConfig yCfg = new AxisConfig().setUnitsPerPixel(0.01);
        axisPanel.withXAxisConfig(xCfg);
        axisPanel.withYAxisConfig(yCfg);

        noopGraphics = new NoOpGraphics2D();

        // Prewarm caches to avoid counting one-time allocations in measurements.
        histogramRenderer.render(g2, histogramModel, histogramContextAaOn);
        boxPlotRenderer.render(g2, boxPlotModel, boxPlotContextAaOn);
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
        histogramRenderer.render(g2, histogramModel, ctx);
    }

    @Benchmark
    public void boxPlotOutlierRendering() {
        PlotContext ctx = aaEnabled ? boxPlotContextAaOn : boxPlotContextAaOff;
        boxPlotRenderer.render(g2, boxPlotModel, ctx);
    }

    @Benchmark
    public void histogramRenderNoOp() {
        PlotContext ctx = aaEnabled ? histogramContextAaOn : histogramContextAaOff;
        histogramRenderer.render(noopGraphics, histogramModel, ctx);
    }

    @Benchmark
    public void boxPlotOutlierRenderingNoOp() {
        PlotContext ctx = aaEnabled ? boxPlotContextAaOn : boxPlotContextAaOff;
        boxPlotRenderer.render(noopGraphics, boxPlotModel, ctx);
    }

    @Benchmark
    public void axisScalingFixedScale() {
        axisPanel.applyAxisOverrides(axisBounds);
    }

    private static final class BoxPlotPerfModel extends DefaultChartModel implements BoxPlotOutlierModel {
        private double[][] outliers = new double[64][];

        BoxPlotPerfModel(String name) {
            super(name);
        }

        void addBoxPlot(double x, double median, double min, double max, double iqr, String label, double[] outlierValues) {
            addPoint(x, median, min, max, iqr, label);
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
        public void addChangeListener(ChartModelListener listener) {
        }

        @Override
        public void removeChangeListener(ChartModelListener listener) {
        }
    }

    private static final class StubRenderer extends BaseRenderer {
        StubRenderer() {
            super("stub");
        }

        @Override
        protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        }
    }

    private static final class NoOpGraphics2D extends Graphics2D {
        private static final AffineTransform IDENTITY = new AffineTransform();
        private static final FontRenderContext FRC = new FontRenderContext(IDENTITY, false, false);
        private static final RenderingHints EMPTY_HINTS = new RenderingHints(null);
        private static final Stroke DEFAULT_STROKE = new BasicStroke();
        private static final Composite DEFAULT_COMPOSITE = AlphaComposite.SrcOver;
        private static final Paint DEFAULT_PAINT = Color.BLACK;
        private static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 12);
        private static final Color DEFAULT_COLOR = Color.BLACK;

        @Override
        public void draw(Shape s) {
        }

        @Override
        public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
            return true;
        }

        @Override
        public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        }

        @Override
        public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        }

        @Override
        public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        }

        @Override
        public void drawString(String str, int x, int y) {
        }

        @Override
        public void drawString(String str, float x, float y) {
        }

        @Override
        public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        }

        @Override
        public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        }

        @Override
        public void drawGlyphVector(GlyphVector g, float x, float y) {
        }

        @Override
        public void fill(Shape s) {
        }

        @Override
        public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
            return false;
        }

        @Override
        public GraphicsConfiguration getDeviceConfiguration() {
            return null;
        }

        @Override
        public void setComposite(Composite comp) {
        }

        @Override
        public void setPaint(Paint paint) {
        }

        @Override
        public void setStroke(Stroke s) {
        }

        @Override
        public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        }

        @Override
        public Object getRenderingHint(RenderingHints.Key hintKey) {
            return null;
        }

        @Override
        public void setRenderingHints(Map<?, ?> hints) {
        }

        @Override
        public void addRenderingHints(Map<?, ?> hints) {
        }

        @Override
        public RenderingHints getRenderingHints() {
            return EMPTY_HINTS;
        }

        @Override
        public void translate(int x, int y) {
        }

        @Override
        public void translate(double tx, double ty) {
        }

        @Override
        public void rotate(double theta) {
        }

        @Override
        public void rotate(double theta, double x, double y) {
        }

        @Override
        public void scale(double sx, double sy) {
        }

        @Override
        public void shear(double shx, double shy) {
        }

        @Override
        public void transform(AffineTransform Tx) {
        }

        @Override
        public void setTransform(AffineTransform Tx) {
        }

        @Override
        public AffineTransform getTransform() {
            return IDENTITY;
        }

        @Override
        public Paint getPaint() {
            return DEFAULT_PAINT;
        }

        @Override
        public Composite getComposite() {
            return DEFAULT_COMPOSITE;
        }

        @Override
        public void setBackground(Color color) {
        }

        @Override
        public Color getBackground() {
            return DEFAULT_COLOR;
        }

        @Override
        public Stroke getStroke() {
            return DEFAULT_STROKE;
        }

        @Override
        public void clip(Shape s) {
        }

        @Override
        public FontRenderContext getFontRenderContext() {
            return FRC;
        }

        @Override
        public Graphics create() {
            return this;
        }

        @Override
        public Color getColor() {
            return DEFAULT_COLOR;
        }

        @Override
        public void setColor(Color c) {
        }

        @Override
        public void setPaintMode() {
        }

        @Override
        public void setXORMode(Color c1) {
        }

        @Override
        public Font getFont() {
            return DEFAULT_FONT;
        }

        @Override
        public void setFont(Font font) {
        }

        @Override
        public FontMetrics getFontMetrics(Font f) {
            return null;
        }

        @Override
        public Rectangle getClipBounds() {
            return null;
        }

        @Override
        public void clipRect(int x, int y, int width, int height) {
        }

        @Override
        public void setClip(int x, int y, int width, int height) {
        }

        @Override
        public Shape getClip() {
            return null;
        }

        @Override
        public void setClip(Shape clip) {
        }

        @Override
        public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        }

        @Override
        public void drawLine(int x1, int y1, int x2, int y2) {
        }

        @Override
        public void fillRect(int x, int y, int width, int height) {
        }

        @Override
        public void clearRect(int x, int y, int width, int height) {
        }

        @Override
        public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        }

        @Override
        public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        }

        @Override
        public void drawOval(int x, int y, int width, int height) {
        }

        @Override
        public void fillOval(int x, int y, int width, int height) {
        }

        @Override
        public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        }

        @Override
        public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        }

        @Override
        public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        }

        @Override
        public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        }

        @Override
        public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        }

        @Override
        public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
            return true;
        }

        @Override
        public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
            return true;
        }

        @Override
        public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
            return true;
        }

        @Override
        public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
            return true;
        }

        @Override
        public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
                                 ImageObserver observer) {
            return true;
        }

        @Override
        public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
                                 Color bgcolor, ImageObserver observer) {
            return true;
        }

        @Override
        public void dispose() {
        }
    }
}
