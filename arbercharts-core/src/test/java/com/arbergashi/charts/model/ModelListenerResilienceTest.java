package com.arbergashi.charts.model;

import org.junit.jupiter.api.Test;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModelListenerResilienceTest {

    @Test
    void defaultModelIsolatesFailingListener() {
        withModelLoggerSuppressed(DefaultChartModel.class, () -> {
        DefaultChartModel model = new DefaultChartModel("series");
        AtomicInteger called = new AtomicInteger();

        model.setChangeListener(() -> {
            throw new RuntimeException("boom");
        });
        model.setChangeListener(called::incrementAndGet);

        model.setXY(1.0, 2.0);

        assertEquals(1, called.get());
        });
    }

    @Test
    void circularModelIsolatesFailingListener() {
        withModelLoggerSuppressed(CircularChartModel.class, () -> {
        CircularChartModel model = new CircularChartModel("series", 8);
        AtomicInteger called = new AtomicInteger();

        model.setChangeListener(() -> {
            throw new RuntimeException("boom");
        });
        model.setChangeListener(called::incrementAndGet);

        model.setXY(1.0, 2.0);

        assertEquals(1, called.get());
        });
    }

    @Test
    void defaultModelFallsBackWhenDispatchExecutorRejects() {
        withModelLoggerSuppressed(DefaultChartModel.class, () -> {
        DefaultChartModel model = new DefaultChartModel("series");
        AtomicInteger called = new AtomicInteger();

        model.setDispatchOnEdt(true);
        model.setDispatchExecutor(command -> {
            throw new RejectedExecutionException("rejected");
        });
        model.setChangeListener(called::incrementAndGet);

        model.setXY(1.0, 2.0);

        assertEquals(1, called.get());
        });
    }

    @Test
    void circularModelFallsBackWhenDispatchExecutorRejects() {
        withModelLoggerSuppressed(CircularChartModel.class, () -> {
        CircularChartModel model = new CircularChartModel("series", 8);
        AtomicInteger called = new AtomicInteger();

        model.setDispatchOnEdt(true);
        model.setDispatchExecutor(command -> {
            throw new RejectedExecutionException("rejected");
        });
        model.setChangeListener(called::incrementAndGet);

        model.setXY(1.0, 2.0);

        assertEquals(1, called.get());
        });
    }

    @Test
    void signalModelIsolatesFailingListener() {
        withModelLoggerSuppressed(DefaultSignalChartModel.class, () -> {
        DefaultSignalChartModel model = new DefaultSignalChartModel(1, 8);
        AtomicInteger called = new AtomicInteger();

        model.setChangeListener(() -> {
            throw new RuntimeException("boom");
        });
        model.setChangeListener(called::incrementAndGet);

        model.setSample(1.0, new double[]{2.0});

        assertEquals(1, called.get());
        });
    }

    @Test
    void signalModelFallsBackWhenDispatchExecutorRejects() {
        withModelLoggerSuppressed(DefaultSignalChartModel.class, () -> {
        DefaultSignalChartModel model = new DefaultSignalChartModel(1, 8);
        AtomicInteger called = new AtomicInteger();

        model.setDispatchOnEdt(true);
        model.setDispatchExecutor(command -> {
            throw new RejectedExecutionException("rejected");
        });
        model.setChangeListener(called::incrementAndGet);

        model.setSample(1.0, new double[]{2.0});

        assertEquals(1, called.get());
        });
    }

    @Test
    void financialModelIsolatesFailingListener() {
        withModelLoggerSuppressed(DefaultFinancialChartModel.class, () -> {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("series");
        AtomicInteger called = new AtomicInteger();

        model.setChangeListener(() -> {
            throw new RuntimeException("boom");
        });
        model.setChangeListener(called::incrementAndGet);

        model.setOHLC(1.0, 1.0, 2.0, 0.5, 1.5);

        assertEquals(1, called.get());
        });
    }

    @Test
    void financialModelFallsBackWhenDispatchExecutorRejects() {
        withModelLoggerSuppressed(DefaultFinancialChartModel.class, () -> {
        DefaultFinancialChartModel model = new DefaultFinancialChartModel("series");
        AtomicInteger called = new AtomicInteger();

        model.setDispatchOnEdt(true);
        model.setDispatchExecutor(command -> {
            throw new RejectedExecutionException("rejected");
        });
        model.setChangeListener(called::incrementAndGet);

        model.setOHLC(1.0, 1.0, 2.0, 0.5, 1.5);

        assertEquals(1, called.get());
        });
    }

    @Test
    void statisticalModelIsolatesFailingListener() {
        withModelLoggerSuppressed(DefaultStatisticalChartModel.class, () -> {
        DefaultStatisticalChartModel model = new DefaultStatisticalChartModel("series");
        AtomicInteger called = new AtomicInteger();

        model.setChangeListener(() -> {
            throw new RuntimeException("boom");
        });
        model.setChangeListener(called::incrementAndGet);

        model.setBoxPlot(1.0, 2.0, 1.0, 3.0, 0.5, 3.5, "p1");

        assertEquals(1, called.get());
        });
    }

    @Test
    void statisticalModelFallsBackWhenDispatchExecutorRejects() {
        withModelLoggerSuppressed(DefaultStatisticalChartModel.class, () -> {
        DefaultStatisticalChartModel model = new DefaultStatisticalChartModel("series");
        AtomicInteger called = new AtomicInteger();

        model.setDispatchOnEdt(true);
        model.setDispatchExecutor(command -> {
            throw new RejectedExecutionException("rejected");
        });
        model.setChangeListener(called::incrementAndGet);

        model.setBoxPlot(1.0, 2.0, 1.0, 3.0, 0.5, 3.5, "p1");

        assertEquals(1, called.get());
        });
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

    private static void withModelLoggerSuppressed(Class<?> modelType, Runnable assertion) {
        Logger logger = Logger.getLogger(modelType.getName());
        Level previous = logger.getLevel();
        boolean previousUseParentHandlers = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.OFF);
        try {
            assertion.run();
        } finally {
            logger.setLevel(previous);
            logger.setUseParentHandlers(previousUseParentHandlers);
        }
    }
}
