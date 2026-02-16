package com.arbergashi.charts.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CircularChartModelTest {

    @Test
    void overwritesOldestPointWhenCapacityExceeded() {
        CircularChartModel model = new CircularChartModel(4);

        model.setXY(0, 0);
        model.setXY(1, 10);
        model.setXY(2, 20);
        model.setXY(3, 30);
        model.setXY(4, 40);

        assertEquals(4, model.getPointCount());
        assertEquals(1.0, model.getX(0), 0.0001);
        assertEquals(2.0, model.getX(1), 0.0001);
        assertEquals(3.0, model.getX(2), 0.0001);
        assertEquals(4.0, model.getX(3), 0.0001);
        assertEquals(10.0, model.getY(0), 0.0001);
        assertEquals(20.0, model.getY(1), 0.0001);
        assertEquals(30.0, model.getY(2), 0.0001);
        assertEquals(40.0, model.getY(3), 0.0001);
    }

    @Test
    void readCacheRefreshesWhenModelAdvances() {
        CircularChartModel model = new CircularChartModel(2);

        model.setXY(1, 10);
        model.setXY(2, 20);
        assertEquals(1.0, model.getX(0), 0.0001);

        model.setXY(3, 30);

        assertEquals(2.0, model.getX(0), 0.0001);
        assertEquals(3.0, model.getX(1), 0.0001);
    }
}
