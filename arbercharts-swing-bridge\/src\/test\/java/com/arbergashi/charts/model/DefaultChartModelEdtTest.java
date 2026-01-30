package com.arbergashi.charts.model;

import org.junit.jupiter.api.Test;

import java.awt.EventQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultChartModelEdtTest {

    @Test
    void listenersDispatchOnEdtWhenEnabled() throws Exception {
        DefaultChartModel model = new DefaultChartModel("m");
        model.setDispatchOnEdt(true);

        CountDownLatch latch = new CountDownLatch(1);
        model.setChangeListener(() -> {
            if (EventQueue.isDispatchThread()) {
                latch.countDown();
            }
        });

        Thread worker = new Thread(() -> model.setXY(1, 2));
        worker.start();
        worker.join(1000);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }
}
