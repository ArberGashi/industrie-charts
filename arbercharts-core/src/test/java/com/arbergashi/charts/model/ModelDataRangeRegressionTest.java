package com.arbergashi.charts.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelDataRangeRegressionTest {

    @Test
    void nonFiniteCoordinatesDoNotPolluteFiniteDataRanges() {
        for (ChartModel model : xyModels()) {
            append(model, 1, 10);
            append(model, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
            append(model, Double.NaN, Double.NaN);
            append(model, 3, 30);

            assertArrayEquals(new double[]{1, 3, 10, 30}, model.getDataRange(), model.getClass().getSimpleName());
            assertTrue(Double.isInfinite(model.getX(1)), "Range calculation must preserve raw input");
            assertTrue(Double.isInfinite(model.getY(1)), "Range calculation must preserve raw input");
            assertTrue(Double.isNaN(model.getY(2)), "Missing samples must remain missing");
        }
    }

    @Test
    void missingAxisReturnsEmptyRangeInsteadOfSentinelExtremes() {
        for (double[] point : new double[][]{
                {Double.NaN, Double.POSITIVE_INFINITY},
                {Double.NEGATIVE_INFINITY, 10},
                {1, Double.NaN}}) {
            for (ChartModel model : xyModels()) {
                append(model, point[0], point[1]);
                assertArrayEquals(new double[]{0, 0, 0, 0}, model.getDataRange(), model.getClass().getSimpleName());
            }
        }
    }

    @Test
    void circularRangeIncludesErrorIntervalsAfterOverwrite() {
        CircularChartModel model = new CircularChartModel(2);
        model.setWithError(1, 10, -100, 100);
        model.setWithError(2, 20, 15, 25);
        assertArrayEquals(new double[]{1, 2, -100, 100}, model.getDataRange());

        model.setWithError(3, 30, 28, 35);

        assertArrayEquals(new double[]{2, 3, 15, 35}, model.getDataRange());
    }

    @Test
    void finiteIntervalEndpointsRemainUsableWithMissingValues() {
        DefaultChartModel regular = new DefaultChartModel();
        regular.setPoint(1, Double.NaN, 5, 15, 1, null);
        regular.setPoint(2, 20, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1, null);
        CircularChartModel circular = new CircularChartModel(4);
        circular.setPoint(1, Double.NaN, 5, 15, 1, null);
        circular.setPoint(2, 20, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 1, null);

        assertArrayEquals(new double[]{1, 2, 5, 20}, regular.getDataRange());
        assertArrayEquals(new double[]{1, 2, 5, 20}, circular.getDataRange());
        assertTrue(Double.isNaN(regular.getY(0)));
        assertTrue(Double.isNaN(circular.getY(0)));
    }

    @Test
    void medicalRangeIncludesFiniteValuesFromEveryChannel() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(2, 2);
        model.add(1, new double[]{Double.NaN, -20});
        model.add(2, new double[]{10, Double.POSITIVE_INFINITY});

        assertArrayEquals(new double[]{1, 2, -20, 10}, model.getDataRange());

        model.add(3, new double[]{5, 30});

        assertArrayEquals(new double[]{2, 3, 5, 30}, model.getDataRange());
        assertEquals(2, model.getPointCount());
    }

    private static List<ChartModel> xyModels() {
        return List.of(new DefaultChartModel(), new CircularChartModel(8),
                new FastMedicalModel("signal"), new CircularFastMedicalModel(8, 1));
    }

    private static void append(ChartModel model, double x, double y) {
        if (model instanceof CircularChartModel circular) {
            circular.setXY(x, y);
        } else if (model instanceof DefaultChartModel regular) {
            regular.setXY(x, y);
        } else if (model instanceof CircularFastMedicalModel medical) {
            medical.add(x, new double[]{y});
        } else if (model instanceof FastMedicalModel medical) {
            medical.setPoint(x, y);
        }
    }
}
