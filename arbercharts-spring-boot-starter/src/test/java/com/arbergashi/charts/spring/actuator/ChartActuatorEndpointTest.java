package com.arbergashi.charts.spring.actuator;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartActuatorEndpointTest {
    @Test
    void exposesUnavailableMeasurementsWithoutInventedHealthCountsOrPercentiles() {
        ChartActuatorEndpoint endpoint = new ChartActuatorEndpoint();
        Map<String, Object> root = endpoint.charts();
        assertEquals(Set.of("measurementSource", "performance", "memory"), root.keySet());
        assertEquals("unavailable", root.get("measurementSource"));
        Map<?, ?> formatted = (Map<?, ?>) root.get("performance");
        assertEquals(Set.of("avgRenderTime", "maxRenderTime", "totalRenders"), formatted.keySet());
        assertEquals(0L, formatted.get("totalRenders"));
        assertNull(formatted.get("avgRenderTime"));
        assertNull(formatted.get("maxRenderTime"));
        Map<?, ?> formattedMemory = (Map<?, ?>) root.get("memory");
        assertEquals(Set.of("currentUsage", "peakUsage"), formattedMemory.keySet());
        assertNull(formattedMemory.get("currentUsage"));
        assertNull(formattedMemory.get("peakUsage"));

        Map<String, Object> performance = endpoint.chartsBySection("performance");
        assertEquals(Set.of("measurementSource", "avgRenderTime", "maxRenderTime", "totalRenders"),
                performance.keySet());
        assertEquals("unavailable", performance.get("measurementSource"));
        assertNull(performance.get("avgRenderTime"));
        assertNull(performance.get("maxRenderTime"));
        assertEquals(0L, performance.get("totalRenders"));
        Map<String, Object> memory = endpoint.chartsBySection("memory");
        assertEquals(Set.of("measurementSource", "currentBytes", "peakBytes"), memory.keySet());
        assertEquals("unavailable", memory.get("measurementSource"));
        assertNull(memory.get("currentBytes"));
        assertNull(memory.get("peakBytes"));
        assertEquals(Map.of("measurementSource", "unavailable"), endpoint.chartsBySection("renderers"));
    }

    @Test
    void reportsTheArithmeticMeanMaximumCountAndSuppliedMemoryValues() {
        ChartActuatorEndpoint endpoint = new ChartActuatorEndpoint();
        endpoint.recordRender(2, 512);
        endpoint.recordRender(5, 1024);
        endpoint.recordRender(11, 64);

        assertEquals(Map.of("measurementSource", "manual", "avgRenderTime", 6.0,
                "maxRenderTime", 11.0, "totalRenders", 3L), endpoint.chartsBySection("performance"));
        assertEquals(Map.of("measurementSource", "manual", "currentBytes", 64L, "peakBytes", 1024L),
                endpoint.chartsBySection("memory"));
        assertEquals(Map.of("measurementSource", "manual",
                "performance", Map.of("avgRenderTime", "6.0ms", "maxRenderTime", "11.0ms", "totalRenders", 3L),
                "memory", Map.of("currentUsage", "64B", "peakUsage", "1.0KB")), endpoint.charts());
    }

    @Test
    void acceptsZeroAndKeepsTheMeanFiniteForLargeFiniteSamples() {
        ChartActuatorEndpoint zero = new ChartActuatorEndpoint();
        zero.recordRender(0, 0);
        assertEquals(Map.of("measurementSource", "manual", "avgRenderTime", 0.0,
                "maxRenderTime", 0.0, "totalRenders", 1L), zero.chartsBySection("performance"));
        assertEquals(Map.of("measurementSource", "manual", "currentBytes", 0L, "peakBytes", 0L),
                zero.chartsBySection("memory"));

        ChartActuatorEndpoint large = new ChartActuatorEndpoint();
        large.recordRender(Double.MAX_VALUE, Long.MAX_VALUE);
        large.recordRender(Double.MAX_VALUE, 1);
        assertEquals(Double.MAX_VALUE, large.chartsBySection("performance").get("avgRenderTime"));
        assertEquals(Double.MAX_VALUE, large.chartsBySection("performance").get("maxRenderTime"));
        assertEquals(Long.MAX_VALUE, large.chartsBySection("memory").get("peakBytes"));
    }

    @Test
    void rejectsInvalidSamplesWithoutChangingAnyStatistic() {
        ChartActuatorEndpoint endpoint = new ChartActuatorEndpoint();
        endpoint.recordRender(3, 100);
        Map<String, Object> before = endpoint.charts();
        Map<String, Object> beforePerformance = endpoint.chartsBySection("performance");
        Map<String, Object> beforeMemory = endpoint.chartsBySection("memory");
        for (double invalid : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> endpoint.recordRender(invalid, 500));
        }
        assertThrows(IllegalArgumentException.class, () -> endpoint.recordRender(50, -1));
        assertEquals(before, endpoint.charts());
        assertEquals(beforePerformance, endpoint.chartsBySection("performance"));
        assertEquals(beforeMemory, endpoint.chartsBySection("memory"));
    }

    @Test
    void countsConcurrentSamplesWithoutLosingUpdatesAndPublishesConsistentSnapshots() throws Exception {
        ChartActuatorEndpoint endpoint = new ChartActuatorEndpoint();
        int writers = 8;
        int samplesPerWriter = 10_000;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(writers);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int writer = 1; writer <= writers; writer++) {
                int duration = writer;
                futures.add(executor.submit(() -> {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    for (int sample = 0; sample < samplesPerWriter; sample++) {
                        endpoint.recordRender(duration, duration * 10L);
                    }
                    return null;
                }));
            }
            start.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            do {
                Map<String, Object> snapshot = endpoint.charts();
                Map<?, ?> performance = (Map<?, ?>) snapshot.get("performance");
                Map<?, ?> memory = (Map<?, ?>) snapshot.get("memory");
                if ((long) performance.get("totalRenders") == 0) {
                    assertEquals("unavailable", snapshot.get("measurementSource"));
                    assertNull(performance.get("maxRenderTime"));
                    assertNull(memory.get("peakUsage"));
                } else {
                    assertEquals("manual", snapshot.get("measurementSource"));
                    String maximum = (String) performance.get("maxRenderTime");
                    String peak = (String) memory.get("peakUsage");
                    double maximumMs = Double.parseDouble(maximum.substring(0, maximum.length() - 2));
                    long peakBytes = Long.parseLong(peak.substring(0, peak.length() - 1));
                    assertEquals(maximumMs * 10, peakBytes,
                            "Performance and memory must describe the same completed sample set");
                }
            } while (futures.stream().anyMatch(future -> !future.isDone()) && System.nanoTime() < deadline);
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
            Map<String, Object> performance = endpoint.chartsBySection("performance");
            assertEquals((long) writers * samplesPerWriter, performance.get("totalRenders"));
            assertEquals(4.5, (double) performance.get("avgRenderTime"), 1e-9);
            assertEquals(8.0, performance.get("maxRenderTime"));
            Map<String, Object> memory = endpoint.chartsBySection("memory");
            assertEquals(80L, memory.get("peakBytes"));
            long current = (long) memory.get("currentBytes");
            assertTrue(current >= 10 && current <= 80 && current % 10 == 0);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void performanceSnapshotsKeepTheCountMeanAndMaximumFromTheSameSampleSet() throws Exception {
        ChartActuatorEndpoint endpoint = new ChartActuatorEndpoint();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> writer = executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                for (int sample = 1; sample <= 100_000; sample++) {
                    endpoint.recordRender(sample, sample);
                }
                return null;
            });
            start.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            do {
                Map<String, Object> snapshot = endpoint.chartsBySection("performance");
                long count = (long) snapshot.get("totalRenders");
                if (count > 0) {
                    assertEquals((count + 1) / 2.0, snapshot.get("avgRenderTime"));
                    assertEquals((double) count, snapshot.get("maxRenderTime"));
                }
            } while (!writer.isDone() && System.nanoTime() < deadline);
            writer.get(5, TimeUnit.SECONDS);
            assertEquals(100_000L, endpoint.chartsBySection("performance").get("totalRenders"));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
