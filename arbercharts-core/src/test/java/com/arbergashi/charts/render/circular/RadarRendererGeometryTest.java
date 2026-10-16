package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberMatrix;
import com.arbergashi.charts.core.rendering.VoxelBuffer;
import com.arbergashi.charts.model.DefaultChartModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadarRendererGeometryTest {
    private static final ArberRect BOUNDS = new ArberRect(20, 30, 640, 400);

    @Test
    void everyValueReachesItsOwnAxisAndEveryOutlineCloses() {
        RadarRenderer renderer = new RadarRenderer();
        DefaultChartModel model = model(25, 50, 75, 100);
        DefaultPlotContext context = context(BOUNDS, 100);
        RecordingCanvas canvas = render(renderer, model, context);

        // Four quarter-turn axes: center (340, 230), outer radius 170.
        assertPath(canvas.fills.getFirst(), new float[]{340, 425, 340, 170},
                new float[]{187.5f, 230, 357.5f, 230});
        assertPath(canvas.lines.getLast(), new float[]{340, 425, 340, 170, 340},
                new float[]{187.5f, 230, 357.5f, 230, 187.5f});
        assertEquals(10, canvas.lines.size(), "Five rings, four spokes, and one closed data contour");
        assertEquals(9, canvas.fills.size(), "One area plus two fills for each measurement marker");
        for (int ring = 0; ring < 5; ring++) {
            float radius = 34 * (ring + 1);
            assertPath(canvas.lines.get(ring), new float[]{340, 340 + radius, 340, 340 - radius, 340},
                    new float[]{230 - radius, 230, 230 + radius, 230, 230 - radius});
        }
        for (int index = 0; index < 4; index++) {
            Path area = canvas.fills.getFirst();
            assertEquals(index, renderer.getPointAt(new ArberPoint(area.xs[index], area.ys[index]), model, context)
                    .orElseThrow());
        }
        assertFinite(canvas);
    }

    @Test
    void growingShrinkingAndResizingRebuildOnlyTheCurrentGeometry() {
        RadarRenderer renderer = new RadarRenderer();
        DefaultChartModel model = model(25, 50, 75, 100);
        DefaultPlotContext context = context(BOUNDS, 100);
        render(renderer, model, context);

        replace(model, 100, 100, 100, 100, 100, 100, 100, 100);
        RecordingCanvas grown = render(renderer, model, context);
        assertEquals(8, grown.fills.getFirst().xs.length);
        assertEquals(9, grown.lines.getLast().xs.length);
        assertEquals(14, grown.lines.size());
        assertEquals(17, grown.fills.size());
        for (int i = 0; i < 8; i++) {
            Path area = grown.fills.getFirst();
            assertEquals(170, Math.hypot(area.xs[i] - 340, area.ys[i] - 230), 0.0001);
        }
        assertEquals(grown.lines.getLast().xs[0], grown.lines.getLast().xs[8]);
        assertEquals(grown.lines.getLast().ys[0], grown.lines.getLast().ys[8]);
        assertEquals(7, renderer.getPointAt(new ArberPoint(grown.fills.getFirst().xs[7], grown.fills.getFirst().ys[7]),
                model, context).orElseThrow());

        replace(model, 50, 50, 50);
        context.setPlotBounds(new ArberRect(100, 200, 120, 80));
        RecordingCanvas shrunk = render(renderer, model, context);
        float horizontal = (float) (Math.sqrt(3) * 8.5);
        assertPath(shrunk.fills.getFirst(), new float[]{160, 160 + horizontal, 160 - horizontal},
                new float[]{223, 248.5f, 248.5f});
        assertPath(shrunk.lines.getLast(), new float[]{160, 160 + horizontal, 160 - horizontal, 160},
                new float[]{223, 248.5f, 248.5f, 223});
        assertEquals(9, shrunk.lines.size());
        assertEquals(7, shrunk.fills.size());
        assertTrue(renderer.getPointAt(new ArberPoint(340, 60), model, context).isEmpty());
        assertEquals(0, renderer.getPointAt(new ArberPoint(160, 223), model, context).orElseThrow());
        assertFinite(grown);
        assertFinite(shrunk);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void fewerThanThreeAxesDrawNothingAndCannotKeepAnOldHit(int count) {
        RadarRenderer renderer = new RadarRenderer();
        DefaultChartModel model = model(100, 100, 100, 100);
        DefaultPlotContext context = context(BOUNDS, 100);
        render(renderer, model, context);
        assertFalse(renderer.getPointAt(new ArberPoint(340, 60), model, context).isEmpty());
        double[] values = new double[count];
        Arrays.fill(values, 100);
        replace(model, values);
        RecordingCanvas canvas = render(renderer, model, context);
        assertTrue(canvas.lines.isEmpty());
        assertTrue(canvas.fills.isEmpty());
        assertTrue(renderer.getPointAt(new ArberPoint(340, 60), model, context).isEmpty());
    }

    @Test
    void nonfiniteMeasurementsLeaveGapsWithoutFillingInventedValues() {
        for (double missing : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            RadarRenderer renderer = new RadarRenderer();
            DefaultChartModel model = model(100, missing, 50, 75);
            DefaultPlotContext context = context(BOUNDS, 100);
            RecordingCanvas canvas = render(renderer, model, context);
            assertEquals(11, canvas.lines.size(), "Only the two edges not incident to the missing axis remain");
            assertPath(canvas.lines.get(9), new float[]{340, 212.5f}, new float[]{315, 230});
            assertPath(canvas.lines.get(10), new float[]{212.5f, 340}, new float[]{230, 60});
            assertEquals(6, canvas.fills.size(), "Three markers remain, but no filled data polygon");
            for (Path fill : canvas.fills) assertEquals(20, fill.xs.length);
            assertEquals(2, renderer.getPointAt(new ArberPoint(340, 315), model, context).orElseThrow());
            assertTrue(renderer.getPointAt(new ArberPoint(510, 230), model, context).isEmpty());
            assertFinite(canvas);
        }
    }

    @Test
    void missingFirstMeasurementDoesNotInventAClosingEdge() {
        RecordingCanvas canvas = render(new RadarRenderer(), model(Double.NaN, 100, 50, 75), context(BOUNDS, 100));
        assertEquals(11, canvas.lines.size());
        assertPath(canvas.lines.get(9), new float[]{510, 340}, new float[]{230, 315});
        assertPath(canvas.lines.get(10), new float[]{340, 212.5f}, new float[]{315, 230});
        assertEquals(6, canvas.fills.size());
        assertFinite(canvas);
    }

    @Test
    void invalidRangeAndUnrepresentableBoundsNeverReachTheCanvas() {
        RadarRenderer renderer = new RadarRenderer();
        DefaultChartModel model = model(25, 50, 75, 100);
        for (double maximum : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertEmpty(renderer, model, context(BOUNDS, maximum));
        }
        for (ArberRect bounds : List.of(new ArberRect(0, 0, 1, 400), new ArberRect(0, 0, 400, 0),
                new ArberRect(Double.NaN, 0, 400, 400), new ArberRect(0, 0, Double.POSITIVE_INFINITY, 400),
                new ArberRect(Double.MAX_VALUE, 0, 400, 400))) {
            assertEmpty(renderer, model, context(bounds, 100));
        }
    }

    private static void assertEmpty(RadarRenderer renderer, DefaultChartModel model, DefaultPlotContext context) {
        RecordingCanvas canvas = render(renderer, model, context);
        assertTrue(canvas.lines.isEmpty());
        assertTrue(canvas.fills.isEmpty());
        assertTrue(renderer.getPointAt(new ArberPoint(340, 60), model, context).isEmpty());
    }

    private static DefaultChartModel model(double... values) {
        DefaultChartModel model = new DefaultChartModel("Radar");
        replace(model, values);
        return model;
    }

    private static void replace(DefaultChartModel model, double... values) {
        model.clear();
        for (int i = 0; i < values.length; i++) model.setPoint(i, values[i], 0, 0, 1, "Axis " + i);
    }

    private static DefaultPlotContext context(ArberRect bounds, double maxY) {
        return new DefaultPlotContext(bounds, 0, 100, 0, maxY, false, false, false,
                null, null, ChartThemes.getDarkTheme(), null, null, null);
    }

    private static RecordingCanvas render(RadarRenderer renderer, DefaultChartModel model, DefaultPlotContext context) {
        RecordingCanvas canvas = new RecordingCanvas();
        renderer.render(canvas, model, context);
        return canvas;
    }

    private static void assertPath(Path path, float[] xs, float[] ys) {
        assertArrayEquals(xs, path.xs, 0.0001f);
        assertArrayEquals(ys, path.ys, 0.0001f);
    }

    private static void assertFinite(RecordingCanvas canvas) {
        for (List<Path> paths : List.of(canvas.lines, canvas.fills)) {
            for (Path path : paths) {
                for (float value : path.xs) assertTrue(Float.isFinite(value));
                for (float value : path.ys) assertTrue(Float.isFinite(value));
            }
        }
    }

    private record Path(float[] xs, float[] ys) { }

    private static final class RecordingCanvas implements ArberCanvas {
        private final List<Path> lines = new ArrayList<>();
        private final List<Path> fills = new ArrayList<>();

        @Override public void setColor(ArberColor color) { }
        @Override public void setStroke(float width) { }
        @Override public void drawPolyline(float[] xs, float[] ys, int count) {
            lines.add(new Path(Arrays.copyOf(xs, count), Arrays.copyOf(ys, count)));
        }
        @Override public void fillPolygon(float[] xs, float[] ys, int count) {
            fills.add(new Path(Arrays.copyOf(xs, count), Arrays.copyOf(ys, count)));
        }
        @Override public void moveTo(float x, float y) { throw new AssertionError("Unexpected path operation"); }
        @Override public void lineTo(float x, float y) { throw new AssertionError("Unexpected path operation"); }
        @Override public void drawRect(float x, float y, float w, float h) { throw new AssertionError("Unexpected rectangle"); }
        @Override public void fillRect(float x, float y, float w, float h) { throw new AssertionError("Unexpected rectangle"); }
        @Override public void drawVoxelField(VoxelBuffer buffer) { throw new AssertionError("Unexpected voxel"); }
        @Override public ArberMatrix getTransform() { return null; }
        @Override public void setClip(ArberRect clip) { }
        @Override public ArberRect getClip() { return null; }
    }
}
