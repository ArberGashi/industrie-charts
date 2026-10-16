package com.arbergashi.charts.springdemo;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.model.BoxPlotOutlierModel;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.StatisticalChartModel;
import com.arbergashi.charts.render.circular.DonutRenderer;
import com.arbergashi.charts.render.circular.RadarRenderer;
import com.arbergashi.charts.render.specialized.HeatmapRenderer;
import com.arbergashi.charts.render.statistical.KDERenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoExamplesTest {
    private static final ArberColor BLUE = new ArberColor(0xFF3574D8);

    @Test
    void donutWeightsRepresentFiveCategoryCountsAndPreserveTheObservationTotal() {
        DemoExamples.Dataset data = DemoExamples.find("donut").create(100, BLUE);
        assertInstanceOf(DonutRenderer.class, data.renderer());
        assertEquals(5, data.model().getPointCount());
        double[] expected = {34, 26, 18, 14, 8};
        String[] labels = {"Solar", "Wind", "Hydro", "Storage", "Other"};
        double total = 0;
        for (int i = 0; i < expected.length; i++) {
            assertEquals(labels[i], data.model().getLabel(i));
            assertEquals(expected[i], data.model().getY(i));
            assertEquals(expected[i], data.model().getWeight(i));
            total += data.model().getWeight(i);
        }
        assertEquals(100, total);
    }

    @Test
    void radarAggregatesMeasurementsIntoSixNamedScoresOnTheSameScale() {
        DemoExamples.Dataset data = DemoExamples.find("radar").create(16, BLUE);
        DemoExamples.Dataset larger = DemoExamples.find("radar").create(1024, BLUE);
        assertInstanceOf(RadarRenderer.class, data.renderer());
        assertEquals(6, data.model().getPointCount());
        assertEquals(6, larger.model().getPointCount());
        assertEquals(0, data.minY());
        assertEquals(100, data.maxY());
        for (int i = 0; i < 6; i++) {
            assertTrue(data.model().getY(i) > 60 && data.model().getY(i) < 100);
            assertTrue(!data.model().getLabel(i).isBlank());
        }
        assertNotEquals(data.model().getY(0), larger.model().getY(0));
    }

    @Test
    void heatmapUsesCellCentersAndNeverExceedsTheRequestedCellBudget() {
        DemoExamples.Dataset data = DemoExamples.find("heatmap").create(96, BLUE);
        assertInstanceOf(HeatmapRenderer.class, data.renderer());
        assertEquals(81, data.model().getPointCount());
        assertEquals(10.0 / 18, data.model().getX(0), 1e-12);
        assertEquals(6.0 / 18, data.model().getY(0), 1e-12);
        double peak = 0;
        for (int i = 0; i < data.model().getPointCount(); i++) {
            double x = data.model().getX(i);
            double y = data.model().getY(i);
            assertTrue(x > data.minX() && x < data.maxX());
            assertTrue(y > data.minY() && y < data.maxY());
            assertTrue(Double.isFinite(data.model().getWeight(i)));
            peak = Math.max(peak, data.model().getWeight(i));
        }
        assertTrue(peak > 80);
        assertTrue(data.model().getWeight(0) < 6);
        assertEquals(2025, DemoExamples.find("heatmap").create(2048, BLUE).model().getPointCount());
    }

    @Test
    void boxSummaryUsesInterpolatedQuartilesAndSeparatesTukeyOutliers() {
        double[] observations = {25, 7, 2, 5, 1, 9, 3, 8, 4, 6, 0};
        double[] original = observations.clone();
        DemoExamples.BoxSummary summary = DemoExamples.summarize(observations);
        assertEquals(2.5, summary.q1());
        assertEquals(5, summary.median());
        assertEquals(7.5, summary.q3());
        assertEquals(0, summary.low());
        assertEquals(9, summary.high());
        assertArrayEquals(new double[]{25}, summary.outliers());
        assertArrayEquals(original, observations, "Summarizing must not reorder the caller's samples.");
    }

    @Test
    void boxplotCarriesTypedQuartilesAndVisibleOutliersForEachBatch() {
        DemoExamples.Dataset data = DemoExamples.find("boxplot").create(96, BLUE);
        StatisticalChartModel model = assertInstanceOf(StatisticalChartModel.class, data.model());
        BoxPlotOutlierModel outliers = assertInstanceOf(BoxPlotOutlierModel.class, model);
        assertEquals(6, model.getPointCount());
        for (int i = 0; i < model.getPointCount(); i++) {
            assertTrue(model.getMin(i) <= model.getQ1(i));
            assertTrue(model.getQ1(i) <= model.getMedian(i));
            assertTrue(model.getMedian(i) <= model.getQ3(i));
            assertTrue(model.getQ3(i) <= model.getMax(i));
            double fence = 1.5 * (model.getQ3(i) - model.getQ1(i));
            assertTrue(outliers.getOutliers(i).length >= 2);
            for (double value : outliers.getOutliers(i)) {
                assertTrue(value < model.getQ1(i) - fence || value > model.getQ3(i) + fence);
                assertTrue(value >= data.minY() && value <= data.maxY());
            }
        }
    }

    @Test
    void densityPlotsObservationsOnXAndProbabilityDensityOnY() {
        DemoExamples.Dataset data = DemoExamples.find("kde").create(256, BLUE);
        assertInstanceOf(KDERenderer.class, data.renderer());
        assertEquals(256, data.model().getPointCount());
        assertTrue(data.maxX() < 10, "The X domain must describe measured values, not observation indices.");
        assertEquals(0, data.minY());
        assertTrue(data.maxY() > 0 && data.maxY() < 2);
        for (int i = 0; i < data.model().getPointCount(); i++) {
            double observation = data.model().getY(i);
            assertTrue(observation - 0.54 >= data.minX());
            assertTrue(observation + 0.54 <= data.maxX());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"line", "area", "step", "bar", "scatter", "candlestick", "donut", "radar",
            "heatmap", "boxplot", "kde"})
    void requestsOwnTheirModelsAndStatefulRenderers(String id) {
        DemoExamples.Example example = DemoExamples.find(id);
        DemoExamples.Dataset first = example.create(48, BLUE);
        DemoExamples.Dataset second = example.create(48, BLUE);
        assertNotSame(first.model(), second.model());
        assertNotSame(first.renderer(), second.renderer());
        ChartModel model = first.model();
        assertTrue(model.getPointCount() > 0);
    }
}
