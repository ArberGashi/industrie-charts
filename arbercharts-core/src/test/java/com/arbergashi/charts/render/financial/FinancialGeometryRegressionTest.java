package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.model.DefaultFinancialChartModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static com.arbergashi.charts.render.financial.FinancialRendererTestSupport.render;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialGeometryRegressionTest {
    @ParameterizedTest
    @CsvSource({"false,true,160,80,360", "false,false,160,80,360",
            "true,true,80,40,320", "true,false,80,40,320"})
    void hollowCandlesUseProjectedOpenCloseDistance(boolean invertedY, boolean rising,
                                                    float bodyTop, float wickTop, float wickBottom) {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel();
        model.setOHLC(5, rising ? 20 : 60, 80, 10, rising ? 60 : 20);
        FinancialRendererTestSupport.Context context = new FinancialRendererTestSupport.Context(
                0, 10, 0, 100, new ArberRect(0, 0, 400, 400), invertedY);
        FinancialRendererTestSupport.RecordingCanvas canvas = render(new CandlestickHollowRenderer(), model, context);
        List<FinancialRendererTestSupport.Draw> bodies = canvas.ofType(rising ? "outline" : "fill");
        assertEquals(1, bodies.size());
        assertEquals(bodyTop, bodies.getFirst().coordinate(1));
        assertEquals(160f, bodies.getFirst().coordinate(3));
        List<FinancialRendererTestSupport.Draw> wicks = canvas.ofType("polyline");
        assertEquals(2, wicks.size());
        assertEquals(wickTop, wicks.get(0).coordinate(1));
        assertEquals(bodyTop, wicks.get(0).coordinate(3));
        assertEquals(bodyTop + 160, wicks.get(1).coordinate(1));
        assertEquals(wickBottom, wicks.get(1).coordinate(3));
    }

    @Test
    void hollowCandleLodPreservesOhlcAndUsesTheCurrentVisibleProjection() {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel();
        for (int i = 0; i < 2001; i++) {
            model.setOHLC(5, i == 0 ? 20 : 70, 90, 10, i == 2000 ? 80 : 40);
        }
        // These bars must not contaminate the visible candle's OHLC values.
        model.setOHLC(-100, 500, 1000, -1000, 600);
        model.setOHLC(100, 500, 1000, -1000, 600);
        CandlestickHollowRenderer renderer = new CandlestickHollowRenderer();
        FinancialRendererTestSupport.Context initial = new FinancialRendererTestSupport.Context(
                0, 10, 0, 100, new ArberRect(0, 0, 400, 400), false);
        FinancialRendererTestSupport.RecordingCanvas canvas = render(renderer, model, initial);
        assertEquals(1, canvas.ofType("outline").size());
        FinancialRendererTestSupport.Draw body = canvas.ofType("outline").getFirst();
        assertEquals(200.5f, body.coordinate(0) + body.coordinate(2) / 2);
        assertEquals(80f, body.coordinate(1));
        assertEquals(240f, body.coordinate(3));
        assertEquals(40f, canvas.ofType("polyline").get(0).coordinate(1));
        assertEquals(360f, canvas.ofType("polyline").get(1).coordinate(3));

        FinancialRendererTestSupport.Context changed = new FinancialRendererTestSupport.Context(
                0, 20, 0, 100, new ArberRect(30, 20, 200, 200), true);
        FinancialRendererTestSupport.RecordingCanvas resized = render(renderer, model, changed);
        assertEquals(render(new CandlestickHollowRenderer(), model, changed).draws, resized.draws);
        FinancialRendererTestSupport.Draw changedBody = resized.ofType("outline").getFirst();
        assertEquals(80.5f, changedBody.coordinate(0) + changedBody.coordinate(2) / 2);
        assertEquals(60f, changedBody.coordinate(1));
        assertEquals(120f, changedBody.coordinate(3));
    }

    @Test
    void renkoRetainsEarlierBrickPricesWhenItsBufferGrows() {
        DefaultChartModel model = prices(0, 150);
        FinancialRendererTestSupport.Context context = new FinancialRendererTestSupport.Context(
                0, 1, 0, 150, new ArberRect(0, 0, 600, 600), false);
        List<FinancialRendererTestSupport.Draw> bricks = render(new RenkoRenderer(), model, context).ofType("fill");
        assertEquals(150, bricks.size());
        for (int i = 0; i < bricks.size(); i++) {
            assertEquals(i * 4f, bricks.get(i).coordinate(0), 0.0001f);
            assertEquals(594f - i * 4, bricks.get(i).coordinate(1), 0.0001f);
        }
    }

    @Test
    void pointAndFigureReversesInBothDirectionsAfterThreeBoxes() {
        DefaultChartModel model = prices(10, 14, 12, 11, 8, 10, 11, 13);
        FinancialRendererTestSupport.Context context = new FinancialRendererTestSupport.Context(
                0, 7, 0, 50, new ArberRect(0, 0, 300, 500), false);
        FinancialRendererTestSupport.RecordingCanvas canvas = render(new PointAndFigureAdvancedRenderer(), model, context);
        List<FinancialRendererTestSupport.Draw> falling = canvas.ofType("outline");
        assertEquals(6, falling.size());
        for (int i = 0; i < falling.size(); i++) {
            FinancialRendererTestSupport.Draw box = falling.get(i);
            assertEquals(150f, box.coordinate(0) + box.coordinate(2) / 2, 0.0001f);
            assertEquals(370f + i * 10, box.coordinate(1) + box.coordinate(3) / 2, 0.0001f);
        }
        List<FinancialRendererTestSupport.Draw> rising = canvas.ofType("polyline");
        assertEquals(18, rising.size());
        List<Float> centersX = new ArrayList<>();
        List<Float> centersY = new ArrayList<>();
        for (int i = 0; i < rising.size(); i += 2) {
            FinancialRendererTestSupport.Draw cross = rising.get(i);
            centersX.add((cross.coordinate(0) + cross.coordinate(2)) / 2);
            centersY.add((cross.coordinate(1) + cross.coordinate(3)) / 2);
        }
        assertEquals(List.of(50f, 50f, 50f, 50f, 250f, 250f, 250f, 250f, 250f), centersX);
        assertEquals(List.of(390f, 380f, 370f, 360f, 410f, 400f, 390f, 380f, 370f), centersY);
    }

    @Test
    void pointAndFigureKeepsSubThresholdPullbacksInTheSameColumn() {
        DefaultChartModel model = prices(20, 16, 18, 15);
        FinancialRendererTestSupport.Context context = new FinancialRendererTestSupport.Context(
                0, 3, 0, 50, new ArberRect(0, 0, 300, 500), false);
        FinancialRendererTestSupport.RecordingCanvas canvas = render(new PointAndFigureAdvancedRenderer(), model, context);
        assertTrue(canvas.ofType("polyline").isEmpty());
        assertEquals(5, canvas.ofType("outline").size());
        for (FinancialRendererTestSupport.Draw box : canvas.ofType("outline")) {
            assertEquals(150f, box.coordinate(0) + box.coordinate(2) / 2, 0.0001f);
        }
    }

    @Test
    void volumeProfileWeightsPriceBinsByFinancialVolume() {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel();
        model.setOHLC(0, 20, 30, 10, 25, 100, null);
        model.setOHLC(1, 70, 80, 60, 75, 400, null);
        FinancialRendererTestSupport.Context context = new FinancialRendererTestSupport.Context(
                0, 1, 0, 100, new ArberRect(0, 0, 400, 400), false);
        List<FinancialRendererTestSupport.Draw> bars = render(new VolumeProfileRenderer(), model, context).ofType("fill");
        assertEquals(2, bars.size());
        assertEquals(0.25f, bars.get(0).coordinate(2) / bars.get(1).coordinate(2), 0.0001f);
    }

    @Test
    void volumeProfileDoesNotSubstituteOpenPricesForMissingFinancialVolume() {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel();
        model.setOHLC(0, 20, 30, 10, 25, 0, null);
        model.setOHLC(1, 70, 80, 60, 75, Double.NaN, null);
        FinancialRendererTestSupport.Context context = new FinancialRendererTestSupport.Context(
                0, 1, 0, 100, new ArberRect(0, 0, 400, 400), false);
        assertTrue(render(new VolumeProfileRenderer(), model, context).draws.isEmpty());
    }

    @Test
    void obvAddsAndSubtractsFinancialVolumeInsteadOfOpenPrices() {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel();
        model.setOHLC(0, 90, 110, 80, 100, 10, null);
        model.setOHLC(1, 100, 120, 90, 110, 50, null);
        model.setOHLC(2, 110, 120, 95, 105, 20, null);
        OBVRenderer renderer = new OBVRenderer();
        assertArrayEquals(new double[] {0, 50}, renderer.getPreferredYRange(model), 1e-12);
        FinancialRendererTestSupport.Context context = new FinancialRendererTestSupport.Context(
                0, 2, 0, 100, new ArberRect(0, 0, 200, 200), false);
        List<FinancialRendererTestSupport.Draw> paths = render(renderer, model, context).ofType("polyline");
        FinancialRendererTestSupport.Draw obv = paths.getLast();
        assertEquals(List.of(0f, 200f, 100f, 100f, 200f, 140f), obv.coordinates());
    }

    private static DefaultChartModel prices(double... values) {
        DefaultChartModel model = new DefaultChartModel();
        for (int i = 0; i < values.length; i++) model.setXY(i, values[i]);
        return model;
    }
}
