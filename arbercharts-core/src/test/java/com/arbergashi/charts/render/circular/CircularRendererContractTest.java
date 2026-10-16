package com.arbergashi.charts.render.circular;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.BasicChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberFont;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberMatrix;
import com.arbergashi.charts.core.rendering.VoxelBuffer;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.model.DefaultHierarchicalChartModel;
import com.arbergashi.charts.model.DefaultMatrixChartModel;
import com.arbergashi.charts.render.ChartRenderer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircularRendererContractTest {
    private static final ArberRect BOUNDS = new ArberRect(20, 30, 640, 400);

    @Test
    void pieAndDonutRefreshSectorColorsAfterThemeChange() {
        DefaultChartModel model = categories(20, 30, 50);
        for (PieRenderer renderer : List.of(new PieRenderer(), new DonutRenderer())) {
            RecordingCanvas dark = render(renderer, model, BOUNDS, ChartThemes.getDarkTheme());
            RecordingCanvas nord = render(renderer, model, BOUNDS, ChartThemes.getNordTheme());
            PieRenderer fresh = renderer instanceof DonutRenderer ? new DonutRenderer() : new PieRenderer();
            assertNotEquals(dark.fills.getFirst().color(), nord.fills.getFirst().color());
            assertEquals(render(fresh, model, BOUNDS, ChartThemes.getNordTheme()).commands, nord.commands);
        }
    }

    @Test
    void piePaletteKeepsItsSequenceBeyond256Categories() {
        DefaultChartModel model = new DefaultChartModel("Many categories");
        for (int i = 0; i < 300; i++) model.setPoint(i, 1, 0, 0, 1, "C" + i);
        RecordingCanvas canvas = render(new PieRenderer(), model, BOUNDS, ChartThemes.getNordTheme());
        assertEquals(ChartThemes.getNordTheme().getSeriesColor(256), canvas.fills.get(256).color());
    }

    @Test
    void pieAndDonutRefreshEverySectorAfterTheSameThemeChangesItsPalette() {
        DefaultChartModel model = new DefaultChartModel("Mutable palette");
        for (int i = 0; i < 300; i++) model.setPoint(i, 1, 0, 0, 1, "C" + i);
        ArberColor[] original = {new ArberColor(0xFFFF0000), new ArberColor(0xFF00FF00),
                new ArberColor(0xFF0000FF)};
        ArberColor[] replacement = {new ArberColor(0xFFFFFF00), new ArberColor(0xFFFF00FF),
                new ArberColor(0xFF00FFFF), new ArberColor(0xFF804020), new ArberColor(0xFF204080)};
        for (PieRenderer renderer : List.of(new PieRenderer(), new DonutRenderer())) {
            BasicChartTheme theme = BasicChartTheme.builder().setSeriesColors(original).build();
            PlotContext context = context(model, BOUNDS, theme);
            RecordingCanvas before = new RecordingCanvas();
            renderer.render(before, model, context);

            theme.setSeriesColors(replacement);
            RecordingCanvas after = new RecordingCanvas();
            renderer.render(after, model, context);

            for (int i = 0; i < 300; i++) {
                assertEquals(original[i % original.length], before.fills.get(i).color(), "Original sector " + i);
                assertEquals(replacement[i % replacement.length], after.fills.get(i).color(), "Updated sector " + i);
            }
        }
    }

    @Test
    void fullCircleHitsEveryQuadrantAndEmptyInputClearsHits() {
        PieRenderer renderer = new PieRenderer();
        DefaultChartModel model = categories(1);
        PlotContext context = context(model, BOUNDS, ChartThemes.getDarkTheme());
        renderer.render(new RecordingCanvas(), model, context);
        for (double degrees : new double[]{0, 45, 135, 225, 315}) {
            double a = Math.toRadians(degrees);
            assertEquals(0, renderer.getPointAt(new ArberPoint(340 + Math.cos(a) * 80,
                    230 + Math.sin(a) * 80), model, context).orElseThrow());
        }
        model.clear();
        renderer.render(new RecordingCanvas(), model, context);
        assertTrue(renderer.getPointAt(new ArberPoint(340, 230), model, context).isEmpty());
    }

    @Test
    void groupedPieAndDonutReturnOriginalIndicesButHighlightTheirCategory() {
        for (PieRenderer renderer : List.of(new PieRenderer(), new DonutRenderer())) {
            DefaultChartModel model = new DefaultChartModel("Grouped categories");
            model.setPoint(0, 1, 0, 0, 1, "A");
            model.setPoint(1, 1, 0, 0, 1, "A");
            model.setPoint(2, 3, 0, 0, 3, "B");
            PlotContext context = context(model, BOUNDS, ChartThemes.getDarkTheme());
            RecordingCanvas before = render(renderer, model, BOUNDS, ChartThemes.getDarkTheme());
            assertEquals(2, renderer.getPointAt(new ArberPoint(240, 230), model, context).orElseThrow());
            RecordingCanvas hovered = render(renderer, model, BOUNDS, ChartThemes.getDarkTheme());
            assertEquals(before.fills.get(0).color(), hovered.fills.get(0).color());
            assertNotEquals(before.fills.get(1).color(), hovered.fills.get(1).color());
            assertEquals(0, renderer.getPointAt(new ArberPoint(440, 230), model, context).orElseThrow());

            model.clear();
            model.setPoint(0, 0, 0, 0, 0, "A");
            model.setPoint(1, 1, 0, 0, 1, "A");
            model.setPoint(2, 1, 0, 0, 1, "A");
            model.setPoint(3, 3, 0, 0, 3, "B");
            render(renderer, model, BOUNDS, ChartThemes.getDarkTheme());
            assertEquals(1, renderer.getPointAt(new ArberPoint(440, 230), model, context).orElseThrow());
            assertEquals(3, renderer.getPointAt(new ArberPoint(240, 230), model, context).orElseThrow());
        }
    }

    @Test
    void donutEmitsMeasuredCenterTextAndExternalLabelsStayInsideBounds() {
        DonutRenderer renderer = new DonutRenderer().setCenterText("Total").setCenterSubText("100");
        RecordingCanvas canvas = render(renderer, categories(30, 70), BOUNDS, ChartThemes.getDarkTheme());
        Text main = canvas.texts.stream().filter(t -> t.value().equals("Total")).findFirst().orElseThrow();
        Text sub = canvas.texts.stream().filter(t -> t.value().equals("100")).findFirst().orElseThrow();
        assertEquals(BOUNDS.centerX(), main.x() + main.width() / 2, 0.001);
        assertEquals(BOUNDS.centerX(), sub.x() + sub.width() / 2, 0.001);
        assertEquals(main.lineHeight(), sub.y() - main.y(), 0.001);
        assertEquals(4, canvas.texts.size(), "Two category labels and two center lines must be emitted");
        for (Text text : canvas.texts) {
            assertTrue(text.x() >= BOUNDS.x());
            assertTrue(text.x() + text.width() <= BOUNDS.maxX());
            assertTrue(text.y() - text.ascent() >= BOUNDS.y());
            assertTrue(text.y() + text.lineHeight() - text.ascent() <= BOUNDS.maxY());
        }
    }

    @Test
    void donutScalesTheMeasuredCenterBlockInsideTheHoleAndRestoresItsClip() {
        ChartTheme theme = themeWithFont(20);
        DefaultChartModel model = categories(100);
        DonutRenderer renderer = new DonutRenderer().setCenterText("7").setCenterSubText("8");
        ArberRect bounds = new ArberRect(10, 15, 100, 100);
        RecordingCanvas canvas = new RecordingCanvas();
        canvas.setClip(bounds);
        renderer.render(canvas, model, context(model, bounds, theme));
        List<Text> center = canvas.texts.stream().filter(t -> t.value().equals("7") || t.value().equals("8")).toList();
        assertEquals(2, center.size());
        assertTrue(center.getFirst().font().size() < 20);
        assertEquals(center.getFirst().font(), center.getLast().font());
        assertEquals(center.getFirst().lineHeight(), center.getLast().y() - center.getFirst().y(), 0.001);
        for (Text text : center) assertInsideDonutHole(text, bounds);
        assertEquals(bounds, canvas.getClip());

        RecordingCanvas resized = render(renderer, model, BOUNDS, themeWithFont(32));
        Text large = resized.texts.stream().filter(t -> t.value().equals("7")).findFirst().orElseThrow();
        assertEquals(32f, large.font().size());
        assertInsideDonutHole(large, BOUNDS);
    }

    @Test
    void donutRetainsThePrimaryLineWhenTwoReadableLinesCannotFit() {
        ArberRect bounds = new ArberRect(0, 0, 50, 50);
        DonutRenderer renderer = new DonutRenderer().setCenterText("7").setCenterSubText("units");
        RecordingCanvas canvas = render(renderer, categories(100), bounds, themeWithFont(40));
        List<Text> center = canvas.texts.stream().filter(t -> t.clip() != null).toList();
        assertEquals(1, center.size());
        assertEquals("7", center.getFirst().value());
        assertEquals(8f, center.getFirst().font().size());
        assertInsideDonutHole(center.getFirst(), bounds);
        assertEquals(null, canvas.getClip());

        RecordingCanvas tiny = render(renderer, categories(100), new ArberRect(0, 0, 10, 10), themeWithFont(40));
        assertTrue(tiny.texts.isEmpty(), "Omit center content when even one readable line cannot fit");
    }

    private static void assertInsideDonutHole(Text text, ArberRect bounds) {
        double radius = Math.min(bounds.width(), bounds.height()) * 0.7 * 0.55 / 2;
        double top = text.y() - text.ascent();
        double bottom = top + text.lineHeight();
        for (double x : new double[]{text.x(), text.x() + text.width()}) {
            for (double y : new double[]{top, bottom}) {
                assertTrue(Math.hypot(x - bounds.centerX(), y - bounds.centerY()) <= radius + 0.001);
            }
        }
        ArberRect clip = text.clip();
        assertTrue(clip != null);
        for (double x : new double[]{clip.minX(), clip.maxX()}) {
            for (double y : new double[]{clip.minY(), clip.maxY()}) {
                assertTrue(Math.hypot(x - bounds.centerX(), y - bounds.centerY()) <= radius + 0.001);
            }
        }
    }

    private static ChartTheme themeWithFont(float size) {
        ChartTheme base = ChartThemes.getDarkTheme();
        return new BasicChartTheme(base.getBackground(), base.getForeground(), base.getGridColor(),
                base.getAxisLabelColor(), base.getAccentColor(), new ArberColor[]{base.getSeriesColor(0)},
                new ArberFont("SansSerif", ArberFont.PLAIN, size));
    }

    @Test
    void semiDonutTrackAndFullProgressShareTheUpperHalf() {
        RecordingCanvas canvas = render(new SemiDonutRenderer(), categories(100), BOUNDS, ChartThemes.getDarkTheme());
        assertEquals(2, canvas.lines.size());
        assertTrue(Arrays.equals(canvas.lines.get(0).xs(), canvas.lines.get(1).xs()));
        assertTrue(Arrays.equals(canvas.lines.get(0).ys(), canvas.lines.get(1).ys()));
        double centerY = BOUNDS.centerY() + 400 * 0.95 * 0.10;
        for (float y : canvas.lines.getFirst().ys()) assertTrue(y <= centerY + 0.001);
    }

    @Test
    void semiDonutZeroProgressDoesNotDrawAColoredDot() {
        RecordingCanvas canvas = render(new SemiDonutRenderer(), categories(0), BOUNDS, ChartThemes.getDarkTheme());
        assertEquals(1, canvas.lines.size());
    }

    @Test
    void chordRecomputesForSameMatrixEditsAndResize() {
        double[][] values = {{0, 1}, {1, 0}};
        DefaultMatrixChartModel model = new DefaultMatrixChartModel(values, List.of("A", "B"));
        ChordDiagramRenderer renderer = new ChordDiagramRenderer();
        RecordingCanvas first = render(renderer, model, BOUNDS, ChartThemes.getDarkTheme());
        values[0][1] = 5;
        RecordingCanvas edited = render(renderer, model, BOUNDS, ChartThemes.getDarkTheme());
        assertNotEquals(first.commands, edited.commands);
        assertEquals(render(new ChordDiagramRenderer(), model, BOUNDS, ChartThemes.getDarkTheme()).commands, edited.commands);
        ArberRect resized = new ArberRect(100, 50, 240, 180);
        RecordingCanvas resizedCanvas = render(renderer, model, resized, ChartThemes.getDarkTheme());
        assertEquals(render(new ChordDiagramRenderer(), model, resized, ChartThemes.getDarkTheme()).commands,
                resizedCanvas.commands);
        assertEquals(resized.centerX(), resizedCanvas.fills.getFirst().xs()[0], 0.001);
        assertEquals(resized.centerY(), resizedCanvas.fills.getFirst().ys()[0], 0.001);
    }

    @Test
    void chordHitsClockwiseSweepAcrossZeroAndDropsEmptyGeometry() {
        double[][] values = {{0, 1}, {1, 0}};
        DefaultMatrixChartModel model = new DefaultMatrixChartModel(values, List.of("A", "B"));
        ChordDiagramRenderer renderer = new ChordDiagramRenderer();
        PlotContext context = context(model, BOUNDS, ChartThemes.getDarkTheme());
        renderer.render(new RecordingCanvas(), model, context);
        assertEquals(0, renderer.getPointAt(new ArberPoint(420, 230), model, context).orElseThrow());
        assertEquals(1, renderer.getPointAt(new ArberPoint(260, 230), model, context).orElseThrow());
        values[0][1] = 0;
        values[1][0] = 0;
        RecordingCanvas empty = render(renderer, model, BOUNDS, ChartThemes.getDarkTheme());
        assertTrue(empty.fills.isEmpty());
        assertTrue(renderer.getPointAt(new ArberPoint(420, 230), model, context).isEmpty());
    }

    @Test
    void nightingaleHitMatchesEachDrawnSector() {
        NightingaleRoseRenderer renderer = new NightingaleRoseRenderer();
        DefaultChartModel model = categories(100, 80, 60, 40);
        PlotContext context = context(model, BOUNDS, ChartThemes.getDarkTheme());
        renderer.render(new RecordingCanvas(), model, context);
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(-90 + i * 90 + 45);
            assertEquals(i, renderer.getPointAt(new ArberPoint(340 + Math.cos(a) * 30,
                    230 + Math.sin(a) * 30), model, context).orElseThrow());
        }
    }

    @Test
    void sunburstRingsStayInsideBoundsAndChildRingSupportsDrillDown() {
        var root = new DefaultHierarchicalChartModel.DefaultNode("Root", 0);
        var child = new DefaultHierarchicalChartModel.DefaultNode("Child", 0);
        child.setChild(new DefaultHierarchicalChartModel.DefaultNode("Leaf", 1));
        root.setChild(child);
        var model = new DefaultHierarchicalChartModel(root);
        SunburstRenderer renderer = new SunburstRenderer();
        PlotContext context = context(model, BOUNDS, ChartThemes.getDarkTheme());
        RecordingCanvas canvas = render(renderer, model, BOUNDS, ChartThemes.getDarkTheme());
        assertEquals(3, canvas.fills.size());
        double maximum = 400 * 0.475;
        for (Path path : canvas.fills) {
            for (int i = 0; i < path.xs().length; i++) {
                assertTrue(Math.hypot(path.xs()[i] - 340, path.ys()[i] - 230) <= maximum + 0.001);
            }
        }
        ArberPoint childPoint = new ArberPoint(340 + maximum / 2, 230);
        assertFalse(renderer.getPointAt(childPoint, model, context).isEmpty());
        renderer.handleClick(childPoint, context);
        assertEquals(2, render(renderer, model, BOUNDS, ChartThemes.getDarkTheme()).fills.size());
        var replacement = new DefaultHierarchicalChartModel(new DefaultHierarchicalChartModel.DefaultNode("New", 10));
        assertEquals(1, render(renderer, replacement, BOUNDS, ChartThemes.getDarkTheme()).fills.size());
    }

    @Test
    void radialBarHitRejectsAngularAndRadialGaps() {
        RadialBarRenderer renderer = new RadialBarRenderer();
        DefaultChartModel model = categories(25);
        PlotContext context = context(model, BOUNDS, ChartThemes.getDarkTheme());
        renderer.render(new RecordingCanvas(), model, context);
        double r = 400 * 0.425 * 0.76;
        assertEquals(0, renderer.getPointAt(new ArberPoint(340 + r / Math.sqrt(2), 230 - r / Math.sqrt(2)),
                model, context).orElseThrow());
        assertTrue(renderer.getPointAt(new ArberPoint(340 - r, 230), model, context).isEmpty());
        assertTrue(renderer.getPointAt(new ArberPoint(340, 230 - 50), model, context).isEmpty());
    }

    @Test
    void radialBarHitIncludesOnlyTheVisibleStrokeEndCaps() {
        RadialBarRenderer renderer = new RadialBarRenderer();
        DefaultChartModel model = categories(25);
        PlotContext context = context(model, BOUNDS, ChartThemes.getDarkTheme());
        renderer.render(new RecordingCanvas(), model, context);
        // These points lie inside the square start/end caps of the 81.6-wide stroke.
        assertEquals(0, renderer.getPointAt(new ArberPoint(320, 101), model, context).orElseThrow());
        assertEquals(0, renderer.getPointAt(new ArberPoint(469, 250), model, context).orElseThrow());
        assertEquals(0, renderer.getPointAt(new ArberPoint(339, 140), model, context).orElseThrow());
        // The corner of the start cap also extends beyond the ideal outer radius of 170.
        assertEquals(0, renderer.getPointAt(new ArberPoint(315, 61), model, context).orElseThrow());
        assertTrue(renderer.getPointAt(new ArberPoint(290, 101), model, context).isEmpty());
        assertTrue(renderer.getPointAt(new ArberPoint(469, 280), model, context).isEmpty());
        assertTrue(renderer.getPointAt(new ArberPoint(240, 230), model, context).isEmpty());
        assertTrue(renderer.getPointAt(new ArberPoint(340, 180), model, context).isEmpty());
    }

    @Test
    void radialStackedHitMatchesTheWedgeAndRejectsOutside() {
        RadialStackedRenderer renderer = new RadialStackedRenderer();
        DefaultChartModel model = new DefaultChartModel("Stacked");
        for (int i = 0; i < 4; i++) model.setXY(20, 30);
        PlotContext context = context(model, BOUNDS, ChartThemes.getDarkTheme());
        renderer.render(new RecordingCanvas(), model, context);
        assertEquals(0, renderer.getPointAt(new ArberPoint(340, 230 - 60), model, context).orElseThrow());
        assertTrue(renderer.getPointAt(new ArberPoint(340, 230 - 400), model, context).isEmpty());
        assertTrue(renderer.getPointAt(new ArberPoint(340, 230 - 10), model, context).isEmpty());
        assertTrue(renderer.getPointAt(new ArberPoint(340 + 60 / Math.sqrt(2), 230 - 60 / Math.sqrt(2)),
                model, context).isEmpty());
    }

    private static DefaultChartModel categories(double... values) {
        DefaultChartModel model = new DefaultChartModel("Categories");
        for (int i = 0; i < values.length; i++) model.setPoint(i, values[i], 0, 0, values[i], "C" + i);
        return model;
    }

    private static PlotContext context(ChartModel model, ArberRect bounds, ChartTheme theme) {
        return new DefaultPlotContext(bounds, model, 0, 100, 0, 100, theme);
    }

    private static RecordingCanvas render(ChartRenderer renderer, ChartModel model, ArberRect bounds, ChartTheme theme) {
        RecordingCanvas canvas = new RecordingCanvas();
        renderer.render(canvas, model, context(model, bounds, theme));
        return canvas;
    }

    private record Path(float[] xs, float[] ys, ArberColor color) { }
    private record Text(float x, float y, String value, ArberFont font, float width,
                        float ascent, float lineHeight, ArberRect clip) { }

    private static final class RecordingCanvas implements ArberCanvas {
        private final List<Path> fills = new ArrayList<>();
        private final List<Path> lines = new ArrayList<>();
        private final List<Text> texts = new ArrayList<>();
        private final List<String> commands = new ArrayList<>();
        private ArberColor color;
        private ArberRect clip;
        private float x;
        private float y;
        private ArberFont font = new ArberFont("SansSerif", ArberFont.PLAIN, 12);

        @Override public void setColor(ArberColor value) { color = value; }
        @Override public void setFont(ArberFont value) {
            ArberCanvas.super.setFont(value);
            font = value == null ? new ArberFont("SansSerif", ArberFont.PLAIN, 12) : value;
            commands.add("font " + font);
        }
        @Override public float measureTextWidth(String text) {
            if (text == null) return 0;
            double units = text.codePoints().mapToDouble(cp -> switch (cp) {
                case 'i', 'l', '1', ' ' -> 0.3;
                case 'W', 'M' -> 0.9;
                default -> 0.55;
            }).sum();
            return (float) (units * font.size());
        }
        @Override public float getTextLineHeight() { return (float) Math.ceil(font.size() * 1.2); }
        @Override public float getTextAscent() { return (float) Math.ceil(font.size() * 0.9); }
        @Override public void setStroke(float value) { commands.add("stroke " + value); }
        @Override public void moveTo(float px, float py) { x = px; y = py; }
        @Override public void lineTo(float px, float py) {
            drawPolyline(new float[]{x, px}, new float[]{y, py}, 2);
            x = px;
            y = py;
        }
        @Override public void drawPolyline(float[] xs, float[] ys, int count) {
            recordPath(lines, "line", xs, ys, count);
        }
        @Override public void fillPolygon(float[] xs, float[] ys, int count) {
            recordPath(fills, "fill", xs, ys, count);
        }
        private void recordPath(List<Path> paths, String operation, float[] xs, float[] ys, int count) {
            float[] cx = Arrays.copyOf(xs, count);
            float[] cy = Arrays.copyOf(ys, count);
            paths.add(new Path(cx, cy, color));
            commands.add(operation + " " + color + " " + Arrays.toString(cx) + " " + Arrays.toString(cy));
        }
        @Override public void drawText(float px, float py, String value) {
            if (value == null || value.isEmpty()) return;
            texts.add(new Text(px, py, value, font, measureTextWidth(value), getTextAscent(), getTextLineHeight(), clip));
            commands.add("text " + px + " " + py + " " + value);
        }
        @Override public void drawRect(float px, float py, float width, float height) {
            throw new AssertionError("Unexpected rectangle");
        }
        @Override public void fillRect(float px, float py, float width, float height) {
            throw new AssertionError("Unexpected rectangle");
        }
        @Override public void drawVoxelField(VoxelBuffer buffer) { throw new AssertionError("Unexpected voxels"); }
        @Override public ArberMatrix getTransform() { return null; }
        @Override public void setClip(ArberRect value) { clip = value; }
        @Override public ArberRect getClip() { return clip; }
    }
}
