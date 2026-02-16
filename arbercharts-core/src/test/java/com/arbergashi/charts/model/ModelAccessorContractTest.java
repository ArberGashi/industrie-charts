package com.arbergashi.charts.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModelAccessorContractTest {

    @Test
    void signalModelReturnsSafeDefaultsForInvalidIndicesAndChannels() {
        DefaultSignalChartModel model = new DefaultSignalChartModel(2, 8);
        model.setSample(1.0, new double[]{2.0, 3.0});

        assertEquals(0.0, model.getX(-1), 0.0);
        assertEquals(0.0, model.getX(99), 0.0);
        assertEquals(0.0, model.getY(99), 0.0);
        assertEquals(0.0, model.getValue(99, 0), 0.0);
        assertEquals(0.0, model.getValue(0, 99), 0.0);
        assertEquals(0, model.getChannelData(-1).length);
        assertEquals(0, model.getChannelData(99).length);
        assertNull(model.getLabel(99));
    }

    @Test
    void financialModelReturnsSafeDefaultsForInvalidIndices() {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("series");
        model.setOHLC(1.0, 1.0, 2.0, 0.5, 1.5, 42.0, "p1");

        assertEquals(0.0, model.getX(-1), 0.0);
        assertEquals(0.0, model.getY(99), 0.0);
        assertEquals(0.0, model.getWeight(99), 0.0);
        assertEquals(0.0, model.getOpen(99), 0.0);
        assertEquals(0.0, model.getHigh(99), 0.0);
        assertEquals(0.0, model.getLow(99), 0.0);
        assertEquals(0.0, model.getClose(99), 0.0);
        assertEquals(0.0, model.getVolume(99), 0.0);
        assertNull(model.getLabel(99));
    }

    @Test
    void statisticalModelReturnsSafeDefaultsForInvalidIndices() {
        StatisticalChartModel model = new DefaultStatisticalChartModel("series");

        assertEquals(0.0, model.getMin(-1), 0.0);
        assertEquals(0.0, model.getMin(99), 0.0);
        assertEquals(0.0, model.getMax(-1), 0.0);
        assertEquals(0.0, model.getMax(99), 0.0);
        assertEquals(0.0, model.getWeight(99), 0.0);
    }
}
