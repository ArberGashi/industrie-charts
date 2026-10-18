package com.arbergashi.charts.render;

import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.PredictionModel;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberMatrix;
import com.arbergashi.charts.core.rendering.VoxelBuffer;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.model.ChartModel.ChartModelListener;
import com.arbergashi.charts.model.FastMedicalModel;
import com.arbergashi.charts.render.analysis.AdaptiveFunctionRenderer;
import com.arbergashi.charts.render.analysis.FourierOverlayRenderer;
import com.arbergashi.charts.render.analysis.LiveFFTRenderer;
import com.arbergashi.charts.render.analysis.ThresholdRenderer;
import com.arbergashi.charts.render.predictive.PredictiveShadowRenderer;
import com.arbergashi.charts.render.standard.AreaRenderer;
import com.arbergashi.charts.render.standard.BubbleRenderer;
import com.arbergashi.charts.render.standard.PredictivePathRenderer;
import com.arbergashi.charts.render.standard.SplineRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.NiceScale;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisStandardCorrectionTest {
    private final Map<String, String> originalProperties = new HashMap<>();

    @AfterEach
    void restoreProperties() {
        originalProperties.forEach((key, value) -> {
            if (value == null) ChartAssets.removeProperty(key);
            else ChartAssets.setProperty(key, value);
        });
    }

    @Test
    void areaFillAndOutlineBreakAtMissingSamples() {
        for (double missing : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            ArrayModel model = new ArrayModel(new double[]{0, 1, 2, 3, 4}, new double[]{1, 2, missing, 2, 1});
            RecordingCanvas canvas = new RecordingCanvas();
            new AreaRenderer().render(canvas, model, context(model, 0, 4, 0, 2));
            assertEquals(2, canvas.lines.size());
            assertEquals(2, canvas.polygons.size());
            assertArrayEquals(new float[]{0, 25}, canvas.lines.get(0).x);
            assertArrayEquals(new float[]{75, 100}, canvas.lines.get(1).x);
            assertArrayEquals(new float[]{0, 0, 25, 25}, canvas.polygons.get(0).x);
            assertArrayEquals(new float[]{100, 50, 0, 100}, canvas.polygons.get(0).y);
            assertArrayEquals(new float[]{75, 75, 100, 100}, canvas.polygons.get(1).x);
            assertArrayEquals(new float[]{100, 0, 50, 100}, canvas.polygons.get(1).y);
        }
    }

    @Test
    void areaMissingXAndLogarithmicDomainFailuresAlsoSplitThePath() {
        ArrayModel model = new ArrayModel(new double[]{0, 1, Double.NaN, 3, 4}, new double[]{1, 10, 5, 10, 1});
        RecordingCanvas canvas = new RecordingCanvas();
        AreaRenderer renderer = new AreaRenderer();
        renderer.render(canvas, model, context(model, 0, 4, 0, 10));
        assertEquals(2, canvas.polygons.size());
        model.x[2] = 2;
        model.y[2] = 0;
        canvas.clear();
        PlotContext logarithmic = new DefaultPlotContext(new ArberRect(0, 0, 100, 100), 0, 4, 1, 10,
                true, NiceScale.ScaleMode.LINEAR, NiceScale.ScaleMode.LOGARITHMIC);
        renderer.render(canvas, model, logarithmic);
        assertEquals(2, canvas.polygons.size());
        assertArrayEquals(new float[]{0, 25}, canvas.lines.get(0).x);
        assertArrayEquals(new float[]{75, 100}, canvas.lines.get(1).x);
    }

    @Test
    void isolatedAreaSamplesCannotCreateFilledBridgesOrReuseOldGeometry() {
        ArrayModel model = new ArrayModel(new double[]{0, 1, 2, 3, 4}, new double[]{1, 2, 3, 2, 1});
        RecordingCanvas canvas = new RecordingCanvas();
        AreaRenderer renderer = new AreaRenderer();
        renderer.render(canvas, model, context(model, 0, 4, 0, 3));
        assertEquals(1, canvas.polygons.size());
        model.y[1] = model.y[3] = Double.NaN;
        canvas.clear();
        renderer.render(canvas, model, context(model, 0, 4, 0, 3));
        assertTrue(canvas.polygons.isEmpty());
        assertTrue(canvas.lines.isEmpty());
        Arrays.fill(model.y, Double.NaN);
        renderer.render(canvas, model, context(model, 0, 4, 0, 3));
        assertTrue(canvas.polygons.isEmpty());
        assertTrue(canvas.lines.isEmpty());
    }

    @Test
    void predictiveBarMapsItsDataCoordinatesExactlyOnce() {
        configure("Chart.predictive.global.smoothing", "0");
        configure("Chart.predictive.global.lookahead", "1");
        ArrayModel model = new ArrayModel(new double[]{1, 2}, new double[]{2, 4});
        RecordingCanvas canvas = new RecordingCanvas();
        new PredictivePathRenderer(PredictivePathRenderer.Style.BAR).render(canvas, model,
                context(model, 0, 10, 0, 10));
        assertEquals(1, canvas.rectangles.size());
        assertArrayEquals(new float[]{26.5f, 40f, 7f, 60f}, canvas.rectangles.getFirst().geometry, 0.001f);
    }

    @Test
    void predictiveBarSupportsDecreasingXAndRejectsNonfiniteSamples() {
        configure("Chart.predictive.global.smoothing", "0");
        configure("Chart.predictive.global.lookahead", "1");
        ArrayModel model = new ArrayModel(new double[]{3, 2}, new double[]{2, 4});
        RecordingCanvas canvas = new RecordingCanvas();
        new PredictivePathRenderer(PredictivePathRenderer.Style.BAR).render(canvas, model,
                context(model, 0, 10, 0, 10));
        assertEquals(7f, canvas.rectangles.getFirst().geometry[2], 0.001f);
        model.y[1] = Double.NaN;
        canvas.clear();
        new PredictivePathRenderer(PredictivePathRenderer.Style.BAR).render(canvas, model,
                context(model, 0, 10, 0, 10));
        assertTrue(canvas.rectangles.isEmpty());
    }

    @Test
    void predictionCacheTracksModelIdentityCountStampAndMutableViewport() {
        AtomicInteger calls = new AtomicInteger();
        PredictionModel predictor = (model, context, out) -> {
            calls.incrementAndGet();
            int n = model.getPointCount();
            if (n == 0) return;
            out.ensureCapacity(1);
            out.x()[0] = model.getX(n - 1) + 1;
            out.y()[0] = model.getY(n - 1);
            out.confidence()[0] = 1;
            out.setCount(1);
        };
        PredictiveShadowRenderer renderer = new PredictiveShadowRenderer(predictor);
        ArrayModel first = new ArrayModel(new double[]{0, 1, 2}, new double[]{1, 2, 3});
        first.count = 2;
        DefaultPlotContext context = context(first, 0, 10, 0, 10);
        renderer.ensurePredicted(first, context);
        renderer.ensurePredicted(first, context);
        assertEquals(1, calls.get(), "Unchanged stamped input should reuse predictions");
        ArrayModel second = new ArrayModel(new double[]{0, 1}, new double[]{4, 5});
        renderer.ensurePredicted(second, context);
        assertEquals(5, renderer.predictedForX(2));
        renderer.ensurePredicted(first, context);
        first.count = 3;
        renderer.ensurePredicted(first, context);
        assertEquals(3, renderer.predictedForX(3));
        first.stamp++;
        renderer.ensurePredicted(first, context);
        context.setMaxX(20);
        renderer.ensurePredicted(first, context);
        assertEquals(6, calls.get());
        first.count = 0;
        renderer.ensurePredicted(first, context);
        assertTrue(Double.isNaN(renderer.predictedForX(3)), "Clearing data must clear stale audit points");
    }

    @Test
    void predictionCacheNeverAssumesZeroStampMeansUnchangedData() {
        AtomicInteger calls = new AtomicInteger();
        PredictiveShadowRenderer renderer = new PredictiveShadowRenderer((model, context, out) -> {
            calls.incrementAndGet();
            int last = model.getPointCount() - 1;
            out.ensureCapacity(1);
            out.x()[0] = model.getX(last) + 1;
            out.y()[0] = model.getY(last);
            out.setCount(1);
        });
        FastMedicalModel model = new FastMedicalModel("unstamped");
        model.setPoint(0, 1);
        model.setPoint(1, 2);
        DefaultPlotContext context = context(model, 0, 5, 0, 10);
        renderer.ensurePredicted(model, context);
        model.clear();
        model.setPoint(0, 4);
        model.setPoint(1, 7);
        renderer.ensurePredicted(model, context);
        assertEquals(2, calls.get());
        assertEquals(7, renderer.predictedForX(2));
    }

    @Test
    void predictionShadowNeverConnectsAcrossUndefinedForecastPoints() {
        PredictiveShadowRenderer renderer = new PredictiveShadowRenderer((model, context, out) -> {
            out.ensureCapacity(4);
            for (int i = 0; i < 4; i++) {
                out.x()[i] = i + 2;
                out.y()[i] = i == 1 ? Double.NaN : i + 2;
            }
            out.setCount(4);
        });
        ArrayModel model = new ArrayModel(new double[]{0, 1}, new double[]{0, 1});
        RecordingCanvas canvas = new RecordingCanvas();
        renderer.render(canvas, model, identityContext());
        assertEquals(2, canvas.lines.size());
        assertArrayEquals(new float[]{1.5f, 2.5f}, canvas.lines.getFirst().x);
        assertArrayEquals(new float[]{4.5f, 5.5f}, canvas.lines.getLast().x);
    }

    @Test
    void predictivePathUsesTheNewestLogicalRingBufferSamples() {
        configure("Chart.predictive.global.smoothing", "0");
        configure("Chart.predictive.global.lookahead", "1");
        RecordingCanvas canvas = new RecordingCanvas();
        new PredictivePathRenderer().render(canvas, wrappedSignal(), identityContext());
        assertArrayEquals(new float[]{5, 6}, canvas.lines.getFirst().x);
        assertArrayEquals(new float[]{1, 2}, canvas.lines.getFirst().y);
    }

    @Test
    void thresholdHonorsConfiguredAlpha() {
        configure("chart.render.threshold.y", "5");
        configure("chart.render.threshold.alpha", "0.25");
        configure("chart.render.threshold.line", "false");
        ArrayModel model = new ArrayModel(new double[]{0, 1}, new double[]{0, 1});
        RecordingCanvas canvas = new RecordingCanvas();
        new ThresholdRenderer().render(canvas, model, context(model, 0, 10, 0, 10));
        assertEquals(63, canvas.rectangles.getFirst().color.alpha());
        assertArrayEquals(new float[]{0, 0, 100, 50}, canvas.rectangles.getFirst().geometry);
    }

    @Test
    void thresholdRespectsAnInvertedYAxis() {
        configure("chart.render.threshold.y", "2");
        configure("chart.render.threshold.mode", "above");
        configure("chart.render.threshold.line", "false");
        ArrayModel model = new ArrayModel(new double[]{0, 1}, new double[]{0, 1});
        DefaultPlotContext inverted = new DefaultPlotContext(new ArberRect(0, 0, 100, 100),
                0, 10, 0, 10, false, false, true, null, null, null, null, null, null);
        RecordingCanvas canvas = new RecordingCanvas();
        new ThresholdRenderer().render(canvas, model, inverted);
        assertArrayEquals(new float[]{0, 20, 100, 80}, canvas.rectangles.getFirst().geometry);
    }

    @Test
    void bubbleUsesExplicitWeightsAndPreservesAreaRatios() {
        ArrayModel model = new ArrayModel(new double[]{2, 5, 8}, new double[]{5, 5, 5});
        model.weight = new double[]{1, 4, 0};
        RecordingCanvas canvas = new RecordingCanvas();
        new BubbleRenderer().render(canvas, model, context(model, 0, 10, 0, 10));
        assertEquals(2, canvas.polygons.size());
        double firstWidth = width(canvas.polygons.get(0).x);
        double secondWidth = width(canvas.polygons.get(1).x);
        assertEquals(4, secondWidth * secondWidth / (firstWidth * firstWidth), 1e-6);
    }

    @Test
    void bubbleSkipsInvalidWeightsCoordinatesAndUnusedBackingCapacity() {
        ArrayModel model = new ArrayModel(new double[]{1, 2, 3, 4, Double.NaN, 6, 7},
                new double[]{5, 5, 5, 5, 5, 5, 5});
        model.weight = new double[]{1, -1, Double.NaN, Double.POSITIVE_INFINITY, 2, 0, 99};
        model.count = 6;
        RecordingCanvas canvas = new RecordingCanvas();
        new BubbleRenderer().render(canvas, model, context(model, 0, 10, 0, 10));
        assertEquals(1, canvas.polygons.size());
        assertEquals(50, width(canvas.polygons.getFirst().x), 1e-4);
    }

    @Test
    void splineInterpolatesReferenceCurveWithoutOvershoot() {
        ArrayModel model = new ArrayModel(new double[]{0, 1, 2}, new double[]{0, 1, 0});
        RecordingCanvas canvas = new RecordingCanvas();
        new SplineRenderer().render(canvas, model, identityContext());
        assertEquals(2, canvas.lines.size());
        Path first = canvas.lines.getFirst();
        assertTrue(first.x.length > 2);
        assertEquals(0.5, first.x[4], 1e-6);
        assertEquals(0.75, first.y[4], 1e-6, "PCHIP endpoint slope is 2 and the peak slope is 0");
        for (Path path : canvas.lines) {
            for (float y : path.y) assertTrue(y >= 0 && y <= 1);
        }
        assertEquals(0, first.x[0]);
        assertEquals(1, first.y[first.y.length - 1]);
    }

    @Test
    void splineSeparatesNonfiniteGapsAndBoundsTheLogicalSize() {
        ArrayModel model = new ArrayModel(new double[]{0, 1, 2, 3, 4, 100},
                new double[]{0, 1, Double.NaN, 1, 0, 100});
        model.count = 5;
        RecordingCanvas canvas = new RecordingCanvas();
        new SplineRenderer().render(canvas, model, identityContext());
        assertEquals(2, canvas.lines.size());
        assertEquals(1, canvas.lines.getFirst().x[canvas.lines.getFirst().x.length - 1]);
        assertEquals(3, canvas.lines.getLast().x[0]);
        assertEquals(4, canvas.lines.getLast().x[canvas.lines.getLast().x.length - 1]);
    }

    @Test
    void splineSupportsDescendingXAndNeverBridgesRepeatedX() {
        ArrayModel model = new ArrayModel(new double[]{2, 1, 0, 0, -1}, new double[]{0, 1, 0, 4, 3});
        RecordingCanvas canvas = new RecordingCanvas();
        new SplineRenderer().render(canvas, model, identityContext());
        assertEquals(3, canvas.lines.size());
        assertEquals(0.75, canvas.lines.getFirst().y[4], 1e-6);
        assertEquals(4, canvas.lines.getLast().y[0]);
    }

    @Test
    void splineConnectsEveryLogicalRingBufferIntervalInOrder() {
        RecordingCanvas canvas = new RecordingCanvas();
        new SplineRenderer().render(canvas, wrappedSignal(), identityContext());
        assertEquals(3, canvas.lines.size());
        double[] values = {0, -1, 0, 1};
        for (int i = 0; i < 3; i++) {
            Path path = canvas.lines.get(i);
            assertEquals(i + 2, path.x[0]);
            assertEquals(i + 3, path.x[path.x.length - 1]);
            assertEquals(values[i], path.y[0]);
            assertEquals(values[i + 1], path.y[path.y.length - 1]);
        }
    }

    @Test
    void fftMatchesIndependentDftForPaddedAndPowerOfTwoSignals() {
        for (double[] signal : new double[][]{{1}, {1, 0, 0, 0}, {1, -2, 3, 4, -5},
                {3, 3, 3, 3, 3, 3, 3, 3}, {0, 1, 0, -1, 0, 1, 0, -1}}) {
            double[] original = signal.clone();
            assertArrayEquals(referenceSpectrum(signal), LiveFFTRenderer.getComputedFFT(signal), 1e-10);
            assertArrayEquals(original, signal, "FFT must preserve caller input");
        }
    }

    @Test
    void fftHasDefinedEmptyAndInvalidInputBehavior() {
        assertEquals(0, LiveFFTRenderer.getComputedFFT(new double[0]).length);
        assertThrows(NullPointerException.class, () -> LiveFFTRenderer.getComputedFFT(null));
        assertThrows(IllegalArgumentException.class,
                () -> LiveFFTRenderer.getComputedFFT(new double[]{1, Double.NaN}));
        assertThrows(IllegalArgumentException.class,
                () -> LiveFFTRenderer.getComputedFFT(new double[]{Double.POSITIVE_INFINITY}));
    }

    @Test
    void fftRendererTransformsTimeSamplesAndPublishesFrequencyRanges() {
        ArrayModel model = new ArrayModel(new double[]{10, 10.25, 10.5, 10.75, 11, 11.25, 11.5, 11.75},
                new double[]{0, 2, 0, -2, 0, 2, 0, -2});
        LiveFFTRenderer renderer = new LiveFFTRenderer().setAsBars(false);
        assertArrayEquals(new double[]{0, 2}, renderer.getPreferredXRange(model), 1e-10);
        assertArrayEquals(new double[]{0, 2.1}, renderer.getPreferredYRange(model), 1e-10);
        RecordingCanvas canvas = new RecordingCanvas();
        renderer.render(canvas, model, identityContext());
        assertEquals(5, canvas.lines.getFirst().x.length);
        assertEquals(1, canvas.lines.getFirst().x[2]);
        assertEquals(2, canvas.lines.getFirst().y[2], 1e-5);
    }

    @Test
    void fftTransformsTheLogicalRingBufferSignal() {
        LiveFFTRenderer renderer = new LiveFFTRenderer().setAsBars(false);
        CircularFastMedicalModel model = wrappedSignal();
        assertArrayEquals(new double[]{0, 0.5}, renderer.getPreferredXRange(model), 1e-12);
        RecordingCanvas canvas = new RecordingCanvas();
        renderer.render(canvas, model, identityContext());
        assertArrayEquals(new float[]{0, 0.25f, 0.5f}, canvas.lines.getFirst().x);
        assertArrayEquals(new float[]{0, 1, 0}, canvas.lines.getFirst().y, 1e-6f);
    }

    @Test
    void spectralSamplingRejectsDuplicateReversedAndUnevenLargeTimestamps() {
        double origin = 1e16;
        for (double[] times : new double[][]{
                {origin, origin + 2, origin + 2, origin + 4},
                {origin, origin + 2, origin, origin + 4},
                {origin, origin + 2, origin + 6, origin + 8}}) {
            ArrayModel model = new ArrayModel(times, new double[]{0, 1, 0, -1});
            RecordingCanvas fft = new RecordingCanvas();
            new LiveFFTRenderer().setAsBars(false).render(fft, model, identityContext());
            assertTrue(fft.lines.isEmpty());
            RecordingCanvas overlay = new RecordingCanvas();
            new FourierOverlayRenderer().render(overlay, model, identityContext());
            assertTrue(overlay.lines.isEmpty());
        }
        ArrayModel uniform = new ArrayModel(new double[]{origin, origin + 2, origin + 4, origin + 6},
                new double[]{0, 1, 0, -1});
        assertArrayEquals(new double[]{0, 0.25}, new LiveFFTRenderer().getPreferredXRange(uniform), 1e-12);
    }

    @Test
    void spectralSamplingAllowsRepresentationalRoundingOfEpochTimes() {
        double[] x = new double[8];
        double[] y = new double[8];
        for (int i = 0; i < x.length; i++) {
            x[i] = 1_700_000_000.0 + i * 0.001;
            y[i] = Math.sin(2 * Math.PI * i / 8);
        }
        ArrayModel model = new ArrayModel(x, y);
        RecordingCanvas canvas = new RecordingCanvas();
        new LiveFFTRenderer().setAsBars(false).render(canvas, model, identityContext());
        assertEquals(1, canvas.lines.size());
        assertEquals(125, canvas.lines.getFirst().x[1], 0.01);
        assertEquals(1, canvas.lines.getFirst().y[1], 1e-6);
    }

    @Test
    void fftSpectrumModeUsesHostTransformsAndSkipsInvalidBins() {
        ArrayModel model = new ArrayModel(new double[]{0, 1, 2, 3, 4}, new double[]{1, 2, Double.NaN, 2, 1});
        LiveFFTRenderer renderer = new LiveFFTRenderer().setSpectrumInput(true).setAsBars(false);
        RecordingCanvas canvas = new RecordingCanvas();
        renderer.render(canvas, model, context(model, 0, 10, 0, 10));
        assertEquals(2, canvas.lines.size());
        assertArrayEquals(new float[]{0, 10}, canvas.lines.getFirst().x);
        assertArrayEquals(new float[]{90, 80}, canvas.lines.getFirst().y);
        assertTrue(canvas.stroke > 0);
        canvas.clear();
        renderer.render(canvas, model, context(model, 0, 5, 0, 5));
        assertArrayEquals(new float[]{0, 20}, canvas.lines.getFirst().x);
    }

    @Test
    void fftRejectsNonuniformSamplesAndIgnoresBackingTail() {
        ArrayModel model = new ArrayModel(new double[]{0, 1, 2, 3, 999}, new double[]{0, 1, 0, -1, 999});
        model.count = 4;
        LiveFFTRenderer renderer = new LiveFFTRenderer().setAsBars(false);
        RecordingCanvas canvas = new RecordingCanvas();
        renderer.render(canvas, model, identityContext());
        assertEquals(3, canvas.lines.getFirst().x.length);
        assertEquals(1, canvas.lines.getFirst().y[1], 1e-6);
        model.x[2] = 2.5;
        canvas.clear();
        renderer.render(canvas, model, identityContext());
        assertTrue(canvas.lines.isEmpty());
    }

    @Test
    void fftBarsStayWithinThePlotAtDcAndNyquist() {
        ArrayModel model = new ArrayModel(new double[]{0, 1, 2}, new double[]{1, 2, 1});
        RecordingCanvas canvas = new RecordingCanvas();
        new LiveFFTRenderer().setSpectrumInput(true).render(canvas, model, context(model, 0, 2, 0, 3));
        assertEquals(3, canvas.rectangles.size());
        for (Rectangle rectangle : canvas.rectangles) {
            assertTrue(rectangle.geometry[0] >= 0);
            assertTrue(rectangle.geometry[0] + rectangle.geometry[2] <= 100);
        }
    }

    @Test
    void fourierOverlayFitsMeasuredConstantAndSinusoidalSignals() {
        double[] x = new double[32];
        double[] y = new double[32];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
            y[i] = 3 + 2 * Math.sin(2 * Math.PI * i / x.length);
        }
        ArrayModel model = new ArrayModel(x, y);
        RecordingCanvas canvas = new RecordingCanvas();
        FourierOverlayRenderer renderer = new FourierOverlayRenderer();
        renderer.render(canvas, model, identityContext());
        Path path = canvas.lines.getFirst();
        for (int i = 0; i < path.x.length; i++) {
            assertEquals(3 + 2 * Math.sin(2 * Math.PI * path.x[i] / 32), path.y[i], 2e-6);
        }
        Arrays.fill(y, 0);
        canvas.clear();
        renderer.render(canvas, model, identityContext());
        assertNotEquals(path.y[0], canvas.lines.getFirst().y[0]);
        for (float value : canvas.lines.getFirst().y) assertEquals(0, value, 1e-6);
    }

    @Test
    void fourierOverlayFitsLogicalRingBufferOrder() {
        RecordingCanvas canvas = new RecordingCanvas();
        new FourierOverlayRenderer().render(canvas, wrappedSignal(), identityContext());
        Path path = canvas.lines.getFirst();
        assertEquals(2, path.x[0]);
        assertEquals(5, path.x[path.x.length - 1]);
        for (int i = 0; i < path.x.length; i++) {
            assertEquals(-Math.sin(2 * Math.PI * (path.x[i] - 2) / 4), path.y[i], 1e-6);
        }
    }

    @Test
    void fourierOverlayRejectsUndefinedAndNonuniformSamples() {
        ArrayModel model = new ArrayModel(new double[]{0, 1, 2, 3}, new double[]{0, 1, Double.NaN, -1});
        RecordingCanvas canvas = new RecordingCanvas();
        FourierOverlayRenderer renderer = new FourierOverlayRenderer();
        renderer.render(canvas, model, identityContext());
        assertTrue(canvas.lines.isEmpty());
        model.y[2] = 0;
        model.x[3] = 5;
        renderer.render(canvas, model, identityContext());
        assertTrue(canvas.lines.isEmpty());
    }

    @Test
    void adaptiveFunctionSeparatesUndefinedIntervals() {
        ArrayModel model = new ArrayModel(new double[0], new double[0]);
        RecordingCanvas canvas = new RecordingCanvas();
        try (AdaptiveFunctionRenderer renderer = new AdaptiveFunctionRenderer(
                x -> Math.abs(x) < 0.25 ? Double.NaN : x * x)) {
            renderer.render(canvas, model, context(model, -1, 1, 0, 1));
        }
        assertEquals(2, canvas.lines.size());
        assertTrue(canvas.lines.getFirst().x[canvas.lines.getFirst().x.length - 1] <= 37.5);
        assertTrue(canvas.lines.getLast().x[0] >= 62.5);
    }

    @Test
    void adaptiveFunctionDetectsMutationOfAnExistingContext() throws Exception {
        ArrayModel model = new ArrayModel(new double[0], new double[0]);
        DefaultPlotContext context = context(model, 0, 1, 0, 5);
        RecordingCanvas canvas = new RecordingCanvas();
        try (AdaptiveFunctionRenderer renderer = new AdaptiveFunctionRenderer(x -> x)) {
            renderer.render(canvas, model, context);
            CountDownLatch refreshed = new CountDownLatch(1);
            renderer.setRepaintCallback(refreshed::countDown);
            context.setMinX(2).setMaxX(3);
            renderer.render(canvas, model, context);
            assertTrue(refreshed.await(2, TimeUnit.SECONDS), "Mutating the same context must resample");
            canvas.clear();
            renderer.render(canvas, model, context);
            assertEquals(0, canvas.lines.getFirst().x[0], 1e-5);
            assertEquals(60, canvas.lines.getFirst().y[0], 1e-5);
            assertFalse(canvas.lines.isEmpty());
        }
    }

    private void configure(String key, String value) {
        if (!originalProperties.containsKey(key)) originalProperties.put(key, ChartAssets.getString(key, null));
        ChartAssets.setProperty(key, value);
    }

    private static CircularFastMedicalModel wrappedSignal() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(4, 1);
        double[] values = {0, 1, 0, -1, 0, 1};
        for (int i = 0; i < values.length; i++) model.add(i, new double[]{values[i]});
        return model;
    }

    private static DefaultPlotContext context(ChartModel model, double minX, double maxX, double minY, double maxY) {
        return new DefaultPlotContext(new ArberRect(0, 0, 100, 100), model, minX, maxX, minY, maxY);
    }

    private static PlotContext identityContext() {
        return new PlotContext() {
            public double getMinX() { return 0; }
            public double getMaxX() { return 100; }
            public double getMinY() { return 0; }
            public double getMaxY() { return 100; }
            public ArberRect getPlotBounds() { return new ArberRect(-100, -100, 200, 200); }
            public void mapToPixel(double x, double y, double[] out) { out[0] = x; out[1] = y; }
            public void mapToData(double x, double y, double[] out) { out[0] = x; out[1] = y; }
        };
    }

    private static double width(float[] values) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (float value : values) { min = Math.min(min, value); max = Math.max(max, value); }
        return max - min;
    }

    private static double[] referenceSpectrum(double[] signal) {
        int length = 1;
        while (length < signal.length) length *= 2;
        double[] result = new double[length / 2 + 1];
        for (int bin = 0; bin < result.length; bin++) {
            double real = 0;
            double imaginary = 0;
            for (int sample = 0; sample < signal.length; sample++) {
                double angle = -2 * Math.PI * bin * sample / length;
                real += signal[sample] * Math.cos(angle);
                imaginary += signal[sample] * Math.sin(angle);
            }
            double factor = bin == 0 || bin == length / 2 ? 1 : 2;
            result[bin] = Math.hypot(real, imaginary) * factor / signal.length;
        }
        return result;
    }

    private static final class ArrayModel implements ChartModel {
        final double[] x;
        final double[] y;
        double[] weight;
        int count;
        long stamp = 1;
        ArrayModel(double[] x, double[] y) { this.x = x; this.y = y; weight = y; count = x.length; }
        public String getName() { return "correction fixture"; }
        public int getPointCount() { return count; }
        public double[] getXData() { return x; }
        public double[] getYData() { return y; }
        public double[] getWeightData() { return weight; }
        public long getUpdateStamp() { return stamp; }
        public void setChangeListener(ChartModelListener listener) { }
        public void removeChangeListener(ChartModelListener listener) { }
    }

    private record Path(float[] x, float[] y) { }
    private record Rectangle(float[] geometry, ArberColor color) { }

    private static final class RecordingCanvas implements ArberCanvas {
        final List<Path> lines = new ArrayList<>();
        final List<Path> polygons = new ArrayList<>();
        final List<Rectangle> rectangles = new ArrayList<>();
        ArberColor color;
        ArberRect clip;
        float stroke;
        void clear() { lines.clear(); polygons.clear(); rectangles.clear(); }
        public void setColor(ArberColor color) { this.color = color; }
        public void setStroke(float width) { stroke = width; }
        public void moveTo(float x, float y) { }
        public void lineTo(float x, float y) { }
        public void drawPolyline(float[] xs, float[] ys, int count) {
            lines.add(new Path(Arrays.copyOf(xs, count), Arrays.copyOf(ys, count)));
        }
        public void drawRect(float x, float y, float w, float h) { }
        public void fillRect(float x, float y, float w, float h) {
            rectangles.add(new Rectangle(new float[]{x, y, w, h}, color));
        }
        public void fillPolygon(float[] xs, float[] ys, int count) {
            polygons.add(new Path(Arrays.copyOf(xs, count), Arrays.copyOf(ys, count)));
        }
        public void drawVoxelField(VoxelBuffer buffer) { }
        public ArberMatrix getTransform() { return null; }
        public void setClip(ArberRect clip) { this.clip = clip; }
        public ArberRect getClip() { return clip; }
    }
}
