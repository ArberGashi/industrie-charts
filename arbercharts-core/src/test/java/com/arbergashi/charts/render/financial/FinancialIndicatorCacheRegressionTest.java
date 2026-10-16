package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.CircularChartModel;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.arbergashi.charts.render.financial.FinancialRendererTestSupport.render;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialIndicatorCacheRegressionTest {
    private static final int POINT_COUNT = 128;
    private static final FinancialRendererTestSupport.Context CONTEXT =
            new FinancialRendererTestSupport.Context(0, POINT_COUNT - 1, -10000, 10000,
                    new ArberRect(20, 30, 640, 480), false);

    static Stream<Arguments> indicators() {
        return Stream.of(
                indicator("ATR", ATRRenderer::new),
                indicator("ADX", ADXRenderer::new),
                indicator("RSI", RSIRenderer::new),
                indicator("Stochastic", StochasticRenderer::new),
                indicator("OBV", OBVRenderer::new),
                indicator("Parabolic SAR", ParabolicSARRenderer::new),
                indicator("Heikin Ashi", HeikinAshiRenderer::new),
                indicator("Ichimoku", IchimokuRenderer::new),
                indicator("Fibonacci", FibonacciRenderer::new),
                indicator("Pivot Points", PivotPointsRenderer::new),
                indicator("Renko", RenkoRenderer::new),
                indicator("MACD", MACDRenderer::new),
                indicator("Bollinger Bands", BollingerBandsRenderer::new));
    }

    private static Arguments indicator(String name, Supplier<BaseRenderer> factory) {
        return Arguments.of(name, factory);
    }

    @ParameterizedTest(name = "{0}: same model and point count")
    @MethodSource("indicators")
    void recomputesAfterSameLengthReplacement(String name, Supplier<BaseRenderer> factory) {
        DefaultChartModel model = series(false);
        BaseRenderer reused = factory.get();
        List<FinancialRendererTestSupport.Draw> before = render(reused, model, CONTEXT).draws;
        long stamp = model.getUpdateStamp();
        model.clear();
        fill(model, true);
        assertEquals(POINT_COUNT, model.getPointCount());
        assertTrue(model.getUpdateStamp() > stamp);
        assertUpdatedAndMatchesFresh(factory, reused, model, before);
    }

    @ParameterizedTest(name = "{0}: different model with identical stamp")
    @MethodSource("indicators")
    void distinguishesModelsWithSameStampAndPointCount(String name, Supplier<BaseRenderer> factory) {
        DefaultChartModel first = series(false);
        DefaultChartModel second = series(true);
        assertEquals(first.getPointCount(), second.getPointCount());
        assertEquals(first.getUpdateStamp(), second.getUpdateStamp());
        BaseRenderer reused = factory.get();
        List<FinancialRendererTestSupport.Draw> before = render(reused, first, CONTEXT).draws;
        assertUpdatedAndMatchesFresh(factory, reused, second, before);
    }

    @ParameterizedTest(name = "{0}: full ring buffer")
    @MethodSource("indicators")
    void recomputesAfterFixedCapacityStreamAdvances(String name, Supplier<BaseRenderer> factory) {
        CircularChartModel model = new CircularChartModel(POINT_COUNT);
        append(model, series(false));
        BaseRenderer reused = factory.get();
        List<FinancialRendererTestSupport.Draw> before = render(reused, model, CONTEXT).draws;
        long stamp = model.getUpdateStamp();
        append(model, series(true));
        assertEquals(POINT_COUNT, model.getPointCount());
        assertTrue(model.getUpdateStamp() > stamp);
        assertUpdatedAndMatchesFresh(factory, reused, model, before);
    }

    @ParameterizedTest(name = "{0}: changed view and bounds")
    @MethodSource("indicators")
    void reprojectsCachedSeriesForChangedViewAndSize(String name, Supplier<BaseRenderer> factory) {
        DefaultChartModel model = series(false);
        BaseRenderer reused = factory.get();
        List<FinancialRendererTestSupport.Draw> before = render(reused, model, CONTEXT).draws;
        FinancialRendererTestSupport.Context changed = new FinancialRendererTestSupport.Context(
                15, 110, -2000, 5000, new ArberRect(75, 60, 380, 720), true);
        List<FinancialRendererTestSupport.Draw> expected = render(factory.get(), model, changed).draws;
        assertFalse(expected.isEmpty());
        assertNotEquals(before, expected);
        assertEquals(expected, render(reused, model, changed).draws);
    }

    @Test
    void atrUsesNewTrueRangesAfterSameLengthReplacement() {
        DefaultChartModel model = new DefaultChartModel();
        for (int i = 0; i < 50; i++) model.setOHLC(i, 100, 102, 98, 100);
        ATRRenderer reused = new ATRRenderer();
        assertArrayEquals(new double[] {0, 4}, reused.getPreferredYRange(model), 1e-12);
        model.clear();
        for (int i = 0; i < 50; i++) model.setOHLC(i, 100, 120, 80, 100);
        assertArrayEquals(new double[] {0, 40}, reused.getPreferredYRange(model), 1e-12);
    }

    @Test
    void pivotMethodChangeInvalidatesCalculatedLevels() {
        DefaultChartModel model = series(false);
        PivotPointsRenderer renderer = new PivotPointsRenderer();
        List<FinancialRendererTestSupport.Draw> before = render(renderer, model, CONTEXT).draws;
        renderer.setPivotMethod(PivotPointsRenderer.PivotMethod.FIBONACCI);
        PivotPointsRenderer fresh = new PivotPointsRenderer(PivotPointsRenderer.PivotMethod.FIBONACCI);
        List<FinancialRendererTestSupport.Draw> expected = render(fresh, model, CONTEXT).draws;
        assertNotEquals(before, expected);
        assertEquals(expected, render(renderer, model, CONTEXT).draws);
    }

    @Test
    void bollingerParameterChangesInvalidateCalculatedBands() {
        String periodKey = "chart.financial.bollinger.period";
        String deviationKey = "chart.financial.bollinger.stddev";
        String oldPeriod = ChartAssets.getString(periodKey, null);
        String oldDeviation = ChartAssets.getString(deviationKey, null);
        try {
            ChartAssets.setProperty(periodKey, "20");
            ChartAssets.setProperty(deviationKey, "2.0");
            DefaultChartModel model = series(false);
            BollingerBandsRenderer renderer = new BollingerBandsRenderer();
            List<FinancialRendererTestSupport.Draw> before = render(renderer, model, CONTEXT).draws;
            ChartAssets.setProperty(periodKey, "10");
            List<FinancialRendererTestSupport.Draw> newPeriod = render(new BollingerBandsRenderer(), model, CONTEXT).draws;
            assertNotEquals(before, newPeriod);
            assertEquals(newPeriod, render(renderer, model, CONTEXT).draws);
            ChartAssets.setProperty(deviationKey, "3.0");
            List<FinancialRendererTestSupport.Draw> newDeviation = render(new BollingerBandsRenderer(), model, CONTEXT).draws;
            assertNotEquals(newPeriod, newDeviation);
            assertEquals(newDeviation, render(renderer, model, CONTEXT).draws);
        } finally {
            restoreProperty(periodKey, oldPeriod);
            restoreProperty(deviationKey, oldDeviation);
        }
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) ChartAssets.removeProperty(key);
        else ChartAssets.setProperty(key, value);
    }

    private static void assertUpdatedAndMatchesFresh(Supplier<BaseRenderer> factory, BaseRenderer reused,
                                                   ChartModel model, List<FinancialRendererTestSupport.Draw> before) {
        BaseRenderer fresh = factory.get();
        List<FinancialRendererTestSupport.Draw> expected = render(fresh, model, CONTEXT).draws;
        assertFalse(expected.isEmpty());
        assertNotEquals(before, expected, "The fixture must change observable geometry");
        assertEquals(expected, render(reused, model, CONTEXT).draws);
        assertArrayEquals(fresh.getPreferredYRange(model), reused.getPreferredYRange(model));
    }

    private static DefaultChartModel series(boolean alternate) {
        DefaultChartModel model = new DefaultChartModel();
        fill(model, alternate);
        return model;
    }

    private static void fill(DefaultChartModel model, boolean alternate) {
        for (int i = 0; i < POINT_COUNT; i++) {
            int source = alternate ? POINT_COUNT - 1 - i : i;
            double close = 240 + source * 0.5 + 18 * Math.sin(source * 0.18) + 5 * Math.cos(source * 0.7);
            double low = close - (alternate ? 18 : 4);
            double high = close + (alternate ? 23 : 6);
            model.setPoint(i, close, low, high, close - (i % 2 == 0 ? 2 : -2), null);
        }
    }

    private static void append(CircularChartModel target, ChartModel source) {
        for (int i = 0; i < source.getPointCount(); i++) {
            target.setPoint(source.getX(i), source.getY(i), source.getMin(i), source.getMax(i),
                    source.getWeight(i), null);
        }
    }
}
