package com.arbergashi.charts.visualverifier.dto;

/**
 * Benchmark result with performance metrics.
 *
 * @param renderer        renderer class name
 * @param iterations      number of iterations
 * @param avgTimeMs       average render time in milliseconds
 * @param p50Ms           50th percentile latency
 * @param p99Ms           99th percentile latency
 * @param p999Ms          99.9th percentile latency
 * @param memoryPeakBytes peak memory usage in bytes
 * @param throughput      renders per second
 * @since 2.0.0
 */
public record BenchmarkResult(
        String renderer,
        int iterations,
        double avgTimeMs,
        double p50Ms,
        double p99Ms,
        double p999Ms,
        long memoryPeakBytes,
        double throughput
) {
    /**
     * Builder for constructing BenchmarkResult.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String renderer;
        private int iterations;
        private double avgTimeMs;
        private double p50Ms;
        private double p99Ms;
        private double p999Ms;
        private long memoryPeakBytes;
        private double throughput;

        public Builder renderer(String renderer) {
            this.renderer = renderer;
            return this;
        }

        public Builder iterations(int iterations) {
            this.iterations = iterations;
            return this;
        }

        public Builder avgTimeMs(double avgTimeMs) {
            this.avgTimeMs = avgTimeMs;
            return this;
        }

        public Builder p50Ms(double p50Ms) {
            this.p50Ms = p50Ms;
            return this;
        }

        public Builder p99Ms(double p99Ms) {
            this.p99Ms = p99Ms;
            return this;
        }

        public Builder p999Ms(double p999Ms) {
            this.p999Ms = p999Ms;
            return this;
        }

        public Builder memoryPeakBytes(long memoryPeakBytes) {
            this.memoryPeakBytes = memoryPeakBytes;
            return this;
        }

        public Builder throughput(double throughput) {
            this.throughput = throughput;
            return this;
        }

        public BenchmarkResult build() {
            return new BenchmarkResult(
                    renderer, iterations, avgTimeMs, p50Ms, p99Ms, p999Ms,
                    memoryPeakBytes, throughput
            );
        }
    }
}

