package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberMatrix;
import com.arbergashi.charts.core.rendering.VoxelBuffer;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.ChartModel.ChartModelListener;
import com.arbergashi.charts.model.CircularChartModel;
import com.arbergashi.charts.model.DefaultFlowChartModel;
import com.arbergashi.charts.model.DefaultMultiDimensionalChartModel;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.render.grid.PolarGeometry;
import com.arbergashi.charts.util.NiceScale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecializedRendererRegressionTest {
    private static final ArberRect BOUNDS = new ArberRect(17, 23, 640, 400);

    @Test
    void negativeParallelCoordinatesUseTheWholeAxisAndMatchingHitGeometry() {
        var model = new DefaultMultiDimensionalChartModel(
                List.of(new double[]{-10, -20}, new double[]{-5, -10}), List.of("A", "B"));
        var renderer = new ParallelCoordinatesRenderer();
        PlotContext context = context(BOUNDS, 0, 1, 0, 1);
        RecordingCanvas canvas = render(renderer, model, context);
        assertEquals(4, canvas.lines.size());
        assertArrayEquals(new float[]{423, 423}, canvas.lines.get(0).ys);
        assertArrayEquals(new float[]{23, 23}, canvas.lines.get(1).ys);
        assertEquals(1, renderer.getPointAt(new ArberPoint(337, 23), model, context).orElseThrow());
    }

    @Test
    void oneParallelDimensionUsesAFiniteCenteredAxisAndCanBeSelected() {
        var model = new DefaultMultiDimensionalChartModel(List.of(new double[]{-5}, new double[]{-5}), List.of("A"));
        var renderer = new ParallelCoordinatesRenderer();
        PlotContext context = context(BOUNDS, 0, 1, 0, 1);
        RecordingCanvas canvas = render(renderer, model, context);
        assertEquals(2, canvas.rects.size());
        assertEquals(BOUNDS.centerX(), canvas.rects.getFirst().x + 2, 0.0001);
        assertEquals(BOUNDS.centerY(), canvas.rects.getFirst().y + 2, 0.0001);
        assertEquals(0, renderer.getPointAt(new ArberPoint(337, 223), model, context).orElseThrow());
    }

    @Test
    void paretoSortsDisplayedCategoriesAndRetainsTheirOriginalHitIndices() {
        ArrayModel model = new ArrayModel(new double[]{30, 10, 20}, new double[]{1, 3, 2});
        var renderer = new ParetoRenderer();
        PlotContext context = context(new ArberRect(0, 0, 400, 400), 0, 40, 0, 4);
        RecordingCanvas canvas = render(renderer, model, context);
        assertEquals(3, canvas.rects.size());
        for (int i = 0; i < 3; i++) {
            Rect bar = canvas.rects.get(i);
            assertEquals((i + 1) * 100, bar.x + bar.w / 2, 0.0001);
            assertEquals((3 - i) * 100, bar.h, 0.0001);
        }
        Line cumulative = canvas.lines.getFirst();
        assertArrayEquals(new float[]{100, 200, 300}, cumulative.xs);
        assertArrayEquals(new float[]{200, 400f / 6, 0}, cumulative.ys);
        assertEquals(1, renderer.getPointAt(new ArberPoint(100, 250), model, context).orElseThrow());
    }

    @Test
    void controlStatisticsRefreshWhenAFullRingReusesItsArrays() {
        var model = new CircularChartModel(4);
        for (int i = 0; i < 4; i++) model.setPoint(i, 1, 1, "");
        var renderer = new ControlChartRenderer();
        PlotContext context = context(BOUNDS, 0, 4, 0, 10);
        assertEquals(383, render(renderer, model, context).lines.get(2).ys[0], 0.0001);
        double[] original = model.getYData();
        for (int i = 0; i < 4; i++) model.setPoint(i, 9, 1, "");
        assertTrue(original == model.getYData(), "The fixture must exercise reused backing arrays");
        assertEquals(63, render(renderer, model, context).lines.get(2).ys[0], 0.0001);
    }

    @Test
    void paretoOrderAndTotalRefreshWhenAFullRingReusesItsArrays() {
        var model = new CircularChartModel(4);
        for (int i = 0; i < 4; i++) model.setPoint(i, 4 - i, 1, "");
        var renderer = new ParetoRenderer();
        PlotContext context = context(BOUNDS, -0.5, 3.5, 0, 10);
        render(renderer, model, context);
        double[] original = model.getYData();
        for (int i = 0; i < 4; i++) model.setPoint(i, i == 3 ? 10 : i + 1, 1, "");
        assertTrue(original == model.getYData());
        RecordingCanvas canvas = render(renderer, model, context);
        assertEquals(400, canvas.rects.getFirst().h, 0.0001);
        assertEquals(173, canvas.lines.getFirst().ys[0], 0.0001);
        assertEquals(23, canvas.lines.getFirst().ys[3], 0.0001);
    }

    @Test
    void unversionedMutableModelsDoNotKeepStatisticsOrParetoOrder() {
        ArrayModel model = new ArrayModel(new double[]{0, 1}, new double[]{1, 2});
        model.stamp = 0;
        var control = new ControlChartRenderer();
        var pareto = new ParetoRenderer();
        PlotContext context = context(BOUNDS, -0.5, 1.5, 0, 10);
        render(control, model, context);
        render(pareto, model, context);
        model.ys[0] = 8;
        model.ys[1] = 6;
        assertEquals(143, render(control, model, context).lines.get(2).ys[0], 0.0001);
        assertEquals(320, render(pareto, model, context).rects.getFirst().h, 0.0001);
    }

    @Test
    void equalUpdateStampsInDifferentModelsDoNotShareCachedStatistics() {
        var control = new ControlChartRenderer();
        var pareto = new ParetoRenderer();
        PlotContext context = context(BOUNDS, -0.5, 1.5, 0, 10);
        render(control, new ArrayModel(new double[]{0, 1}, new double[]{1, 1}), context);
        render(pareto, new ArrayModel(new double[]{0, 1}, new double[]{1, 1}), context);
        ArrayModel second = new ArrayModel(new double[]{0, 1}, new double[]{7, 7});
        assertEquals(143, render(control, second, context).lines.get(2).ys[0], 0.0001);
        assertEquals(280, render(pareto, second, context).rects.getFirst().h, 0.0001);
    }

    @Test
    void fastParetoResizesItsLogicalBucketsAndSortsRepeatedValuesWithoutDeepRecursion() {
        ArrayModel model = sequence(6000);
        Arrays.fill(model.ys, 1);
        var renderer = new ParetoRenderer();
        render(renderer, model, context(BOUNDS, 0, 5999, 0, 2));
        ArberRect small = new ArberRect(5, 7, 32, 40);
        RecordingCanvas canvas = render(renderer, model, context(small, 0, 5999, 0, 2));
        assertTrue(canvas.rects.size() <= 32);
        assertFalse(canvas.rects.isEmpty());
        for (Rect rectangle : canvas.rects) assertInside(rectangle, small);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void explicitHeatmapIncludesConstantAndMinimumCellsWithEitherAxisDirection(boolean inverted) {
        ArrayModel model = new ArrayModel(new double[0], new double[0]);
        PlotContext context = new DefaultPlotContext(BOUNDS, 0, 2, 0, 2, false,
                inverted, inverted, NiceScale.ScaleMode.LINEAR, NiceScale.ScaleMode.LINEAR);
        var renderer = new HeatmapRenderer().setGridData(new double[][]{{5, 5}, {5, 5}}, 0, 2, 0, 2);
        RecordingCanvas constant = render(renderer, model, context);
        assertEquals(4, constant.rects.size());
        for (Rect rectangle : constant.rects) {
            assertInside(rectangle, BOUNDS);
            assertEquals(320, rectangle.w, 0.0001);
            assertEquals(200, rectangle.h, 0.0001);
        }
        renderer.setGridData(new double[][]{{0, 1}, {2, 3}}, 0, 2, 0, 2);
        assertEquals(4, render(renderer, model, context).rects.size());
    }

    @Test
    void densityHeatmapDrawsOnlyOccupiedBinsIncludingTheMaximumBoundary() {
        ArrayModel model = new ArrayModel(new double[]{2}, new double[]{2});
        var renderer = new HeatmapRenderer();
        PlotContext context = context(BOUNDS, 0, 2, 0, 2);
        RecordingCanvas canvas = render(renderer, model, context);
        assertEquals(1, canvas.rects.size());
        assertTrue(renderer.getPointAt(new ArberPoint(BOUNDS.maxX(), BOUNDS.y()), model, context).isPresent());
        assertTrue(renderer.getTooltipText(0, model).endsWith(String.format("%.3f", 1.0)));
    }

    @Test
    void contourInterpolationControlsColorsWithoutMovingCells() {
        ArrayModel model = new ArrayModel(new double[]{0.25, 0.75, 0.25, 0.75}, new double[]{0.25, 0.25, 0.75, 0.75});
        model.weights = new double[]{0, 0.2, 0.7, 1};
        var renderer = new HeatmapContourRenderer();
        PlotContext context = context(BOUNDS, 0, 1, 0, 1);
        RecordingCanvas continuous = render(renderer, model, context);
        RecordingCanvas discrete = render(renderer.setInterpolate(false), model, context);
        assertEquals(93, continuous.rects.get(1).color.alpha());
        assertEquals(171, continuous.rects.get(2).color.alpha());
        assertEquals(63, discrete.rects.get(1).color.alpha());
        assertEquals(165, discrete.rects.get(2).color.alpha());
        for (int i = 0; i < 4; i++) {
            Rect a = continuous.rects.get(i);
            Rect b = discrete.rects.get(i);
            assertEquals(a.x, b.x);
            assertEquals(a.y, b.y);
            assertEquals(a.w, b.w);
            assertEquals(a.h, b.h);
        }
    }

    @Test
    void contourIgnoresNonFiniteSamplesAndPlacesInvertedCellsAtTheMappedCorner() {
        ArrayModel model = new ArrayModel(new double[]{0.25, 0.75, 0.25, 0.75, Double.NaN},
                new double[]{0.25, 0.25, 0.75, 0.75, 0.5});
        model.weights = new double[]{0, 0.2, 0.7, Double.NaN, 100};
        PlotContext context = new DefaultPlotContext(BOUNDS, 0, 1, 0, 1, false,
                true, true, NiceScale.ScaleMode.LINEAR, NiceScale.ScaleMode.LINEAR);
        RecordingCanvas canvas = render(new HeatmapContourRenderer(), model, context);
        assertEquals(3, canvas.rects.size());
        for (Rect rectangle : canvas.rects) assertInside(rectangle, BOUNDS);
        Rect first = canvas.rects.getFirst();
        assertEquals(497, first.x + first.w / 2, 0.0001);
        assertEquals(123, first.y + first.h / 2, 0.0001);
    }

    @Test
    void sankeyUsesUniqueNodeCountAndKeepsEveryRibbonInsideThePlot() {
        ArrayModel model = links("A:B", "C:D");
        var renderer = new SankeyProRenderer();
        for (ArberRect bounds : List.of(BOUNDS, new ArberRect(5, 7, 20, 10))) {
            RecordingCanvas canvas = render(renderer, model, context(bounds, 0, 1, 0, 1));
            assertEquals(2, canvas.lines.size());
            for (Line line : canvas.lines) assertInside(line, bounds, line.stroke / 2.0);
        }
        RecordingCanvas canvas = render(renderer, model, context(BOUNDS, 0, 1, 0, 1));
        assertEquals(63, canvas.lines.getFirst().ys[0], 0.0001);
        assertEquals(383, canvas.lines.getLast().ys[24], 0.0001);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 5, 7, 10})
    void alluvialLayoutsIncompleteColumnsWithinTheirBounds(int count) {
        var nodes = new ArrayList<DefaultFlowChartModel.DefaultNode>();
        for (int i = 0; i < count; i++) nodes.add(new DefaultFlowChartModel.DefaultNode("N" + i, "N" + i));
        var model = new DefaultFlowChartModel(nodes,
                List.of(new DefaultFlowChartModel.DefaultLink("N0", "N" + (count - 1), 1)));
        var renderer = new AlluvialRenderer();
        for (ArberRect bounds : List.of(BOUNDS, new ArberRect(0.5, 1.5, 18, 9))) {
            RecordingCanvas canvas = render(renderer, model, context(bounds, 0, 1, 0, 1));
            assertEquals(count, canvas.rects.size());
            for (Rect rectangle : canvas.rects) assertInside(rectangle, bounds);
            for (Line polygon : canvas.polygons) assertInside(polygon, bounds, 0);
        }
    }

    @Test
    void dependencyWheelAssignsFourDistinctPositionsToTwoDisjointEdges() {
        RecordingCanvas canvas = render(new DependencyWheelRenderer(), links("A:B", "C:D"),
                context(BOUNDS, 0, 1, 0, 1));
        Line first = canvas.lines.get(0);
        Line second = canvas.lines.get(1);
        double radius = 140;
        assertArrayEquals(new float[]{(float) (337 + radius), 337}, first.xs);
        assertArrayEquals(new float[]{223, (float) (223 + radius)}, first.ys);
        assertArrayEquals(new float[]{(float) (337 - radius), 337}, second.xs);
        assertArrayEquals(new float[]{223, (float) (223 - radius)}, second.ys);
    }

    @Test
    void bothHorizonVariantsFitAllPositiveAndNegativeBandsInsideThePlot() {
        ArrayModel model = new ArrayModel(new double[]{-1, 1, 3}, new double[]{1, -1, 1});
        for (ChartRenderer renderer : List.of(new HorizonRenderer(), new HorizonChartRenderer())) {
            RecordingCanvas canvas = render(renderer, model, context(BOUNDS, 0, 2, -1, 1));
            assertEquals(8, canvas.rects.size());
            for (Rect rectangle : canvas.rects) assertInside(rectangle, BOUNDS);
            assertEquals(23, canvas.rects.stream().mapToDouble(Rect::y).min().orElseThrow(), 0.0001);
            assertEquals(423, canvas.rects.stream().mapToDouble(r -> r.y + r.h).max().orElseThrow(), 0.0001);
        }
    }

    @Test
    void spectrogramIncludesTheFinalSampleAndPeaksBetweenPixelSamplePositions() {
        ArrayModel model = sequence(1000);
        Arrays.fill(model.ys, 0);
        model.ys[998] = 10;
        model.ys[999] = 10;
        RecordingCanvas canvas = render(new SpectrogramRenderer(), model, context(BOUNDS, 0, 999, 0, 10));
        assertEquals(640, canvas.rects.size());
        assertTrue(canvas.rects.getLast().color.alpha() > canvas.rects.getFirst().color.alpha());
        assertEquals(656, canvas.rects.getLast().x, 0.0001);
    }

    @Test
    void sparklineEndpointMarksTheFinalSampleAfterPixelDecimation() {
        ArrayModel model = sequence(3000);
        Arrays.fill(model.ys, 1);
        model.ys[2998] = 10;
        model.ys[2999] = 0;
        RecordingCanvas canvas = render(new SparklineRenderer(), model, context(BOUNDS, 0, 2999, 0, 10));
        assertEquals(1, canvas.polygons.size());
        Line endpoint = canvas.polygons.getFirst();
        assertEquals(BOUNDS.maxX(), (endpoint.xs[0] + endpoint.xs[4]) / 2, 0.0001);
        assertEquals(BOUNDS.maxY(), (endpoint.ys[2] + endpoint.ys[6]) / 2, 0.0001);
    }

    @Test
    void windRoseUsesTheSharedPolarGridRadius() {
        RecordingCanvas canvas = render(new WindRoseRenderer(),
                new ArrayModel(new double[]{0}, new double[]{1}), context(BOUNDS, 0, 360, 0, 1));
        assertEquals(1, canvas.polygons.size());
        Line wedge = canvas.polygons.getFirst();
        for (int i = 0; i < 25; i++) {
            assertEquals(PolarGeometry.radius(BOUNDS),
                    Math.hypot(wedge.xs[i] - BOUNDS.centerX(), wedge.ys[i] - BOUNDS.centerY()), 0.0001);
        }
    }

    static PlotContext context(ArberRect bounds, double minX, double maxX, double minY, double maxY) {
        return new DefaultPlotContext(bounds, minX, maxX, minY, maxY, false,
                NiceScale.ScaleMode.LINEAR, NiceScale.ScaleMode.LINEAR);
    }

    static RecordingCanvas render(ChartRenderer renderer, ChartModel model, PlotContext context) {
        RecordingCanvas canvas = new RecordingCanvas();
        renderer.render(canvas, model, context);
        return canvas;
    }

    static ArrayModel sequence(int count) {
        double[] xs = new double[count];
        double[] ys = new double[count];
        for (int i = 0; i < count; i++) xs[i] = i;
        return new ArrayModel(xs, ys);
    }

    private static ArrayModel links(String... labels) {
        ArrayModel model = sequence(labels.length);
        Arrays.fill(model.ys, 10);
        model.labels = labels;
        return model;
    }

    private static void assertArrayEquals(float[] expected, float[] actual) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual, 0.0001f);
    }

    private static void assertInside(Rect rectangle, ArberRect bounds) {
        assertTrue(rectangle.w > 0 && rectangle.h > 0);
        assertTrue(rectangle.x >= bounds.x() - 0.0001 && rectangle.y >= bounds.y() - 0.0001);
        assertTrue(rectangle.x + rectangle.w <= bounds.maxX() + 0.0001);
        assertTrue(rectangle.y + rectangle.h <= bounds.maxY() + 0.0001);
    }

    private static void assertInside(Line line, ArberRect bounds, double inset) {
        for (int i = 0; i < line.xs.length; i++) {
            assertTrue(line.xs[i] >= bounds.x() + inset - 0.0001 && line.xs[i] <= bounds.maxX() - inset + 0.0001);
            assertTrue(line.ys[i] >= bounds.y() + inset - 0.0001 && line.ys[i] <= bounds.maxY() - inset + 0.0001);
        }
    }

    static final class ArrayModel implements ChartModel {
        final double[] xs;
        final double[] ys;
        int count;
        long stamp = 1;
        String[] labels;
        double[] weights;
        ArrayModel(double[] xs, double[] ys) { this.xs = xs; this.ys = ys; this.count = Math.min(xs.length, ys.length); }
        @Override public String getName() { return "Regression fixture"; }
        @Override public int getPointCount() { return count; }
        @Override public double[] getXData() { return xs; }
        @Override public double[] getYData() { return ys; }
        @Override public double[] getWeightData() { return weights == null ? ys : weights; }
        @Override public String getLabel(int index) { return labels == null ? "" : labels[index]; }
        @Override public long getUpdateStamp() { return stamp; }
        @Override public void setChangeListener(ChartModelListener listener) { }
        @Override public void removeChangeListener(ChartModelListener listener) { }
    }

    record Line(float[] xs, float[] ys, float stroke, ArberColor color) { }
    record Rect(float x, float y, float w, float h, ArberColor color) { }

    static final class RecordingCanvas implements ArberCanvas {
        final List<Line> lines = new ArrayList<>();
        final List<Line> polygons = new ArrayList<>();
        final List<Rect> rects = new ArrayList<>();
        private ArberColor color;
        private float stroke = 1;
        private float x;
        private float y;
        private ArberRect clip;
        @Override public void setColor(ArberColor color) { this.color = color; }
        @Override public void setStroke(float width) { stroke = width; }
        @Override public void moveTo(float x, float y) { this.x = x; this.y = y; }
        @Override public void lineTo(float x, float y) { drawPolyline(new float[]{this.x, x}, new float[]{this.y, y}, 2); this.x = x; this.y = y; }
        @Override public void drawPolyline(float[] xs, float[] ys, int count) { lines.add(new Line(Arrays.copyOf(xs, count), Arrays.copyOf(ys, count), stroke, color)); }
        @Override public void drawRect(float x, float y, float w, float h) { rects.add(new Rect(x, y, w, h, color)); }
        @Override public void fillRect(float x, float y, float w, float h) { rects.add(new Rect(x, y, w, h, color)); }
        @Override public void fillPolygon(float[] xs, float[] ys, int count) { polygons.add(new Line(Arrays.copyOf(xs, count), Arrays.copyOf(ys, count), stroke, color)); }
        @Override public void drawVoxelField(VoxelBuffer buffer) { throw new AssertionError("Unexpected voxel output"); }
        @Override public ArberMatrix getTransform() { return null; }
        @Override public void setClip(ArberRect clip) { this.clip = clip; }
        @Override public ArberRect getClip() { return clip; }
    }
}
