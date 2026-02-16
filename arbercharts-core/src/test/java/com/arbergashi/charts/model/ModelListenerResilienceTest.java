package com.arbergashi.charts.model;

import org.junit.jupiter.api.Test;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModelListenerResilienceTest {

    @Test
    void defaultModelIsolatesFailingListener() {
        DefaultChartModel model = new DefaultChartModel("series");
        AtomicInteger called = new AtomicInteger();

        model.setChangeListener(() -> {
            throw new RuntimeException("boom");
        });
        model.setChangeListener(called::incrementAndGet);

        model.setXY(1.0, 2.0);

        assertEquals(1, called.get());
    }

    @Test
    void circularModelIsolatesFailingListener() {
        CircularChartModel model = new CircularChartModel("series", 8);
        AtomicInteger called = new AtomicInteger();

        model.setChangeListener(() -> {
            throw new RuntimeException("boom");
        });
        model.setChangeListener(called::incrementAndGet);

        model.setXY(1.0, 2.0);

        assertEquals(1, called.get());
    }

    @Test
    void defaultModelFallsBackWhenDispatchExecutorRejects() {
        DefaultChartModel model = new DefaultChartModel("series");
        AtomicInteger called = new AtomicInteger();

        model.setDispatchOnEdt(true);
        model.setDispatchExecutor(command -> {
            throw new RejectedExecutionException("rejected");
        });
        model.setChangeListener(called::incrementAndGet);

        model.setXY(1.0, 2.0);

        assertEquals(1, called.get());
    }

    @Test
    void circularModelFallsBackWhenDispatchExecutorRejects() {
        CircularChartModel model = new CircularChartModel("series", 8);
        AtomicInteger called = new AtomicInteger();

        model.setDispatchOnEdt(true);
        model.setDispatchExecutor(command -> {
            throw new RejectedExecutionException("rejected");
        });
        model.setChangeListener(called::incrementAndGet);

        model.setXY(1.0, 2.0);

        assertEquals(1, called.get());
    }

    @Test
    void defaultModelOutOfRangeAccessorsReturnSafeDefaults() {
        DefaultChartModel model = new DefaultChartModel("series");
        model.setXY(1.0, 2.0);

        assertEquals(0.0, model.getX(-1), 0.0);
        assertEquals(0.0, model.getY(99), 0.0);
        assertEquals(0.0, model.getMin(99), 0.0);
        assertEquals(0.0, model.getMax(99), 0.0);
        assertEquals(0.0, model.getWeight(99), 0.0);
        assertEquals(0.0, model.getValue(99, 0), 0.0);
        assertNull(model.getLabel(99));
    }
}

