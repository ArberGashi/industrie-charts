package com.arbergashi.charts.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

class ConcurrentModelTest {

    @Test
    void concurrentReadsDuringWrites() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("arbercharts.stress"),
                "Set -Darbercharts.stress=true to run the concurrency stress test.");
        Assumptions.assumeTrue(Boolean.getBoolean("arbercharts.stress.default"),
                "Set -Darbercharts.stress.default=true to run the DefaultChartModel stress test.");

        DefaultChartModel model = new DefaultChartModel("stress");
        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicLong counter = new AtomicLong(0L);
        AtomicLong successfulWrites = new AtomicLong();
        List<Future<?>> tasks = new ArrayList<>();

        ExecutorService writers = Executors.newFixedThreadPool(4);
        for (int t = 0; t < 4; t++) {
            tasks.add(writers.submit(() -> {
                while (!stop.get()) {
                    long id = counter.getAndIncrement();
                    double x = id;
                    double y = id * 2.0;
                    model.setPoint(x, y, y - 1.0, y + 1.0, 1.0, Long.toString(id));
                    successfulWrites.incrementAndGet();
                }
            }));
        }

        long iterations = 0L;
        long successfulReads = 0L;
        boolean failed = false;
        String failureMessage = null;
        long duration = Long.getLong("arbercharts.stress.seconds", 5L);
        long endTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(duration);

        try {
            while (System.nanoTime() < endTime) {
                iterations++;
                int size = model.getPointCount();
                if (size <= 0) {
                    continue;
                }
                int idx = ThreadLocalRandom.current().nextInt(size);
                try {
                    double x = model.getX(idx);
                    double y = model.getY(idx);
                    double min = model.getMin(idx);
                    double max = model.getMax(idx);
                    if (y != x * 2.0 || min != y - 1.0 || max != y + 1.0) {
                        failed = true;
                        failureMessage = "Data mismatch at idx=" + idx
                                + " x=" + x + " y=" + y + " min=" + min + " max=" + max
                                + " after iterations=" + iterations;
                        break;
                    }
                    successfulReads++;
                } catch (ArrayIndexOutOfBoundsException ex) {
                    failed = true;
                    failureMessage = "Array index failure at idx=" + idx
                            + " size=" + size + " after iterations=" + iterations;
                    break;
                }
            }
        } finally {
            stopAndJoinWriters(writers, stop, tasks);
        }

        if (failed) {
            Assertions.fail(failureMessage);
        }
        Assertions.assertTrue(successfulWrites.get() > 0, "The stress run must complete real writes");
        Assertions.assertTrue(successfulReads > 0, "The stress run must validate non-empty data");
    }

    @Test
    void concurrentReadsDuringWritesCircular() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("arbercharts.stress"),
                "Set -Darbercharts.stress=true to run the concurrency stress test.");

        CircularChartModel model = new CircularChartModel("stress", 4096);
        model.setLabelsEnabled(false);
        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicLong counter = new AtomicLong(0L);
        AtomicLong successfulWrites = new AtomicLong();
        List<Future<?>> tasks = new ArrayList<>();
        double[] buffer = new double[5];

        ExecutorService writers = Executors.newFixedThreadPool(4);
        for (int t = 0; t < 4; t++) {
            tasks.add(writers.submit(() -> {
                while (!stop.get()) {
                    long id = counter.getAndIncrement();
                    double x = id;
                    double y = id * 2.0;
                    model.setPoint(x, y, y - 1.0, y + 1.0, 1.0, null);
                    successfulWrites.incrementAndGet();
                }
            }));
        }

        long iterations = 0L;
        long successfulReads = 0L;
        boolean failed = false;
        String failureMessage = null;
        long duration = Long.getLong("arbercharts.stress.seconds", 5L);
        long endTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(duration);

        try {
            while (System.nanoTime() < endTime) {
                iterations++;
                int size = model.getPointCount();
                if (size <= 0) {
                    continue;
                }
                int idx = ThreadLocalRandom.current().nextInt(size);
                if (!model.readPoint(idx, buffer)) {
                    continue;
                }
                double x = buffer[0];
                double y = buffer[1];
                double min = buffer[2];
                double max = buffer[3];
                if (y != x * 2.0 || min != y - 1.0 || max != y + 1.0) {
                    long[] seq = new long[2];
                    model.readSequencePair(idx, seq);
                    failed = true;
                    failureMessage = "Data mismatch at idx=" + idx
                            + " x=" + x + " y=" + y + " min=" + min + " max=" + max
                            + " seq1=" + seq[0] + " seq2=" + seq[1]
                            + " after iterations=" + iterations;
                    break;
                }
                successfulReads++;
            }
        } finally {
            stopAndJoinWriters(writers, stop, tasks);
        }

        if (failed) {
            Assertions.fail(failureMessage);
        }
        Assertions.assertTrue(successfulWrites.get() > 0, "The stress run must complete real writes");
        Assertions.assertTrue(successfulReads > 0, "The stress run must validate non-empty data");
    }

    private static void stopAndJoinWriters(ExecutorService writers, AtomicBoolean stop,
                                          List<Future<?>> tasks) throws Exception {
        stop.set(true);
        writers.shutdown();
        boolean terminated = false;
        try {
            terminated = writers.awaitTermination(2, TimeUnit.SECONDS);
        } finally {
            if (!terminated) writers.shutdownNow();
        }
        Assertions.assertTrue(terminated, "All stress writers must finish after the stop signal");
        for (Future<?> task : tasks) task.get();
    }
}
