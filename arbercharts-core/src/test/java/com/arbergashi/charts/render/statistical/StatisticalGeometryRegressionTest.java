package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.rendering.ArberMatrices;
import com.arbergashi.charts.core.rendering.ArberMatrix;
import com.arbergashi.charts.core.rendering.VoxelBuffer;
import com.arbergashi.charts.model.BoxPlotOutlierModel;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.model.DefaultStatisticalChartModel;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.NiceScale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticalGeometryRegressionTest {
    private static final double EPSILON = 0.0001;

    @Test
    void boxPlotPreservesExplicitQuartilesMedianWhiskersAndOutliersForEachBox() {
        OutlierSummary model = new OutlierSummary();
        model.setBoxPlot(1, 50, 20, 60, 0, 100, "Asymmetric");
        model.setBoxPlot(2, 40, 30, 70, 10, 90, "Second");
        Geometry canvas = render(new BoxPlotRenderer(), model, context(model, 0, 3, 0, 100));

        assertEquals(2, canvas.outlines.size());
        assertRect(canvas.outlines.get(0), 400.0 / 3 - 15, 160, 30, 160);
        assertRect(canvas.outlines.get(1), 800.0 / 3 - 15, 120, 30, 160);
        assertLine(canvas.lines.get(0), 400.0 / 3, 0, 400.0 / 3, 160);
        assertLine(canvas.lines.get(1), 400.0 / 3, 400, 400.0 / 3, 320);
        assertEquals(200, canvas.lines.get(4).y[0], EPSILON);
        assertLine(canvas.lines.get(5), 800.0 / 3, 40, 800.0 / 3, 120);
        assertLine(canvas.lines.get(6), 800.0 / 3, 360, 800.0 / 3, 280);
        assertEquals(240, canvas.lines.get(9).y[0], EPSILON);
        List<Rect> outliers = canvas.fills.stream().filter(rect -> rect.w < 10).toList();
        assertEquals(3, outliers.size());
        assertEquals(440, outliers.get(0).centerY(), EPSILON);
        assertEquals(-40, outliers.get(1).centerY(), EPSILON);
        assertEquals(20, outliers.get(2).centerY(), EPSILON);
    }

    @Test
    void genericBoxPlotRetainsItsSymmetricIqrFallbackButPlacesTheMedianCorrectly() {
        DefaultChartModel model = new DefaultChartModel();
        model.setPoint(1, 50, 0, 100, 40, "Fallback");
        Geometry canvas = render(new BoxPlotRenderer(), model, context(model, 0, 2, 0, 100));
        assertRect(canvas.outlines.getFirst(), 185, 120, 30, 160);
        assertEquals(200, canvas.lines.get(4).y[0], EPSILON);
    }

    @Test
    void summaryPreferredRangesIncludeWhiskersAndOutliersBeyondTheMedianValues() {
        OutlierSummary model = new OutlierSummary();
        model.setBoxPlot(1, 50, 20, 60, 0, 100, "Summary");
        assertArrayEquals(new double[]{-10, 110}, new BoxPlotRenderer().getPreferredYRange(model), EPSILON);
        assertArrayEquals(new double[]{0, 100}, new ViolinPlotRenderer().getPreferredYRange(model), EPSILON);
    }

    @Test
    void violinCentralMarkerUsesTheSuppliedCenterInsteadOfTheUpperExtreme() {
        DefaultStatisticalChartModel model = new DefaultStatisticalChartModel();
        model.setBoxPlot(1, 50, 20, 60, 0, 100, "Summary");
        Geometry canvas = render(new ViolinPlotRenderer(), model, context(model, 0, 2, 0, 100));
        assertEquals(200, canvas.fills.getFirst().centerY(), EPSILON);
        assertEquals(200, canvas.fills.getFirst().centerX(), EPSILON);
        assertEquals(0, minimum(canvas.polygons.getFirst().y), EPSILON);
        assertEquals(400, maximum(canvas.polygons.getFirst().y), EPSILON);
    }

    @Test
    void kdeIgnoresUnusedCacheCapacityWhenSamplesGrowAndShrink() {
        KDERenderer reused = new KDERenderer();
        double[][] samples = {{20, 30}, {20, 30, 40}, {4, 9, 16, 25, 36}, {10, 15}, {7}};
        for (double[] values : samples) {
            Samples model = Samples.observations(values);
            PlotContext context = context(model, -10, 70, 0, 1);
            Geometry actual = render(reused, model, context);
            KDERenderer fresh = new KDERenderer();
            Geometry expected = render(fresh, model, context);
            assertPath(expected.lines.getFirst(), actual.lines.getFirst());
            assertArrayEquals(fresh.getPreferredYRange(model), reused.getPreferredYRange(model), 1e-12);
            assertArrayEquals(fresh.getPreferredXRange(model), reused.getPreferredXRange(model), 1e-12);
        }
    }

    @Test
    void kdeMatchesGaussianDensityAndClosesItsFillAtTheCurveEndpoints() {
        Samples model = Samples.observations(-1, 1);
        KDERenderer renderer = new KDERenderer().setBandwidth(1);
        Geometry canvas = render(renderer, model, context(model, -5, 5, 0, 1));
        Path curve = canvas.lines.getFirst();
        assertArrayEquals(new double[]{-4, 4}, renderer.getPreferredXRange(model), EPSILON);
        assertEquals(40, curve.x[0], EPSILON);
        assertEquals(360, curve.x[curve.x.length - 1], EPSILON);
        for (int i = 0; i < curve.x.length; i++) {
            double x = curve.x[i] / 40.0 - 5;
            double expected = (gaussian(x + 1) + gaussian(x - 1)) / 2;
            assertEquals(400 * (1 - expected), curve.y[i], EPSILON);
        }
        Path fill = canvas.polygons.getFirst();
        int count = curve.x.length;
        assertEquals(curve.x[count - 1], fill.x[count]);
        assertEquals(curve.x[0], fill.x[count + 1]);
        assertEquals(400, fill.y[count], EPSILON);
        assertEquals(400, fill.y[count + 1], EPSILON);
    }

    @Test
    void broadKdeRetainsTheGaussianMassBeyondTheObservedDataRange() {
        Samples model = Samples.observations(-1, 1);
        KDERenderer renderer = new KDERenderer().setBandwidth(10);
        assertArrayEquals(new double[]{-31, 31}, renderer.getPreferredXRange(model), EPSILON);
        Path curve = render(renderer, model, context(model, -40, 40, 0, 0.1)).lines.getFirst();
        double mass = 0.0;
        for (int i = 1; i < curve.x.length; i++) {
            double width = (curve.x[i] - curve.x[i - 1]) / 5.0;
            double leftDensity = (400 - curve.y[i - 1]) / 4000.0;
            double rightDensity = (400 - curve.y[i]) / 4000.0;
            mass += width * (leftDensity + rightDensity) / 2;
        }
        assertTrue(mass > 0.997 && mass < 1.001, "The visible curve must retain at least 99.7% of Gaussian mass.");
        assertEquals(gaussian(0.1) / 10, renderer.getPreferredYRange(model)[1] / 1.05, 1e-12);
    }

    @Test
    void narrowKdeIncludesTheKnownGaussianPeaksAtBothObservationCenters() {
        Samples model = Samples.observations(0, 1);
        KDERenderer renderer = new KDERenderer().setBandwidth(0.0001);
        Path curve = render(renderer, model, context(model, -0.1, 1.1, 0, 2200)).lines.getFirst();
        double expectedPeak = gaussian(0) / (2 * 0.0001);
        for (double observation : new double[]{0, 1}) {
            double expectedX = (observation + 0.1) * 400 / 1.2;
            boolean found = false;
            for (int i = 0; i < curve.x.length; i++) {
                if (Math.abs(curve.x[i] - expectedX) <= EPSILON) {
                    assertEquals(expectedPeak, (400 - curve.y[i]) * 2200 / 400.0, 0.001);
                    found = true;
                }
            }
            assertTrue(found, "Every observation center must be sampled even when the bandwidth is very small.");
        }
        assertEquals(expectedPeak, renderer.getPreferredYRange(model)[1] / 1.05, 1e-9);
    }

    @Test
    void kdeRetainsEverySeparatedPeakBeyondAConstantAnchorBudget() {
        double[] observations = new double[300];
        for (int i = 0; i < observations.length; i++) observations[i] = 17 * i + 0.01 * i * i;
        Samples model = Samples.observations(observations);
        double h = 0.001;
        double expectedPeak = gaussian(0) / (observations.length * h);
        KDERenderer renderer = new KDERenderer().setBandwidth(h);
        double maxX = observations[observations.length - 1] + 1;
        Path curve = render(renderer, model, context(model, -1, maxX, 0, expectedPeak * 1.1)).lines.getFirst();
        for (double observation : observations) {
            double pixelX = (observation + 1) * 400 / (maxX + 1);
            double peak = 0.0;
            for (int i = 0; i < curve.x.length; i++) {
                if (Math.abs(curve.x[i] - pixelX) <= EPSILON) {
                    peak = Math.max(peak, (400 - curve.y[i]) * expectedPeak * 1.1 / 400);
                }
            }
            assertEquals(expectedPeak, peak, 1e-6, "No separated observation may lose its narrow kernel peak.");
        }
        assertEquals(expectedPeak * 1.05, renderer.getPreferredYRange(model)[1], 1e-12);
    }

    @Test
    void kdeResolvesAnOverlappingModeBetweenObservationCentersDespiteADistantOutlier() {
        Samples model = Samples.observations(0.13, 0.87, 1000);
        KDERenderer renderer = new KDERenderer().setBandwidth(1);
        // The close pair is symmetric about 0.5 and separated by less than two bandwidths.
        // Its mode is therefore at 0.5; the distant kernel has underflowed there.
        double expectedPeak = 2 * gaussian(0.37) / 3;
        Path curve = render(renderer, model, context(model, -1, 1001, 0, 1)).lines.getFirst();
        double sampledPeak = (400 - minimum(curve.y)) / 400;
        assertTrue(sampledPeak >= expectedPeak * 0.992 && sampledPeak <= expectedPeak + 1e-7);
        assertTrue(renderer.getPreferredYRange(model)[1] >= expectedPeak,
                "Autoscaling must include the mode between observations, not only the observed centers.");
    }

    @Test
    void kdeRebuildsAfterSameLengthContentChangesWithoutAnUpdateStamp() {
        Samples model = Samples.observations(0, 0);
        KDERenderer renderer = new KDERenderer().setBandwidth(1);
        assertEquals(gaussian(0) * 1.05, renderer.getPreferredYRange(model)[1], 1e-12);
        model.y()[0] = -10;
        model.y()[1] = 10;
        assertEquals(0, model.getUpdateStamp());
        assertArrayEquals(new double[]{-13, 13}, renderer.getPreferredXRange(model), EPSILON);
        assertEquals(gaussian(0) * 1.05 / 2, renderer.getPreferredYRange(model)[1], 1e-12);
        Path curve = render(renderer, model, context(model, -15, 15, 0, 1)).lines.getFirst();
        assertEquals(gaussian(0) / 2, (400 - minimum(curve.y)) / 400, 1e-7);
    }

    @Test
    void kdeRebuildsForAModelWithTheSameCountAndStampAndForABandwidthChange() {
        KDERenderer renderer = new KDERenderer().setBandwidth(1);
        Samples first = Samples.observations(0, 0);
        Samples second = Samples.observations(10, 10);
        renderer.getPreferredYRange(first);
        assertArrayEquals(new double[]{7, 13}, renderer.getPreferredXRange(second), EPSILON);
        assertEquals(gaussian(0) * 1.05, renderer.getPreferredYRange(second)[1], 1e-12);
        renderer.setBandwidth(2);
        assertArrayEquals(new double[]{4, 16}, renderer.getPreferredXRange(second), EPSILON);
        assertEquals(gaussian(0) * 1.05 / 2, renderer.getPreferredYRange(second)[1], 1e-12);
    }

    @Test
    void kdeRetainsDuplicateObservationWeightsWhenPreparingItsCurve() {
        Samples model = Samples.observations(0, 0, 0, 10, Double.NaN, Double.POSITIVE_INFINITY);
        KDERenderer renderer = new KDERenderer().setBandwidth(1);
        Path curve = render(renderer, model, context(model, -3, 13, 0, 1)).lines.getFirst();
        for (int i = 0; i < curve.x.length; i++) {
            double x = curve.x[i] / 25.0 - 3;
            double expected = (3 * gaussian(x) + gaussian(x - 10)) / 4;
            assertEquals(400 * (1 - expected), curve.y[i], EPSILON);
        }
        assertEquals(gaussian(0) * 3 * 1.05 / 4, renderer.getPreferredYRange(model)[1], 1e-12);
    }

    @Test
    void kdeKeepsFiniteRangesAndNonzeroDensityForVeryBroadFiniteBandwidths() {
        Samples model = Samples.observations(-1, 1);
        KDERenderer renderer = new KDERenderer().setBandwidth(1e308);
        assertArrayEquals(new double[]{-Double.MAX_VALUE, Double.MAX_VALUE}, renderer.getPreferredXRange(model));
        double actual = renderer.getPreferredYRange(model)[1];
        double expected = gaussian(0) / 1e308 * 1.05;
        assertTrue(Double.isFinite(actual) && actual > 0.0);
        assertEquals(expected, actual, 4 * Double.MIN_VALUE);
    }

    @Test
    void automaticKdeBandwidthDoesNotOverflowForLargeFiniteObservations() {
        Samples model = Samples.observations(-1e200, 1e200);
        KDERenderer renderer = new KDERenderer();
        double h = 0.9 * Math.pow(2, -0.2) * 1e200;
        double[] range = renderer.getPreferredXRange(model);
        assertEquals(-1e200 - 3 * h, range[0], h * 1e-14);
        assertEquals(1e200 + 3 * h, range[1], h * 1e-14);
        double maximum = renderer.getPreferredYRange(model)[1];
        assertTrue(Double.isFinite(maximum) && maximum > 0.0);
    }

    @Test
    void constantKdeHasFinitePositiveBandwidthAndAnObservableCurve() {
        Samples model = Samples.observations(5, 5, 5, Double.NaN);
        KDERenderer renderer = new KDERenderer();
        assertArrayEquals(new double[]{2, 8}, renderer.getPreferredXRange(model), EPSILON);
        Geometry canvas = render(renderer, model, context(model, 0, 10, 0, 1));
        Path curve = canvas.lines.getFirst();
        assertEquals(80, curve.x[0], EPSILON);
        assertEquals(320, curve.x[curve.x.length - 1], EPSILON);
        assertTrue(minimum(curve.y) < 245, "The constant sample must have a finite density peak.");
        double maxDensity = (400 - minimum(curve.y)) / 400;
        assertEquals(maxDensity * 1.05, renderer.getPreferredYRange(model)[1], 1e-6);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ecdfKeepsProbabilityConstantBetweenObservations(boolean multiColor) {
        Samples model = Samples.observations(0, 10);
        ECDFRenderer renderer = new ECDFRenderer();
        renderer.setMultiColor(multiColor);
        Geometry canvas = render(renderer, model, context(model, -5, 15, 0, 1));
        assertContainsSegment(canvas, 100, 400, 100, 200);
        assertContainsSegment(canvas, 100, 200, 300, 200);
        assertContainsSegment(canvas, 300, 200, 300, 0);
        assertContainsSegment(canvas, 300, 0, 400, 0);
        assertAxisAligned(canvas);
        assertArrayEquals(new double[]{0, 10}, renderer.getPreferredXRange(model), EPSILON);
    }

    @Test
    void ecdfCombinesDuplicatesAndDrawsASingleObservation() {
        Samples duplicates = Samples.observations(2, 2, 10, Double.NaN);
        Geometry duplicateCanvas = render(new ECDFRenderer(), duplicates, context(duplicates, 0, 10, 0, 1));
        assertContainsSegment(duplicateCanvas, 80, 400, 80, 400.0 / 3);
        assertContainsSegment(duplicateCanvas, 80, 400.0 / 3, 400, 400.0 / 3);
        Samples singleton = Samples.observations(2);
        Geometry singletonCanvas = render(new ECDFRenderer(), singleton, context(singleton, 0, 4, 0, 1));
        assertContainsSegment(singletonCanvas, 200, 400, 200, 0);
        assertContainsSegment(singletonCanvas, 200, 0, 400, 0);
    }

    @Test
    void histogramMapsWeightedCountsAndBinEdgesThroughTheRequestedAxes() {
        withProperty("chart.histogram.bins", "2", () -> {
            Samples model = new Samples(new double[]{0, 1, 2, 3, 4}, new double[]{100, 100, 100, 100, 100},
                    new double[]{1, 1, 1, 1, 2});
            HistogramRenderer renderer = new HistogramRenderer();
            Geometry full = render(renderer, model, context(model, 0, 4, 0, 8));
            assertRect(full.fills.get(0), 0, 300, 180, 100);
            assertRect(full.fills.get(1), 200, 200, 180, 200);
            assertArrayEquals(new double[]{0, 4.2}, renderer.getPreferredYRange(model), EPSILON);
            Geometry zoomY = render(renderer, model, context(model, 0, 4, 0, 16));
            assertRect(zoomY.fills.get(0), 0, 350, 180, 50);
            assertRect(zoomY.fills.get(1), 200, 300, 180, 100);
            Geometry zoomX = render(renderer, model, context(model, 2, 4, 0, 8));
            assertEquals(1, zoomX.fills.size());
            assertRect(zoomX.fills.getFirst(), 0, 200, 360, 200);
        });
    }

    @Test
    void histogramIncludesTheUpperObservationAndIgnoresNonfiniteObservations() {
        withProperty("chart.histogram.bins", "2", () -> {
            Samples model = new Samples(new double[]{0, 2, Double.NaN, Double.POSITIVE_INFINITY},
                    new double[]{0, 0, 0, 0}, new double[]{1, 1, 1, 1});
            HistogramRenderer renderer = new HistogramRenderer();
            Geometry canvas = render(renderer, model, context(model, 0, 2, 0, 2));
            assertEquals(2, canvas.fills.size());
            assertEquals(200, canvas.fills.get(0).h, EPSILON);
            assertEquals(200, canvas.fills.get(1).h, EPSILON);
            assertArrayEquals(new double[]{0, 1.05}, renderer.getPreferredYRange(model), EPSILON);
        });
    }

    @Test
    void liveDistributionIncludesBothBoundsAndRejectsValuesJustOutsideThem() {
        withProperty("Chart.statistical.histogram.bins", "12", () -> {
            LiveDistributionOverlayRenderer renderer = new LiveDistributionOverlayRenderer();
            Samples upper = Samples.observations(10, 10, 10, 10);
            Geometry top = render(renderer, upper, context(upper, 0, 4, 0, 10));
            assertEquals(1, top.fills.size());
            assertEquals(0, top.fills.getFirst().y, EPSILON);
            Samples lower = Samples.observations(0, 0, 0, 0);
            Geometry bottom = render(renderer, lower, context(lower, 0, 4, 0, 10));
            assertEquals(1, bottom.fills.size());
            assertEquals(400 - 400.0 / 12, bottom.fills.getFirst().y, EPSILON);
            Samples outside = Samples.observations(-0.1, -0.1, -0.1, -0.1);
            assertTrue(render(renderer, outside, context(outside, 0, 4, 0, 10)).fills.isEmpty());
            Samples insufficient = Samples.observations(5, 5, 5, -0.1);
            assertTrue(render(renderer, insufficient, context(insufficient, 0, 4, 0, 10)).fills.isEmpty());
        });
    }

    @Test
    void liveDistributionBinEdgesRespectLogarithmicYMapping() {
        withProperty("Chart.statistical.histogram.bins", "12", () -> {
            Samples model = Samples.observations(1000, 1000, 1000, 1000);
            PlotContext context = new DefaultPlotContext(new ArberRect(0, 0, 400, 400), 0, 4, 1, 1000,
                    true, NiceScale.ScaleMode.LINEAR, NiceScale.ScaleMode.LOGARITHMIC);
            Geometry canvas = render(new LiveDistributionOverlayRenderer(), model, context);
            double lowerBinEdge = 1 + 11 * (999.0 / 12);
            double expectedLowerPixel = 400 * (1 - Math.log10(lowerBinEdge) / 3);
            assertEquals(0, canvas.fills.getFirst().y, EPSILON);
            assertEquals(expectedLowerPixel - 1, canvas.fills.getFirst().h, EPSILON);
        });
    }

    @Test
    void translatingTheRidgeXDomainDoesNotMoveItVertically() {
        Samples original = new Samples(new double[]{0, 1, 2}, new double[]{20, 40, 10}, null);
        Samples translated = new Samples(new double[]{1000, 1001, 1002}, new double[]{20, 40, 10}, null);
        RidgeLineRenderer renderer = new RidgeLineRenderer();
        Geometry first = render(renderer, original, context(original, 0, 2, 0, 50));
        Geometry second = render(renderer, translated, context(translated, 1000, 1002, 0, 50));
        assertPath(first.polygons.getFirst(), second.polygons.getFirst());
        assertArrayEquals(new float[]{400, 240, 80, 320, 400}, second.polygons.getFirst().y, 0.0001f);
    }

    @ParameterizedTest
    @ValueSource(floats = {1, 2})
    void beeswarmSeparatesIdenticalPointsWithoutChangingTheirYValue(float scale) {
        double[] x = new double[10];
        double[] y = new double[10];
        Arrays.fill(x, 1);
        Arrays.fill(y, 50);
        Samples model = new Samples(x, y, null);
        BeeswarmRenderer renderer = new BeeswarmRenderer();
        Geometry first = render(renderer, model, context(model, 0, 2, 0, 100), scale);
        Geometry second = render(renderer, model, context(model, 0, 2, 0, 100), scale);
        assertEquals(10, first.polygons.size());
        for (int i = 0; i < 10; i++) {
            assertPath(first.polygons.get(i), second.polygons.get(i));
            assertEquals(200, average(first.polygons.get(i).y), EPSILON);
        }
        assertSeparated(first, 8.5 * scale);
    }

    @Test
    void beeswarmAlsoAvoidsNearbyPointsFromDifferentXPositions() {
        Samples model = new Samples(new double[]{1, 1.002, 1, 0.998, 1.003},
                new double[]{50, 50.25, 51, 49.75, 52}, null);
        Geometry canvas = render(new BeeswarmRenderer(), model, context(model, 0, 2, 0, 100));
        assertSeparated(canvas, 8.5);
        for (int i = 0; i < model.getPointCount(); i++) {
            assertEquals(400 - model.getY(i) * 4, average(canvas.polygons.get(i).y), EPSILON);
        }
    }

    private static Geometry render(ChartRenderer renderer, ChartModel model, PlotContext context) {
        return render(renderer, model, context, 1);
    }

    private static Geometry render(ChartRenderer renderer, ChartModel model, PlotContext context, float scale) {
        Geometry canvas = new Geometry();
        ChartScale.applyScale(scale, () -> renderer.render(canvas, model, context));
        return canvas;
    }

    private static PlotContext context(ChartModel model, double minX, double maxX, double minY, double maxY) {
        return new DefaultPlotContext(new ArberRect(0, 0, 400, 400), model, minX, maxX, minY, maxY);
    }

    private static void withProperty(String key, String value, Runnable test) {
        String previous = ChartAssets.getString(key, null);
        ChartAssets.setProperty(key, value);
        try {
            test.run();
        } finally {
            if (previous == null) ChartAssets.removeProperty(key);
            else ChartAssets.setProperty(key, previous);
        }
    }

    private static void assertRect(Rect rect, double x, double y, double width, double height) {
        assertEquals(x, rect.x, EPSILON);
        assertEquals(y, rect.y, EPSILON);
        assertEquals(width, rect.w, EPSILON);
        assertEquals(height, rect.h, EPSILON);
    }

    private static void assertLine(Path line, double x0, double y0, double x1, double y1) {
        assertArrayEquals(new float[]{(float) x0, (float) x1}, line.x, (float) EPSILON);
        assertArrayEquals(new float[]{(float) y0, (float) y1}, line.y, (float) EPSILON);
    }

    private static void assertPath(Path expected, Path actual) {
        assertArrayEquals(expected.x, actual.x, (float) EPSILON);
        assertArrayEquals(expected.y, actual.y, (float) EPSILON);
    }

    private static void assertContainsSegment(Geometry canvas, double x0, double y0, double x1, double y1) {
        for (Path path : canvas.lines) {
            for (int i = 1; i < path.x.length; i++) {
                if (Math.abs(path.x[i - 1] - x0) < EPSILON && Math.abs(path.y[i - 1] - y0) < EPSILON
                        && Math.abs(path.x[i] - x1) < EPSILON && Math.abs(path.y[i] - y1) < EPSILON) return;
            }
        }
        throw new AssertionError("Missing expected segment: " + x0 + "," + y0 + " to " + x1 + "," + y1);
    }

    private static void assertAxisAligned(Geometry canvas) {
        for (Path path : canvas.lines) {
            for (int i = 1; i < path.x.length; i++) {
                assertTrue(path.x[i] == path.x[i - 1] || path.y[i] == path.y[i - 1], "An ECDF segment must not be diagonal.");
            }
        }
    }

    private static void assertSeparated(Geometry canvas, double spacing) {
        for (int i = 0; i < canvas.polygons.size(); i++) {
            for (int j = 0; j < i; j++) {
                double dx = average(canvas.polygons.get(i).x) - average(canvas.polygons.get(j).x);
                double dy = average(canvas.polygons.get(i).y) - average(canvas.polygons.get(j).y);
                assertTrue(Math.hypot(dx, dy) >= spacing - EPSILON, "Markers must not overlap.");
            }
        }
    }

    private static double average(float[] values) {
        double sum = 0;
        for (float value : values) sum += value;
        return sum / values.length;
    }

    private static double minimum(float[] values) {
        double result = Double.POSITIVE_INFINITY;
        for (float value : values) result = Math.min(result, value);
        return result;
    }

    private static double maximum(float[] values) {
        double result = Double.NEGATIVE_INFINITY;
        for (float value : values) result = Math.max(result, value);
        return result;
    }

    private static double gaussian(double value) {
        return Math.exp(-0.5 * value * value) / Math.sqrt(2 * Math.PI);
    }

    private static final class OutlierSummary extends DefaultStatisticalChartModel implements BoxPlotOutlierModel {
        @Override
        public double[] getOutliers(int index) {
            return index == 0 ? new double[]{-10, 110} : new double[]{95};
        }
    }

    private record Samples(double[] x, double[] y, double[] weights) implements ChartModel {
        static Samples observations(double... values) {
            double[] x = new double[values.length];
            for (int i = 0; i < x.length; i++) x[i] = i;
            return new Samples(x, values, null);
        }

        @Override public String getName() { return "Statistical regression fixture"; }
        @Override public int getPointCount() { return y.length; }
        @Override public double[] getXData() { return x; }
        @Override public double[] getYData() { return y; }
        @Override public double getValue(int index, int component) { return component == 2 && weights != null ? weights[index] : 1; }
        @Override public void setChangeListener(ChartModelListener listener) { }
        @Override public void removeChangeListener(ChartModelListener listener) { }
    }

    private record Path(float[] x, float[] y) { }
    private record Rect(float x, float y, float w, float h) {
        double centerX() { return x + w / 2; }
        double centerY() { return y + h / 2; }
    }

    private static final class Geometry implements ArberCanvas {
        private final List<Path> lines = new ArrayList<>();
        private final List<Path> polygons = new ArrayList<>();
        private final List<Rect> fills = new ArrayList<>();
        private final List<Rect> outlines = new ArrayList<>();
        private ArberRect clip;
        private float lastX;
        private float lastY;

        @Override public void setColor(ArberColor color) { }
        @Override public void setStroke(float width) { }
        @Override public void moveTo(float x, float y) { lastX = x; lastY = y; }
        @Override public void lineTo(float x, float y) {
            drawPolyline(new float[]{lastX, x}, new float[]{lastY, y}, 2);
            moveTo(x, y);
        }
        @Override public void drawPolyline(float[] x, float[] y, int count) { lines.add(path(x, y, count)); }
        @Override public void fillPolygon(float[] x, float[] y, int count) { polygons.add(path(x, y, count)); }
        @Override public void drawRect(float x, float y, float w, float h) { outlines.add(rect(x, y, w, h)); }
        @Override public void fillRect(float x, float y, float w, float h) { fills.add(rect(x, y, w, h)); }
        @Override public void drawVoxelField(VoxelBuffer buffer) { throw new AssertionError("Unexpected voxel output"); }
        @Override public ArberMatrix getTransform() { return ArberMatrices.identity(); }
        @Override public void setClip(ArberRect clip) { this.clip = clip; }
        @Override public ArberRect getClip() { return clip; }

        private static Path path(float[] x, float[] y, int count) {
            for (int i = 0; i < count; i++) {
                assertTrue(Float.isFinite(x[i]) && Float.isFinite(y[i]), "Geometry must be finite.");
            }
            return new Path(Arrays.copyOf(x, count), Arrays.copyOf(y, count));
        }

        private static Rect rect(float x, float y, float w, float h) {
            assertTrue(Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(w) && Float.isFinite(h));
            return new Rect(x, y, w, h);
        }
    }
}
