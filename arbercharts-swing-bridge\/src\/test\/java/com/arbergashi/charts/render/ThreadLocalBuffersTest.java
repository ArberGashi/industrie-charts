package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ThreadLocalBuffersTest {

    @Test
    public void threadLocalBuffersAreIndependentUnderConcurrency() throws Exception {
        DummyRenderer r = new DummyRenderer();
        // record main thread buffer
        double[] mainBuf = r.pBuffer();
        assertNotNull(mainBuf);
        final Set<double[]> seen = Collections.synchronizedSet(new HashSet<>());

        int threads = 12;
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        CountDownLatch started = new CountDownLatch(threads);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            ex.submit(() -> {
                try {
                    started.countDown();
                    started.await();
                    double[] buf = r.pBuffer();
                    // mutate and keep reference
                    buf[0] = idx + 0.5;
                    seen.add(buf);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(done.await(5, TimeUnit.SECONDS));
        ex.shutdownNow();
        // ensure we saw as many distinct buffers as threads (at least >1)
        assertTrue(seen.size() > 1);
        // and main thread's buffer wasn't stomped by other threads
        boolean mainAppears = seen.stream().anyMatch(b -> b == mainBuf);
        assertFalse(mainAppears, "Main thread buffer should be distinct from worker buffers");
    }

    private static class DummyRenderer extends BaseRenderer {
        public DummyRenderer() {
            super("dummy");
        }

        @Override
        public String getName() {
            return "dummy";
        }

        protected void drawData(java.awt.Graphics2D g, ChartModel model, PlotContext ctx) { /* no-op */ }
    }
}
