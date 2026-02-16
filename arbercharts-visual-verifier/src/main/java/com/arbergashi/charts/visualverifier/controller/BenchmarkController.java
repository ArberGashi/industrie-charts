package com.arbergashi.charts.visualverifier.controller;

import com.arbergashi.charts.visualverifier.dto.BenchmarkResult;
import com.arbergashi.charts.visualverifier.service.BenchmarkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for performance benchmarking.
 *
 * <p>Measures render performance with configurable iterations and warmup.
 * Returns latency distribution (p50, p99, p999) and throughput metrics.
 *
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    /**
     * Benchmarks a renderer.
     *
     * @param renderer   renderer class name
     * @param iterations number of iterations (default 100)
     * @param warmup     warmup iterations (default 20)
     * @return benchmark results with latency percentiles
     */
    @GetMapping("/{renderer}")
    public BenchmarkResult benchmark(
            @PathVariable String renderer,
            @RequestParam(defaultValue = "100") int iterations,
            @RequestParam(defaultValue = "20") int warmup
    ) {
        return benchmarkService.benchmark(renderer, iterations, warmup);
    }
}
