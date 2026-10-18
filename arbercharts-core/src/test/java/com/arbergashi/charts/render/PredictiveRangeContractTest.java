package com.arbergashi.charts.render;

import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.LinearLeastSquaresPredictor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.model.DefaultFinancialChartModel;
import com.arbergashi.charts.render.financial.PredictiveCandleRenderer;
import com.arbergashi.charts.render.predictive.PredictiveShadowRenderer;
import com.arbergashi.charts.render.standard.PredictivePathRenderer;
import com.arbergashi.charts.util.ChartAssets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PredictiveRangeContractTest {
    @Test
    void shadowRangeContainsHistoryAndEveryForecastPoint() {
        DefaultChartModel model = linearModel(0, 10);
        PredictiveShadowRenderer renderer = new PredictiveShadowRenderer(new LinearLeastSquaresPredictor(4, 4, 1e-6, 0));
        assertArrayEquals(new double[]{0, 7}, renderer.getPreferredXRange(model), 1e-9);
        assertArrayEquals(new double[]{10, 80}, renderer.getPreferredYRange(model), 1e-9);
    }

    @Test
    void fittingANewWindowDoesNotKeepOldBoundsOrOverwriteTheRenderedAudit() {
        DefaultChartModel model = linearModel(0, 10);
        PredictiveShadowRenderer renderer = new PredictiveShadowRenderer(new LinearLeastSquaresPredictor(4, 4, 1e-6, 0));
        renderer.ensurePredicted(model, new DefaultPlotContext(new ArberRect(0, 0, 400, 300), model, 0, 7, 0, 90));
        double oldPrediction = renderer.predictedForX(4);
        model.clear();
        for (int i = 0; i < 4; i++) model.setPoint(100 + i, i + 1, 1, "");
        assertArrayEquals(new double[]{100, 107}, renderer.getPreferredXRange(model), 1e-9);
        assertArrayEquals(new double[]{1, 8}, renderer.getPreferredYRange(model), 1e-9);
        assertEquals(oldPrediction, renderer.predictedForX(4),
                "Range estimation must not replace the actual rendered prediction's audit history");
    }

    @Test
    void predictionRangeUsesANonNullContextAndIgnoresNonFiniteForecasts() {
        PredictiveShadowRenderer renderer = new PredictiveShadowRenderer((model, context, out) -> {
            assertNotNull(context);
            out.ensureCapacity(3);
            out.x()[0] = 4;
            out.y()[0] = 50;
            out.x()[1] = Double.POSITIVE_INFINITY;
            out.y()[1] = 1000;
            out.x()[2] = 1000;
            out.y()[2] = Double.NaN;
            out.setCount(3);
        });
        assertArrayEquals(new double[]{0, 4}, renderer.getPreferredXRange(linearModel(0, 10)));
        assertArrayEquals(new double[]{10, 50}, renderer.getPreferredYRange(linearModel(0, 10)));
    }

    @Test
    void pathRangeIncludesItsConvergedEndpointAndBarWidth() {
        DefaultChartModel model = linearModel(0, 1);
        int lookahead = Math.max(1, ChartAssets.getInt("Chart.predictive.global.lookahead", 32));
        double endX = 3 + lookahead;
        double endY = 4 + lookahead;
        PredictivePathRenderer line = new PredictivePathRenderer();
        assertArrayEquals(new double[]{0, endX}, line.getPreferredXRange(model), 1e-9);
        assertArrayEquals(new double[]{1, endY}, line.getPreferredYRange(model), 1e-9);
        PredictivePathRenderer bar = new PredictivePathRenderer(PredictivePathRenderer.Style.BAR);
        assertEquals(endX + lookahead * 0.35, bar.getPreferredXRange(model)[1], 1e-9);
        assertEquals(0, bar.getPreferredYRange(model)[0]);
    }

    @Test
    void candleRangeContainsHistoricalWicksAndTheFullForecastWick() {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("prices");
        for (int i = 0; i < 20; i++) model.setOHLC(i, 100, 105, 95, 100);
        PredictiveShadowRenderer shadow = new PredictiveShadowRenderer((source, context, out) -> {
            out.ensureCapacity(1);
            out.x()[0] = 20;
            out.y()[0] = 130;
            out.setCount(1).setResidualStd(20).setResidualScale(6);
        });
        PredictiveCandleRenderer candle = new PredictiveCandleRenderer(shadow);
        double sigmaFactor = Math.max(0.1, ChartAssets.getFloat("Chart.financial.ghost.sigmaFactor", 1));
        double rangeFactor = Math.max(0.1, ChartAssets.getFloat("Chart.financial.ghost.rangeFactor", 1));
        double uncertainty = 120 * sigmaFactor * rangeFactor;
        double[] x = candle.getPreferredXRange(model);
        double[] y = candle.getPreferredYRange(model);
        assertEquals(0, x[0]);
        assertEquals(20.35, x[1], 1e-9);
        assertEquals(Math.min(95, 100 - uncertainty), y[0], 1e-9);
        assertEquals(Math.max(105, 130 + uncertainty), y[1], 1e-9);
    }

    @Test
    void emptyModelsUseTheHostsDefaultEmptyRange() {
        DefaultChartModel empty = new DefaultChartModel("empty");
        for (ChartRenderer renderer : new ChartRenderer[]{new PredictiveShadowRenderer(),
                new PredictivePathRenderer(), new PredictiveCandleRenderer(new PredictiveShadowRenderer())}) {
            assertNull(renderer.getPreferredXRange(empty));
            assertNull(renderer.getPreferredYRange(empty));
        }
    }

    private static DefaultChartModel linearModel(int startX, double stepY) {
        DefaultChartModel model = new DefaultChartModel("linear");
        for (int i = 0; i < 4; i++) model.setPoint(startX + i, (i + 1) * stepY, 1, "");
        return model;
    }
}
