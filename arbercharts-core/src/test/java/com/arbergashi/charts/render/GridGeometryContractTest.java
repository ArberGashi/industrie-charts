package com.arbergashi.charts.render;

import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberMatrix;
import com.arbergashi.charts.core.rendering.VoxelBuffer;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;
import com.arbergashi.charts.engine.spatial.SpatialStyleDescriptor;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.model.DefaultTernaryChartModel;
import com.arbergashi.charts.model.TernaryChartModel;
import com.arbergashi.charts.render.grid.AnalysisGridLayer;
import com.arbergashi.charts.render.grid.DefaultGridLayer;
import com.arbergashi.charts.render.grid.FinancialGridLayer;
import com.arbergashi.charts.render.grid.GeoGridLayer;
import com.arbergashi.charts.render.grid.GridBatchConfig;
import com.arbergashi.charts.render.grid.GridLayer;
import com.arbergashi.charts.render.grid.IsometricGridLayer;
import com.arbergashi.charts.render.grid.LogarithmicGridLayer;
import com.arbergashi.charts.render.grid.MedicalGridLayer;
import com.arbergashi.charts.render.grid.PolarGridLayer;
import com.arbergashi.charts.render.grid.SmithChartGridLayer;
import com.arbergashi.charts.render.grid.StatisticalGridLayer;
import com.arbergashi.charts.render.grid.TernaryGridLayer;
import com.arbergashi.charts.render.specialized.SmithChartTransform;
import com.arbergashi.charts.render.specialized.TernaryContourRenderer;
import com.arbergashi.charts.render.specialized.TernaryPhasediagramRenderer;
import com.arbergashi.charts.render.specialized.TernaryPlotRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.NiceScale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridGeometryContractTest {
    static Stream<GridLayer> grids() {
        return Stream.of(new DefaultGridLayer(), new MedicalGridLayer(), new FinancialGridLayer(),
                new StatisticalGridLayer(), new AnalysisGridLayer(), new IsometricGridLayer(),
                new SmithChartGridLayer(), new PolarGridLayer(), new TernaryGridLayer(),
                new GeoGridLayer(), new LogarithmicGridLayer());
    }

    static Stream<GridLayer> cartesianGrids() {
        return Stream.of(new DefaultGridLayer(), new MedicalGridLayer(), new FinancialGridLayer(),
                new StatisticalGridLayer(), new AnalysisGridLayer(), new GeoGridLayer(), new LogarithmicGridLayer());
    }

    static Stream<GridLayer> numericTickGrids() {
        return Stream.of(new DefaultGridLayer(), new FinancialGridLayer());
    }

    @ParameterizedTest
    @MethodSource("grids")
    void batchPreservesCanvasGeometryStyleAndExplicitDepthClipping(GridLayer grid) {
        PlotContext context = context(new ArberRect(17, 23, 640, 400));
        RecordingCanvas canvas = new RecordingCanvas();
        grid.renderGrid(canvas, context);
        assertFalse(canvas.lines.isEmpty());
        SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
        for (GridBatchConfig config : Arrays.asList(null, new GridBatchConfig(),
                new GridBatchConfig().setGridZ(-2).setZMin(-3),
                new GridBatchConfig().setGridZ(-2).setZMin(0).setClippingMode(SpatialPathBatchBuilder.ClippingMode.CLAMP))) {
            builder.reset();
            grid.renderGridBatch(builder, context, config);
            assertEquals(canvas.lines.size() * 2, builder.getBatch().getPointCount());
            for (int i = 0; i < canvas.lines.size(); i++) {
                Line line = canvas.lines.get(i);
                assertEquals(line.x1, builder.getBatch().getXData()[i * 2]);
                assertEquals(line.y1, builder.getBatch().getYData()[i * 2]);
                assertEquals(line.x2, builder.getBatch().getXData()[i * 2 + 1]);
                assertEquals(line.y2, builder.getBatch().getYData()[i * 2 + 1]);
                assertTrue(builder.isMoveTo(i * 2));
                assertFalse(builder.isMoveTo(i * 2 + 1));
                for (int j = 0; j < 2; j++) {
                    long key = builder.getBatch().getStyleKey(i * 2 + j);
                    assertEquals(line.argb, SpatialStyleDescriptor.unpackArgb(key));
                    assertEquals(line.stroke, SpatialStyleDescriptor.unpackStrokeWidth(key), 1.0 / 256.0);
                    assertEquals(0, SpatialStyleDescriptor.unpackDashId(key));
                }
            }
        }
        builder.reset();
        grid.renderGridBatch(builder, context, new GridBatchConfig().setGridZ(0));
        assertEquals(0, builder.getBatch().getPointCount(), "The explicit clip plane remains exclusive");
    }

    @ParameterizedTest
    @MethodSource("cartesianGrids")
    void axisGridVisibilityIsIndependent(GridLayer grid) {
        ArberRect bounds = new ArberRect(17, 23, 640, 400);
        for (boolean xVisible : new boolean[]{true, false}) {
            PlotContext context = new Flags(context(bounds), xVisible, !xVisible);
            RecordingCanvas canvas = new RecordingCanvas();
            grid.renderGrid(canvas, context);
            assertTrue(canvas.lines.stream().anyMatch(line -> xVisible
                    ? line.x1 == line.x2 && line.x1 > bounds.x() && line.x1 < bounds.maxX()
                    : line.y1 == line.y2 && line.y1 > bounds.y() && line.y1 < bounds.maxY()));
            assertTrue(canvas.lines.stream().noneMatch(line -> xVisible
                    ? line.y1 == line.y2 && line.y1 > bounds.y() && line.y1 < bounds.maxY()
                    : line.x1 == line.x2 && line.x1 > bounds.x() && line.x1 < bounds.maxX()));
            SpatialPathBatchBuilder builder = new SpatialPathBatchBuilder();
            grid.renderGridBatch(builder, context, null);
            assertEquals(canvas.lines.size() * 2, builder.getBatch().getPointCount());
        }
    }

    @Test
    void medicalMinorSpacingIsCheckedForEachAxisAndStaysInsideTheDataWindow() {
        DefaultPlotContext context = new DefaultPlotContext(new ArberRect(0, 0, 640, 400),
                new DefaultChartModel("medical"), 0.03, 100.03, -1, 1);
        RecordingCanvas canvas = new RecordingCanvas();
        new MedicalGridLayer().renderGrid(canvas, context);
        assertTrue(canvas.lines.stream().anyMatch(line -> Math.abs(line.stroke - 0.45f) < 0.01
                && line.y1 == line.y2), "Y minor lines remain useful when X minor lines are too dense");
        assertFalse(canvas.lines.stream().anyMatch(line -> Math.abs(line.stroke - 0.45f) < 0.01
                && line.x1 == line.x2));
        for (Line line : canvas.lines) {
            assertTrue(line.x1 >= 0 && line.x2 >= 0 && line.x1 <= 640 && line.x2 <= 640);
            assertTrue(line.y1 >= 0 && line.y2 >= 0 && line.y1 <= 400 && line.y2 <= 400);
        }
    }

    @Test
    void majorGridLinesUseRequestedAxisTicks() {
        DefaultPlotContext context = new DefaultPlotContext(new ArberRect(0, 0, 600, 400),
                new DefaultChartModel("ticks"), 0, 10, -2, 2);
        context.setRequestedTickCountX(3).setRequestedTickCountY(3);
        RecordingCanvas canvas = new RecordingCanvas();
        new DefaultGridLayer().renderGrid(canvas, context);
        List<Line> majorVertical = canvas.lines.stream().filter(line -> Math.abs(line.stroke - 0.85f) < 0.001
                && line.x1 == line.x2 && line.x1 > 1 && line.x1 < 599).toList();
        assertEquals(1, majorVertical.size());
        assertEquals(300.5f, majorVertical.getFirst().x1);
        List<Line> majorHorizontal = canvas.lines.stream().filter(line -> Math.abs(line.stroke - 0.85f) < 0.001
                && line.y1 == line.y2 && line.y1 > 1 && line.y1 < 399).toList();
        assertEquals(1, majorHorizontal.size());
        assertEquals(200.5f, majorHorizontal.getFirst().y1);
    }

    @ParameterizedTest
    @MethodSource("numericTickGrids")
    void narrowLargeOffsetWindowDoesNotOverdrawOneGridPosition(GridLayer grid) {
        for (double min : new double[]{1e16, -1e16}) {
            DefaultPlotContext context = new DefaultPlotContext(new ArberRect(0, 0, 600, 400),
                    new DefaultChartModel("offset"), min, min + 4, 0, 10);
            RecordingCanvas canvas = new RecordingCanvas();
            grid.renderGrid(canvas, context);
            assertTrue(canvas.lines.size() < 100);
            List<Line> vertical = canvas.lines.stream().filter(line -> line.x1 == line.x2).toList();
            assertEquals(2, vertical.stream().filter(line -> line.x1 == 300.5f).count());
            assertTrue(vertical.size() < 10);
        }
    }

    @ParameterizedTest
    @MethodSource("numericTickGrids")
    void gridsTerminateWithFiniteGeometryAtExtremeFiniteBounds(GridLayer grid) {
        for (double[] domain : new double[][]{{-Double.MAX_VALUE, Double.MAX_VALUE},
                {0, Double.MAX_VALUE}, {Math.nextDown(Double.MAX_VALUE), Double.MAX_VALUE}}) {
            DefaultPlotContext context = new DefaultPlotContext(new ArberRect(0, 0, 600, 400),
                    new DefaultChartModel("extremes"), domain[0], domain[1], domain[0], domain[1]);
            RecordingCanvas canvas = new RecordingCanvas();
            grid.renderGrid(canvas, context);
            assertTrue(canvas.lines.size() < 200);
            for (Line line : canvas.lines) {
                assertTrue(Float.isFinite(line.x1) && Float.isFinite(line.x2));
                assertTrue(Float.isFinite(line.y1) && Float.isFinite(line.y2));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("numericTickGrids")
    void invalidLogarithmicCoordinatesAreNotSnappedToThePlotEdge(GridLayer grid) {
        DefaultPlotContext context = new DefaultPlotContext(new ArberRect(0, 0, 600, 400),
                0, 10, 0, 1000, true, NiceScale.ScaleMode.LINEAR, NiceScale.ScaleMode.LOGARITHMIC);
        RecordingCanvas canvas = new RecordingCanvas();
        grid.renderGrid(canvas, context);
        assertTrue(canvas.lines.stream().anyMatch(line -> line.x1 == line.x2 && line.x1 > 0 && line.x1 < 600));
        for (Line line : canvas.lines) {
            assertTrue(Float.isFinite(line.y1) && Float.isFinite(line.y2));
            if (line.y1 == line.y2) {
                assertTrue(line.y1 == 0 || line.y1 == 400
                        || (grid instanceof FinancialGridLayer && line.y1 == 300), "Only the frame and volume separator remain");
            }
        }
    }

    @Test
    void isometricSpacingCannotCreateUnboundedLoopsOrLinesOutsidePlot() {
        String key = "Chart.isometricGrid.spacing";
        String previous = ChartAssets.getString(key, null);
        try {
            for (String spacing : List.of("0", "-1", "NaN", "Infinity", "40")) {
                ChartAssets.setProperty(key, spacing);
                RecordingCanvas canvas = new RecordingCanvas();
                ArberRect bounds = new ArberRect(17, 23, 615, 397);
                new IsometricGridLayer().renderGrid(canvas, context(bounds));
                assertFalse(canvas.lines.isEmpty());
                assertTrue(canvas.lines.size() < 1000);
                for (Line line : canvas.lines) {
                    assertTrue(line.x1 >= bounds.x() - 0.0001 && line.x2 >= bounds.x() - 0.0001);
                    assertTrue(line.x1 <= bounds.maxX() + 0.0001 && line.x2 <= bounds.maxX() + 0.0001);
                    assertTrue(line.y1 >= bounds.y() - 0.0001 && line.y2 >= bounds.y() - 0.0001);
                    assertTrue(line.y1 <= bounds.maxY() + 0.0001 && line.y2 <= bounds.maxY() + 0.0001);
                }
            }
        } finally {
            if (previous == null) ChartAssets.removeProperty(key);
            else ChartAssets.setProperty(key, previous);
        }
    }

    @ParameterizedTest
    @CsvSource({"640,400", "400,640", "400,400"})
    void allTernaryRenderersShareEquilateralGridVerticesAndComponentOrientation(int width, int height) {
        ArberRect bounds = new ArberRect(17, 23, width, height);
        PlotContext context = context(bounds);
        RecordingCanvas grid = new RecordingCanvas();
        new TernaryGridLayer().renderGrid(grid, context);
        Line ab = grid.lines.get(0);
        Line bc = grid.lines.get(1);
        Line ca = grid.lines.get(2);
        double side = Math.hypot(ab.x2 - ab.x1, ab.y2 - ab.y1);
        assertEquals(side, Math.hypot(bc.x2 - bc.x1, bc.y2 - bc.y1), 0.0001);
        assertEquals(side, Math.hypot(ca.x2 - ca.x1, ca.y2 - ca.y1), 0.0001);
        assertTrue(ab.x2 < ca.x1, "B is left and C is right");
        double[][] expected = {{ab.x1, ab.y1}, {bc.x1, bc.y1}, {ca.x1, ca.y1},
                {(ab.x1 + bc.x1) / 2, (ab.y1 + bc.y1) / 2},
                {(ab.x1 + bc.x1 + ca.x1) / 3, (ab.y1 + bc.y1 + ca.y1) / 3}};
        double[][] components = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}, {1, 1, 0}, {1, 1, 1}};
        DefaultChartModel regular = new DefaultChartModel("ternary");
        List<TernaryChartModel.TernaryPoint> points = new ArrayList<>();
        for (double[] p : components) {
            regular.setPoint(p[0], p[1], 0, 0, p[2], "");
            points.add(new DefaultTernaryChartModel.DefaultTernaryPoint(p[0], p[1], p[2]));
        }
        DefaultTernaryChartModel ternary = new DefaultTernaryChartModel(points, List.of("A", "B", "C"));
        RecordingCanvas basic = new RecordingCanvas();
        new TernaryPlotRenderer().render(basic, ternary, context);
        RecordingCanvas contour = new RecordingCanvas();
        new TernaryContourRenderer().render(contour, regular, context);
        RecordingCanvas phase = new RecordingCanvas();
        new TernaryPhasediagramRenderer().render(phase, regular, context);
        RecordingCanvas typedContour = new RecordingCanvas();
        new TernaryContourRenderer().render(typedContour, ternary, context);
        RecordingCanvas typedPhase = new RecordingCanvas();
        new TernaryPhasediagramRenderer().render(typedPhase, ternary, context);
        for (RecordingCanvas output : List.of(basic, contour, phase, typedContour, typedPhase)) {
            int offset = output.centers.size() - expected.length;
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i][0], output.centers.get(offset + i)[0], 0.0001);
                assertEquals(expected[i][1], output.centers.get(offset + i)[1], 0.0001);
            }
        }
    }

    @Test
    void smithTransformUsesTheGridCircleAndInvertsIt() {
        PlotContext context = context(new ArberRect(17, 23, 640, 400));
        RecordingCanvas grid = new RecordingCanvas();
        new SmithChartGridLayer().renderGrid(grid, context);
        double[] pixel = new double[2];
        SmithChartTransform transform = new SmithChartTransform();
        transform.mapToPixel(context, 1, 0, pixel);
        assertEquals(grid.lines.getFirst().x1, pixel[0], 0.0001);
        assertEquals(grid.lines.getFirst().y1, pixel[1], 0.0001);
        double[] data = new double[2];
        transform.mapToData(context, pixel[0], pixel[1], data);
        assertEquals(1, data[0], 1e-9);
        assertEquals(0, data[1], 1e-9);
    }

    private static PlotContext context(ArberRect bounds) {
        return new DefaultPlotContext(bounds, new DefaultChartModel("grid"), 0, 10, -2, 2,
                ChartThemes.getDarkTheme());
    }

    private record Flags(PlotContext delegate, boolean isGridVisibleX, boolean isGridVisibleY) implements PlotContext {
        public double getMinX() { return delegate.getMinX(); }
        public double getMaxX() { return delegate.getMaxX(); }
        public double getMinY() { return delegate.getMinY(); }
        public double getMaxY() { return delegate.getMaxY(); }
        public ArberRect getPlotBounds() { return delegate.getPlotBounds(); }
        public void mapToPixel(double x, double y, double[] out) { delegate.mapToPixel(x, y, out); }
        public void mapToData(double x, double y, double[] out) { delegate.mapToData(x, y, out); }
    }

    private record Line(float x1, float y1, float x2, float y2, int argb, float stroke) { }

    private static final class RecordingCanvas implements ArberCanvas {
        final List<Line> lines = new ArrayList<>();
        final List<double[]> centers = new ArrayList<>();
        private int argb;
        private float stroke;
        private float penX;
        private float penY;
        private ArberRect clip;
        public void setColor(ArberColor color) { argb = color.argb(); }
        public void setStroke(float width) { stroke = width; }
        public void moveTo(float x, float y) { penX = x; penY = y; }
        public void lineTo(float x, float y) {
            lines.add(new Line(penX, penY, x, y, argb, stroke));
            moveTo(x, y);
        }
        public void drawPolyline(float[] xs, float[] ys, int count) {
            for (int i = 1; i < count; i++) lines.add(new Line(xs[i - 1], ys[i - 1], xs[i], ys[i], argb, stroke));
        }
        public void drawRect(float x, float y, float w, float h) { throw new AssertionError("Unexpected rectangle"); }
        public void fillRect(float x, float y, float w, float h) { centers.add(new double[]{x + w / 2, y + h / 2}); }
        public void fillPolygon(float[] xs, float[] ys, int count) {
            double x = 0;
            double y = 0;
            for (int i = 0; i < count; i++) { x += xs[i]; y += ys[i]; }
            centers.add(new double[]{x / count, y / count});
        }
        public void drawVoxelField(VoxelBuffer buffer) { throw new AssertionError("Unexpected voxel"); }
        public ArberMatrix getTransform() { return null; }
        public void setClip(ArberRect value) { clip = value; }
        public ArberRect getClip() { return clip; }
    }
}
