package com.arbergashi.charts.api;

import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.model.DefaultFinancialChartModel;
import com.arbergashi.charts.util.NiceScale;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPlotContextTest {
    private static final ArberRect BOUNDS = new ArberRect(10, 20, 300, 300);

    @Test
    void mapToPixelAndBackIsStable() {
        ArberRect bounds = new ArberRect(0, 0, 100, 100);
        DefaultPlotContext context = new DefaultPlotContext(
                bounds,
                0, 10,
                0, 20,
                false,
                false,
                false,
                NiceScale.ScaleMode.LINEAR,
                NiceScale.ScaleMode.LINEAR,
                ChartThemes.getDarkTheme(),
                null,
                null,
                null
        );

        double[] pixel = new double[2];
        double[] data = new double[2];

        context.mapToPixel(5, 10, pixel);
        assertEquals(50.0, pixel[0], 0.0001);
        assertEquals(50.0, pixel[1], 0.0001);

        context.mapToData(pixel[0], pixel[1], data);
        assertEquals(5.0, data[0], 0.0001);
        assertEquals(10.0, data[1], 0.0001);
    }

    @Test
    void automaticRangesContainNegativeMixedAndConstantSamples() {
        for (double[] values : new double[][]{{-20, -10}, {-10, -10}, {0, 0}, {10, 10}, {-10, 10},
                {-Double.MAX_VALUE, Double.MAX_VALUE}, {-Double.MAX_VALUE, -Double.MAX_VALUE},
                {Double.MAX_VALUE, Double.MAX_VALUE}, {-Double.MIN_VALUE, -Double.MIN_VALUE}}) {
            DefaultChartModel model = new DefaultChartModel("range");
            model.setXY(42, values[0]);
            model.setXY(42, values[1]);
            DefaultPlotContext context = new DefaultPlotContext(BOUNDS, model,
                    Double.NaN, Double.NaN, Double.NaN, Double.NaN);
            assertTrue(Double.isFinite(context.getMinX()) && Double.isFinite(context.getMaxX()));
            assertTrue(Double.isFinite(context.getMinY()) && Double.isFinite(context.getMaxY()));
            assertTrue(context.getMinX() < context.getMaxX());
            assertTrue(context.getMinY() < context.getMaxY());
            for (double value : values) {
                assertTrue(context.getMinY() <= value && context.getMaxY() >= value);
                double[] pixel = new double[2];
                context.mapToPixel(42, value, pixel);
                assertTrue(pixel[0] >= BOUNDS.minX() && pixel[0] <= BOUNDS.maxX());
                assertTrue(pixel[1] >= BOUNDS.minY() - 1e-9 && pixel[1] <= BOUNDS.maxY() + 1e-9);
            }
        }
    }

    @Test
    void financialHeadroomStaysFiniteAndContainsConstantAndExtremePrices() {
        for (double[] prices : new double[][]{{1, 10}, {10, 10}, {1e-300, 1e-300},
                {Double.MAX_VALUE, Double.MAX_VALUE}, {-Double.MAX_VALUE, Double.MAX_VALUE}}) {
            DefaultFinancialChartModel model = new DefaultFinancialChartModel("prices");
            model.setOHLC(0, prices[0], prices[1], prices[0], prices[1]);
            DefaultPlotContext context = new DefaultPlotContext(BOUNDS, model,
                    Double.NaN, Double.NaN, Double.NaN, Double.NaN);
            assertTrue(Double.isFinite(context.getMinY()) && Double.isFinite(context.getMaxY()));
            assertTrue(context.getMinY() < context.getMaxY());
            assertTrue(context.getMinY() <= prices[0]);
            assertTrue(context.getMaxY() >= prices[1]);
        }
    }

    @Test
    void logarithmicDecadesUseKnownPixelsAndInvertIndependently() {
        for (boolean invertedX : new boolean[]{false, true}) {
            for (boolean invertedY : new boolean[]{false, true}) {
                DefaultPlotContext context = new DefaultPlotContext(BOUNDS, 1, 1000, 1, 1000,
                        false, invertedX, invertedY, NiceScale.ScaleMode.LOGARITHMIC, NiceScale.ScaleMode.LOGARITHMIC);
                double[] pixel = new double[2];
                double[] data = new double[2];
                for (int decade = 0; decade <= 3; decade++) {
                    double value = Math.pow(10, decade);
                    double expectedX = 10 + (invertedX ? 3 - decade : decade) * 100;
                    double expectedY = 20 + (invertedY ? decade : 3 - decade) * 100;
                    context.mapToPixel(value, value, pixel);
                    assertEquals(expectedX, pixel[0], 1e-9);
                    assertEquals(expectedY, pixel[1], 1e-9);
                    context.mapToData(expectedX, expectedY, data);
                    assertEquals(value, data[0], value * 1e-12);
                    assertEquals(value, data[1], value * 1e-12);
                }
            }
        }
    }

    @Test
    void legacyLogarithmicYFlagAndZoomedBoundsUseTheSameTransform() {
        DefaultPlotContext context = new DefaultPlotContext(BOUNDS, 0, 3, 1, 1000,
                true, NiceScale.ScaleMode.LINEAR, NiceScale.ScaleMode.LINEAR);
        double[] pixel = new double[2];
        double[] data = new double[2];
        context.mapToPixel(1, 10, pixel);
        assertEquals(110, pixel[0], 1e-9);
        assertEquals(220, pixel[1], 1e-9);
        context.setMinY(10).setMaxY(1000);
        context.mapToPixel(1, 100, pixel);
        assertEquals(170, pixel[1], 1e-9);
        context.mapToData(110, 170, data);
        assertEquals(100, data[1], 1e-10);
    }

    @Test
    void logarithmicZoomRetainsRepresentableDifferencesAtLargeOffsets() {
        double min = 1e16;
        double max = min + 4;
        DefaultPlotContext context = new DefaultPlotContext(BOUNDS, min, max, min, max,
                true, NiceScale.ScaleMode.LOGARITHMIC, NiceScale.ScaleMode.LOGARITHMIC);
        double[] pixel = new double[2];
        double[] data = new double[2];
        context.mapToPixel(min + 2, min + 2, pixel);
        assertEquals(160, pixel[0], 1e-9);
        assertEquals(170, pixel[1], 1e-9);
        context.mapToData(160, 170, data);
        assertEquals(min + 2, data[0]);
        assertEquals(min + 2, data[1]);
    }

    @Test
    void finiteExtremeDomainsDoNotOverflowTheCoordinateSpan() {
        DefaultPlotContext linear = new DefaultPlotContext(BOUNDS, -Double.MAX_VALUE, Double.MAX_VALUE,
                -Double.MAX_VALUE, Double.MAX_VALUE, false, NiceScale.ScaleMode.LINEAR, NiceScale.ScaleMode.LINEAR);
        double[] pixel = new double[2];
        double[] data = new double[2];
        linear.mapToPixel(0, 0, pixel);
        assertEquals(160, pixel[0]);
        assertEquals(170, pixel[1]);
        linear.mapToData(160, 170, data);
        assertEquals(0, data[0]);
        assertEquals(0, data[1]);

        DefaultPlotContext logarithmic = new DefaultPlotContext(BOUNDS, 1e-300, 1e300, 1e-300, 1e300,
                true, NiceScale.ScaleMode.LOGARITHMIC, NiceScale.ScaleMode.LOGARITHMIC);
        logarithmic.mapToPixel(1, 1, pixel);
        assertEquals(160, pixel[0], 1e-9);
        assertEquals(170, pixel[1], 1e-9);
        logarithmic.mapToData(160, 170, data);
        assertEquals(1, data[0], 1e-12);
        assertEquals(1, data[1], 1e-12);
    }

    @Test
    void invalidLogarithmicValuesRemainMissingWithoutAffectingTheOtherAxis() {
        DefaultPlotContext context = new DefaultPlotContext(BOUNDS, 0, 3, 1, 1000,
                true, NiceScale.ScaleMode.LINEAR, NiceScale.ScaleMode.LOGARITHMIC);
        double[] pixel = new double[2];
        for (double y : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            context.mapToPixel(1, y, pixel);
            assertEquals(110, pixel[0], 1e-9);
            assertTrue(Double.isNaN(pixel[1]));
        }
        context.setMinY(0);
        context.mapToPixel(1, 10, pixel);
        assertTrue(Double.isNaN(pixel[1]));
        context.mapToData(110, 170, pixel);
        assertEquals(1, pixel[0], 1e-9);
        assertTrue(Double.isNaN(pixel[1]));
    }
}
