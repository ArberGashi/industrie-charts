package com.arbergashi.charts.visualverifier.service;

import com.arbergashi.charts.visualverifier.dto.BenchmarkResult;
import com.arbergashi.charts.visualverifier.dto.RenderRequest;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Service for benchmarking renderer performance.
 *
 * <p>Measures render time with proper warmup, calculates percentiles (p50, p99, p999),
 * and tracks memory usage. Designed for Zero-GC validation.
 *
 * @since 2.0.0
 */
@Service
public class BenchmarkService {

    private final ChartRenderService renderService;

    public BenchmarkService(ChartRenderService renderService) {
        this.renderService = renderService;
    }

    /**
     * Benchmarks a renderer with specified parameters.
     *
     * @param rendererClass renderer class name
     * @param iterations    number of measured iterations
     * @param warmup        number of warmup iterations
     * @return benchmark results with latency distribution
     */
    public BenchmarkResult benchmark(String rendererClass, int iterations, int warmup) {
        RenderRequest request = RenderRequest.of(rendererClass);

        // Warmup phase (JIT compilation)
        for (int i = 0; i < warmup; i++) {
            renderService.render(request);
        }

        // Force GC before measurement
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        // Measurement phase
        long[] times = new long[iterations];
        long startMemory = usedMemory();

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            renderService.render(request);
            times[i] = System.nanoTime() - start;
        }

        long endMemory = usedMemory();
        long memoryUsed = Math.max(0, endMemory - startMemory);

        // Calculate statistics
        Arrays.sort(times);
        double avgTimeMs = Arrays.stream(times).average().orElse(0.0) / 1_000_000.0;
        double p50Ms = percentile(times, 0.50);
        double p99Ms = percentile(times, 0.99);
        double p999Ms = percentile(times, 0.999);
        double throughput = avgTimeMs > 0 ? 1000.0 / avgTimeMs : 0;

        return BenchmarkResult.builder()
                .renderer(rendererClass)
                .iterations(iterations)
                .avgTimeMs(round(avgTimeMs))
                .p50Ms(round(p50Ms))
                .p99Ms(round(p99Ms))
                .p999Ms(round(p999Ms))
                .memoryPeakBytes(memoryUsed)
                .throughput(round(throughput))
                .build();
    }

    /**
     * Quick benchmark with default settings.
     */
    public BenchmarkResult benchmark(String rendererClass) {
        return benchmark(rendererClass, 100, 20);
    }

    private double percentile(long[] sortedTimes, double p) {
        int index = (int) Math.ceil(p * sortedTimes.length) - 1;
        index = Math.clamp(index, 0, sortedTimes.length - 1);
        return sortedTimes[index] / 1_000_000.0;
    }

    private long usedMemory() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

