package com.arbergashi.charts.render.medical;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.ChartThemes;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberMatrix;
import com.arbergashi.charts.core.rendering.VoxelBuffer;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.model.DefaultSignalChartModel;
import com.arbergashi.charts.model.FastMedicalModel;
import com.arbergashi.charts.render.ChartRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicalRendererRingBufferTest {
    private static final PlotContext CONTEXT = new IdentityContext(0, 4096);
    private static final PlotContext EEG_CONTEXT = new IdentityContext(-9, 9);

    static Stream<SweepCase> sweepRenderers() {
        return Stream.of(
                new SweepCase("ECG", ECGRenderer::new, 20),
                new SweepCase("EMG", EMGRenderer::new, 12),
                new SweepCase("EOG", EOGRenderer::new, 20),
                new SweepCase("Capnography", CapnographyRenderer::new, 20),
                new SweepCase("IBP", IBPRenderer::new, 20),
                new SweepCase("NIRS", NIRSRenderer::new, 10),
                new SweepCase("PPG", PPGRenderer::new, 25),
                new SweepCase("MedicalSweep", MedicalSweepRenderer::new, 10),
                new SweepCase("SweepEraseEKG", SweepEraseEKGRenderer::new, 20));
    }

    @ParameterizedTest(name = "{0}: empty and single sample")
    @MethodSource("sweepRenderers")
    void sweepDoesNotDrawUnwrittenSlots(SweepCase spec) {
        ChartRenderer renderer = spec.factory.get();
        CircularFastMedicalModel model = medicalModel(64, 0);
        RecordingCanvas empty = render(renderer, model, CONTEXT);
        assertTrue(empty.lines.isEmpty());
        assertTrue(empty.markers.isEmpty());

        appendSamples(model, 0, 1);
        RecordingCanvas single = render(renderer, model, CONTEXT);
        assertTrue(single.lines.isEmpty(), "A lone sample cannot form a line");
        assertEquals(renderer instanceof SweepEraseEKGRenderer ? List.of(point(0, 0)) : List.of(),
                single.markers, "The leading marker must use the sample's actual X coordinate");
    }

    @ParameterizedTest(name = "{0}: partially filled")
    @MethodSource("sweepRenderers")
    void sweepUsesOnlyValidSamplesAndTheFirstYChannel(SweepCase spec) {
        RecordingCanvas canvas = render(spec.factory.get(), medicalModel(64, 6), CONTEXT);
        assertEquals(List.of(points(0, 6, 0)), canvas.lines);
    }

    @ParameterizedTest(name = "{0}: full buffer")
    @MethodSource("sweepRenderers")
    void fullSweepErasesOnlyTheConfiguredGap(SweepCase spec) {
        RecordingCanvas canvas = render(spec.factory.get(), medicalModel(64, 64), CONTEXT);
        assertEquals(List.of(points(spec.gap, 64, 0)), canvas.lines);
    }

    @ParameterizedTest(name = "{0}: wrapped buffer")
    @MethodSource("sweepRenderers")
    void wrappedSweepKeepsOldAndNewPassesDisconnected(SweepCase spec) {
        RecordingCanvas canvas = render(spec.factory.get(), medicalModel(64, 70), CONTEXT);
        assertEquals(List.of(points(64, 70, 0), points(6 + spec.gap, 64, 0)), canvas.lines);
    }

    @ParameterizedTest(name = "{0}: wrapped gap")
    @MethodSource("sweepRenderers")
    void sweepGapCanCrossTheEndOfTheBuffer(SweepCase spec) {
        RecordingCanvas canvas = render(spec.factory.get(), medicalModel(64, 120), CONTEXT);
        assertEquals(List.of(points(56 + spec.gap, 120, 0)), canvas.lines);
    }

    @ParameterizedTest(name = "{0}: clear and reuse")
    @MethodSource("sweepRenderers")
    void clearedSweepDoesNotReuseStaleSamples(SweepCase spec) {
        ChartRenderer renderer = spec.factory.get();
        CircularFastMedicalModel model = medicalModel(64, 80);
        render(renderer, model, CONTEXT);
        model.clear();
        assertTrue(render(renderer, model, CONTEXT).lines.isEmpty());
        appendSamples(model, 200, 203);
        assertEquals(List.of(points(200, 203, 0)), render(renderer, model, CONTEXT).lines);
    }

    @ParameterizedTest(name = "{0}: missing sample")
    @MethodSource("sweepRenderers")
    void sweepDoesNotConnectAcrossMissingValues(SweepCase spec) {
        CircularFastMedicalModel model = medicalModel(64, 6);
        model.getRawChannelArray(0)[2] = Double.NaN;
        RecordingCanvas canvas = render(spec.factory.get(), model, CONTEXT);
        assertEquals(List.of(points(0, 2, 0), points(3, 6, 0)), canvas.lines);
    }

    @ParameterizedTest(name = "{0}: buffer smaller than gap")
    @MethodSource("sweepRenderers")
    void oversizedSweepGapRetainsOnlyTheNewestSample(SweepCase spec) {
        ChartRenderer renderer = spec.factory.get();
        RecordingCanvas canvas = render(renderer, medicalModel(4, 6), CONTEXT);
        assertTrue(canvas.lines.isEmpty());
        assertEquals(renderer instanceof SweepEraseEKGRenderer ? List.of(point(5, 0)) : List.of(),
                canvas.markers);
    }

    @Test
    void sharedSweepHonorsItsSelectedChannel() {
        AbstractMedicalSweepRenderer renderer = new AbstractMedicalSweepRenderer(null, 1, 2) {
            @Override
            protected int getChannelIndex() {
                return 1;
            }
        };
        assertEquals(List.of(points(0, 6, 1)), render(renderer, medicalModel(64, 6), CONTEXT).lines);
    }

    @Test
    void zeroWidthGapStillSeparatesTheTwoSweepPasses() {
        AbstractMedicalSweepRenderer renderer = new AbstractMedicalSweepRenderer(null, 1, 0) {};
        assertEquals(List.of(points(4, 6, 0), points(2, 4, 0)),
                render(renderer, medicalModel(4, 6), CONTEXT).lines);
    }

    @Test
    void sweepHitTestingIgnoresErasedSamplesAndReturnsLogicalIndices() {
        ECGRenderer renderer = new ECGRenderer();
        CircularFastMedicalModel model = medicalModel(64, 70);
        assertTrue(renderer.getPointAt(new ArberPoint(200, 1010), model, CONTEXT).isEmpty());
        assertEquals(24, renderer.getPointAt(new ArberPoint(400, 1030), model, CONTEXT).orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 6, 64, 70, 120})
    void spectrogramUsesValidDepthAndIntensitySamples(int writes) {
        RecordingCanvas canvas = render(new SpectrogramMedicalRenderer(), medicalModel(64, writes), CONTEXT);
        List<Integer> visible = switch (writes) {
            case 0 -> List.of();
            case 1, 6 -> IntStream.range(0, writes).boxed().toList();
            case 64 -> IntStream.range(15, 64).boxed().toList();
            case 70 -> IntStream.concat(IntStream.range(64, 70), IntStream.range(21, 64)).boxed().toList();
            case 120 -> IntStream.range(71, 120).boxed().toList();
            default -> throw new AssertionError("Unexpected test case");
        };
        List<List<Point>> expected = visible.stream()
                .map(i -> List.of(new Point(x(i), 0), point(i, 0))).toList();
        assertEquals(expected, canvas.lines);
    }

    @Test
    void spectrogramRequiresBothChannelsAndSkipsNonFiniteIntensity() {
        CircularFastMedicalModel singleChannel = new CircularFastMedicalModel(64, 1);
        singleChannel.add(123, new double[]{456});
        assertTrue(render(new SpectrogramMedicalRenderer(), singleChannel, CONTEXT).lines.isEmpty());
        CircularFastMedicalModel model = medicalModel(64, 3);
        model.getRawChannelArray(1)[1] = Double.NaN;
        assertEquals(List.of(List.of(new Point(x(0), 0), point(0, 0)),
                        List.of(new Point(x(2), 0), point(2, 0))),
                render(new SpectrogramMedicalRenderer(), model, CONTEXT).lines);
    }

    @Test
    void spectrogramDoesNotDrawStaleColumnsAfterClear() {
        SpectrogramMedicalRenderer renderer = new SpectrogramMedicalRenderer();
        CircularFastMedicalModel model = medicalModel(64, 70);
        render(renderer, model, CONTEXT);
        model.clear();
        assertTrue(render(renderer, model, CONTEXT).lines.isEmpty());
        appendSamples(model, 200, 201);
        assertEquals(List.of(List.of(new Point(x(200), 0), point(200, 0))),
                render(renderer, model, CONTEXT).lines);
    }

    @Test
    void eegDrawsSimultaneousChannelsOverTheirCompleteTimeRange() {
        CircularFastMedicalModel model = eegModel(16, 5);
        assertEquals(3, model.getChannelCount());
        assertEegChannels(render(new EEGRenderer(), model, EEG_CONTEXT), 0, 5);
    }

    @Test
    void eegReadsWrappedChannelsInLogicalOrder() {
        assertEegChannels(render(new EEGRenderer(), eegModel(4, 6), EEG_CONTEXT), 2, 6);
    }

    @Test
    void eegUsesDistinctDarkPaletteColorsForAllChannels() {
        RecordingCanvas canvas = render(new EEGRenderer(), eegModel(4, 6), EEG_CONTEXT);
        assertEegChannels(canvas, 2, 6);
        assertEquals(List.of(new ArberColor(0xFF3B82F6), new ArberColor(0xFF22C55E),
                new ArberColor(0xFFF59E0B)), canvas.lineColors);
    }

    @Test
    void eegFollowsThemeChangesWithoutAlteringChannelGeometry() {
        EEGRenderer renderer = new EEGRenderer();
        CircularFastMedicalModel model = eegModel(4, 6);
        render(renderer, model, EEG_CONTEXT);

        RecordingCanvas canvas = render(renderer, model,
                new IdentityContext(-9, 9, ChartThemes.getLightTheme()));
        assertEegChannels(canvas, 2, 6);
        assertEquals(List.of(new ArberColor(0xFF2563EB), new ArberColor(0xFF16A34A),
                new ArberColor(0xFFD97706)), canvas.lineColors);
    }

    @Test
    void eegSupportsSignalModelChannelSemanticsAndWrap() {
        DefaultSignalChartModel model = new DefaultSignalChartModel(3, 4, true);
        for (int i = 0; i < 6; i++) model.setSample(x(i), eegValues(i));
        assertEegChannels(render(new EEGRenderer(), model, EEG_CONTEXT), 2, 6);
    }

    @Test
    void eegConfiguredChannelCountLimitsOutputWithoutSlicingSamples() {
        RecordingCanvas canvas = render(new EEGRenderer(1), eegModel(16, 5), EEG_CONTEXT);
        assertEquals(1, canvas.lines.size());
        assertLine(canvas.lines.getFirst(), IntStream.range(0, 5)
                .mapToObj(i -> new Point(x(i), eegValues(i)[0])).toList());
    }

    @Test
    void eegFitsEveryChannelIntoItsOwnBandForAnOffsetYRange() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(16, 3);
        model.add(10, new double[]{100, 100, 100});
        model.add(20, new double[]{200, 200, 200});
        RecordingCanvas canvas = render(new EEGRenderer(), model, new IdentityContext(100, 200));
        assertEquals(3, canvas.lines.size());
        for (int channel = 0; channel < 3; channel++) {
            assertLine(canvas.lines.get(channel), List.of(new Point(10, 100 + channel * 100.0 / 3),
                    new Point(20, 100 + (channel + 1) * 100.0 / 3)));
        }
    }

    @Test
    void eegTreatsScalarModelsAsOneChannel() {
        FastMedicalModel model = new FastMedicalModel("Scalar", 16);
        for (int i = 0; i < 5; i++) model.setPoint(x(i), eegValues(i)[0]);
        RecordingCanvas canvas = render(new EEGRenderer(), model, EEG_CONTEXT);
        assertEquals(1, canvas.lines.size());
        assertLine(canvas.lines.getFirst(), IntStream.range(0, 5)
                .mapToObj(i -> new Point(x(i), eegValues(i)[0])).toList());
    }

    @Test
    void missingEegChannelValueDoesNotEraseOtherChannelsOrBridgeTheGap() {
        CircularFastMedicalModel model = eegModel(16, 6);
        model.getRawChannelArray(1)[2] = Double.NaN;
        RecordingCanvas canvas = render(new EEGRenderer(), model, EEG_CONTEXT);
        assertEquals(4, canvas.lines.size());
        assertLine(canvas.lines.get(0), eegPoints(0, 6, 0));
        assertLine(canvas.lines.get(1), eegPoints(0, 2, 1));
        assertLine(canvas.lines.get(2), eegPoints(3, 6, 1));
        assertLine(canvas.lines.get(3), eegPoints(0, 6, 2));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void eegAndRhythmDoNotDrawWithFewerThanTwoSamples(int count) {
        CircularFastMedicalModel model = eegModel(16, count);
        assertTrue(render(new EEGRenderer(), model, EEG_CONTEXT).lines.isEmpty());
        assertTrue(render(new ECGRhythmRenderer(), model, EEG_CONTEXT).lines.isEmpty());
    }

    @Test
    void rhythmReadsWrappedBufferInChronologicalOrder() {
        assertEquals(List.of(points(2, 6, 0)),
                render(new ECGRhythmRenderer(), medicalModel(4, 6), CONTEXT).lines);
    }

    @Test
    void rhythmDoesNotJoinAcrossMissingSamples() {
        CircularFastMedicalModel model = medicalModel(64, 6);
        model.getRawChannelArray(0)[2] = Double.NaN;
        assertEquals(List.of(points(0, 2, 0), points(3, 6, 0)),
                render(new ECGRhythmRenderer(), model, CONTEXT).lines);
    }

    @Test
    void logicalLineRenderersSplitWhenSweepTimeRestarts() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(4, 1);
        for (int i = 0; i < 6; i++) model.add(i % 4, new double[]{i});
        List<List<Point>> expected = List.of(List.of(new Point(2, 2), new Point(3, 3)),
                List.of(new Point(0, 4), new Point(1, 5)));
        assertEquals(expected, render(new ECGRhythmRenderer(), model, EEG_CONTEXT).lines);
        assertEquals(expected, render(new EEGRenderer(), model, EEG_CONTEXT).lines);
        assertEquals(expected, render(new VentilatorWaveformRenderer(), model, EEG_CONTEXT).lines);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void ventilatorUsesOnlyAvailableChannelsAndAtMostThree(int channels) {
        CircularFastMedicalModel model = new CircularFastMedicalModel(4, channels);
        for (int i = 0; i < 6; i++) {
            double[] values = new double[channels];
            for (int channel = 0; channel < channels; channel++) values[channel] = point(i, channel).y;
            model.add(x(i), values);
        }
        RecordingCanvas canvas = render(new VentilatorWaveformRenderer(), model, CONTEXT);
        assertEquals(Math.min(3, channels), canvas.lines.size());
        for (int channel = 0; channel < Math.min(3, channels); channel++) {
            assertEquals(points(2, 6, channel), canvas.lines.get(channel));
        }
    }

    @Test
    void ventilatorDoesNotDuplicateScalarDataIntoThreeChannels() {
        FastMedicalModel model = new FastMedicalModel("Scalar", 16);
        for (int i = 0; i < 3; i++) model.setPoint(x(i), point(i, 0).y);
        assertEquals(List.of(points(0, 3, 0)), render(new VentilatorWaveformRenderer(), model, CONTEXT).lines);
    }

    @Test
    void missingVentilatorChannelValueBreaksOnlyThatChannel() {
        CircularFastMedicalModel model = medicalModel(64, 6);
        model.getRawChannelArray(0)[2] = Double.NaN;
        assertEquals(List.of(points(0, 2, 0), points(3, 6, 0), points(0, 6, 1)),
                render(new VentilatorWaveformRenderer(), model, CONTEXT).lines);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 4, 6})
    void ultrasoundUsesValidSamplesInLogicalOrder(int writes) {
        List<List<Point>> expected = IntStream.range(Math.max(0, writes - 4), writes)
                .mapToObj(i -> List.of(new Point(x(i), 0), point(i, 0))).toList();
        assertEquals(expected,
                render(new UltrasoundMModeRenderer(), medicalModel(4, writes), CONTEXT).lines);
    }

    @Test
    void ultrasoundRequiresDepthAndIntensityAndSkipsInvalidIntensity() {
        CircularFastMedicalModel singleChannel = new CircularFastMedicalModel(64, 1);
        singleChannel.add(123, new double[]{456});
        singleChannel.add(124, new double[]{457});
        assertTrue(render(new UltrasoundMModeRenderer(), singleChannel, CONTEXT).lines.isEmpty());
        FastMedicalModel scalar = new FastMedicalModel("No intensity", 16);
        scalar.setPoint(123, 456);
        scalar.setPoint(124, 457);
        assertTrue(render(new UltrasoundMModeRenderer(), scalar, CONTEXT).lines.isEmpty());
        CircularFastMedicalModel model = medicalModel(64, 3);
        model.getRawChannelArray(1)[1] = Double.POSITIVE_INFINITY;
        assertEquals(List.of(List.of(new Point(x(0), 0), point(0, 0)),
                        List.of(new Point(x(2), 0), point(2, 0))),
                render(new UltrasoundMModeRenderer(), model, CONTEXT).lines);
    }

    @Test
    void intensityColumnsUseTheIntensityChannelIndependentlyOfDepth() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(64, 2);
        model.add(10, new double[]{1000, 0});
        model.add(20, new double[]{1000, 128});
        model.add(30, new double[]{1000, 255});
        for (ChartRenderer renderer : List.of(new SpectrogramMedicalRenderer(), new UltrasoundMModeRenderer())) {
            RecordingCanvas canvas = render(renderer, model, CONTEXT);
            assertEquals(3, canvas.lines.size());
            assertEquals(3, canvas.lineColors.stream().distinct().count(),
                    "Equal depths with different intensities must produce different colors");
        }
    }

    private static CircularFastMedicalModel medicalModel(int capacity, int writes) {
        CircularFastMedicalModel model = new CircularFastMedicalModel(capacity, 2);
        appendSamples(model, 0, writes);
        return model;
    }

    private static void appendSamples(CircularFastMedicalModel model, int from, int until) {
        for (int i = from; i < until; i++) model.add(x(i), new double[]{1000 + i, 2000 + i});
    }

    private static CircularFastMedicalModel eegModel(int capacity, int writes) {
        CircularFastMedicalModel model = new CircularFastMedicalModel(capacity, 3);
        for (int i = 0; i < writes; i++) model.add(x(i), eegValues(i));
        return model;
    }

    private static double[] eegValues(int i) {
        return new double[]{-3 + 0.2 * i, 0.2 * i, 3 + 0.2 * i};
    }

    private static double x(int sample) {
        return 100 + 10 * sample;
    }

    private static Point point(int sample, int channel) {
        return new Point(x(sample), 1000 * (channel + 1) + sample);
    }

    private static List<Point> points(int from, int until, int channel) {
        return IntStream.range(from, until).mapToObj(i -> point(i, channel)).toList();
    }

    private static List<Point> eegPoints(int from, int until, int channel) {
        return IntStream.range(from, until)
                .mapToObj(i -> new Point(x(i), -6 + 6 * channel + eegValues(i)[channel] / 3)).toList();
    }

    private static void assertEegChannels(RecordingCanvas canvas, int from, int until) {
        assertEquals(3, canvas.lines.size());
        for (int channel = 0; channel < 3; channel++) {
            assertLine(canvas.lines.get(channel), eegPoints(from, until, channel));
        }
    }

    private static void assertLine(List<Point> actual, List<Point> expected) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).x, actual.get(i).x, 0.00001, "X at sample " + i);
            assertEquals(expected.get(i).y, actual.get(i).y, 0.00001, "Y at sample " + i);
        }
    }

    private static RecordingCanvas render(ChartRenderer renderer, ChartModel model, PlotContext context) {
        RecordingCanvas canvas = new RecordingCanvas();
        renderer.render(canvas, model, context);
        return canvas;
    }

    private record SweepCase(String name, Supplier<ChartRenderer> factory, int gap) {
        @Override
        public String toString() {
            return name;
        }
    }

    private record Point(double x, double y) {}

    private record IdentityContext(double minY, double maxY, ChartTheme theme) implements PlotContext {
        private IdentityContext(double minY, double maxY) {
            this(minY, maxY, ChartThemes.getDarkTheme());
        }

        @Override public ChartTheme getTheme() { return theme; }
        @Override public double getMinX() { return 0; }
        @Override public double getMaxX() { return 4096; }
        @Override public double getMinY() { return minY; }
        @Override public double getMaxY() { return maxY; }
        @Override public ArberRect getPlotBounds() { return new ArberRect(0, 0, 4096, 4096); }
        @Override public void mapToPixel(double x, double y, double[] out) { out[0] = x; out[1] = y; }
        @Override public void mapToData(double x, double y, double[] out) { out[0] = x; out[1] = y; }
    }

    private static final class RecordingCanvas implements ArberCanvas {
        private final List<List<Point>> lines = new ArrayList<>();
        private final List<ArberColor> lineColors = new ArrayList<>();
        private final List<Point> markers = new ArrayList<>();
        private ArberColor color;
        private ArberRect clip;

        @Override
        public void drawPolyline(float[] xs, float[] ys, int count) {
            assertTrue(count >= 2, "A line requires at least two vertices");
            List<Point> points = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                assertTrue(Float.isFinite(xs[i]) && Float.isFinite(ys[i]));
                points.add(new Point(xs[i], ys[i]));
            }
            lines.add(points);
            lineColors.add(color);
        }

        @Override
        public void fillRect(float x, float y, float width, float height) {
            assertEquals(6f, width);
            assertEquals(6f, height);
            markers.add(new Point(x + width / 2, y + height / 2));
        }

        @Override public void setColor(ArberColor color) { this.color = color; }
        @Override public void setStroke(float width) {}
        @Override public void moveTo(float x, float y) { throw new AssertionError("Unexpected moveTo"); }
        @Override public void lineTo(float x, float y) { throw new AssertionError("Unexpected lineTo"); }
        @Override public void drawRect(float x, float y, float w, float h) { throw new AssertionError("Unexpected rectangle"); }
        @Override public void fillPolygon(float[] xs, float[] ys, int count) { throw new AssertionError("Unexpected polygon"); }
        @Override public void drawVoxelField(VoxelBuffer buffer) { throw new AssertionError("Unexpected voxels"); }
        @Override public ArberMatrix getTransform() { return null; }
        @Override public void setClip(ArberRect clip) { this.clip = clip; }
        @Override public ArberRect getClip() { return clip; }
    }
}
