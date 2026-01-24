package com.arbergashi.charts.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CircularChartModelTest {

    @Test
    void overwritesOldestPointWhenCapacityExceeded() {
        CircularChartModel model = new CircularChartModel(4);

        model.addXY(0, 0);
        model.addXY(1, 10);
        model.addXY(2, 20);
        model.addXY(3, 30);
        model.addXY(4, 40);

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
}
