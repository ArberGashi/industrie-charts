package com.arbergashi.charts.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void snapshotsRefreshAfterFullWrapAndClear() {
        CircularChartModel model = new CircularChartModel(4);
        for (int i = 0; i < 4; i++) model.setXY(i, i * 10.0);
        assertArrayEquals(new double[]{0, 1, 2, 3}, model.getXData());
        assertArrayEquals(new double[]{0, 10, 20, 30}, model.getYData());

        for (int i = 4; i < 8; i++) model.setXY(i, i * 10.0);

        assertEquals(4, model.getPointCount());
        assertArrayEquals(new double[]{4, 5, 6, 7}, model.getXData());
        assertArrayEquals(new double[]{40, 50, 60, 70}, model.getYData());
        model.clear();
        assertEquals(0, model.getPointCount());
        assertArrayEquals(new double[]{0, 0, 0, 0}, model.getDataRange());
        model.setXY(9, 90);
        assertEquals(1, model.getPointCount());
        assertEquals(9, model.getXData()[0]);
        assertEquals(90, model.getYData()[0]);
    }

    @Test
    void listenerCanAppendToTheSameModel() {
        CircularChartModel model = new CircularChartModel(4);
        AtomicBoolean appended = new AtomicBoolean();
        model.setChangeListener(() -> {
            if (appended.compareAndSet(false, true)) {
                model.setXY(2, 20);
            }
        });

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> model.setXY(1, 10));

        assertEquals(2, model.getPointCount());
        assertEquals(2, model.getUpdateStamp());
        assertEquals(1.0, model.getX(0));
        assertEquals(2.0, model.getX(1));
    }

    @Test
    void listenerDoesNotBlockAnotherWriter() throws InterruptedException {
        CircularChartModel model = new CircularChartModel(4);
        AtomicBoolean firstNotification = new AtomicBoolean(true);
        AtomicBoolean completedDuringNotification = new AtomicBoolean();
        CountDownLatch secondWriterCompleted = new CountDownLatch(1);
        model.setChangeListener(() -> {
            if (!firstNotification.getAndSet(false)) return;
            Thread.ofVirtual().start(() -> {
                try {
                    model.setXY(2, 20);
                } finally {
                    secondWriterCompleted.countDown();
                }
            });
            try {
                completedDuringNotification.set(secondWriterCompleted.await(2, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });

        model.setXY(1, 10);

        assertTrue(secondWriterCompleted.await(2, TimeUnit.SECONDS));
        assertTrue(completedDuringNotification.get(), "Callbacks must run after releasing the writer lock");
        assertEquals(2, model.getPointCount());
    }

    @Test
    void timestampReadsRespectLogicalIndicesBeforeAndAfterWrap() {
        CircularChartModel model = new CircularChartModel(2);
        model.setPoint(1, 10, 10, 10, 1, null, ProvenanceFlags.ORIGINAL, (short) 0, 100L);

        assertEquals(100L, model.getTimestampNanos(0));
        assertEquals(0L, model.getTimestampNanos(-2));
        assertEquals(0L, model.getTimestampNanos(1));
        assertEquals(0L, model.getTimestampNanos(2));

        model.setPoint(2, 20, 20, 20, 1, null, ProvenanceFlags.ORIGINAL, (short) 0, 200L);
        model.setPoint(3, 30, 30, 30, 1, null, ProvenanceFlags.ORIGINAL, (short) 0, 300L);

        assertEquals(200L, model.getTimestampNanos(0));
        assertEquals(300L, model.getTimestampNanos(1));
        assertEquals(0L, model.getTimestampNanos(-1));
        assertEquals(0L, model.getTimestampNanos(2));
        assertEquals(0L, model.getTimestampNanos(Integer.MAX_VALUE));

        model.clear();
        assertEquals(0L, model.getTimestampNanos(0));
    }
}
