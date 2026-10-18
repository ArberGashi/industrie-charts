package com.arbergashi.charts.spring.actuator;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Spring Boot Actuator endpoint exposing manually recorded render statistics.
 *
 * <p>Available at {@code /actuator/charts} when the endpoint is exposed. Calls to
 * {@link #recordRender(double, long)} update the render count, mean duration,
 * maximum duration, and supplied memory values. The rendering service does not
 * call this endpoint automatically. Updates and response snapshots are atomic.
 *
 * <p>Since 2.5.0, responses contain only recorded statistics. The historical
 * {@code p99RenderTime} field is replaced by {@code maxRenderTime}, because no
 * percentile distribution is maintained. Fixed health, renderer, percentile,
 * allocation, failure and concurrency values have been removed. The
 * {@code renderers} section reports that measurements are unavailable.
 *
 * <p>{@code measurementSource} is {@code manual} after the first sample and
 * {@code unavailable} before it. Unrecorded durations and memory values are
 * {@code null}; the render count starts at zero. The root response formats
 * durations and byte sizes for display. Section responses provide durations
 * in milliseconds and memory in bytes as numbers.
 *
 * @since 2.0.0
 * @see org.springframework.boot.actuate.endpoint.annotation.Endpoint
 */
@Endpoint(id = "charts")
public class ChartActuatorEndpoint {

    private long totalRenders;
    private double avgRenderTimeMs;
    private double maxRenderTimeMs;
    private long currentMemoryBytes;
    private long peakMemoryBytes;

    /**
     * Returns a consistent snapshot of the manually recorded statistics.
     *
     * @return measurement source, formatted performance and memory statistics
     */
    @ReadOperation
    public Map<String, Object> charts() {
        Snapshot snapshot = snapshot();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("measurementSource", snapshot.measurementSource());

        Map<String, Object> performance = new LinkedHashMap<>();
        performance.put("avgRenderTime", snapshot.hasSamples()
                ? String.format(Locale.ROOT, "%.1fms", snapshot.avgRenderTimeMs()) : null);
        performance.put("maxRenderTime", snapshot.hasSamples()
                ? String.format(Locale.ROOT, "%.1fms", snapshot.maxRenderTimeMs()) : null);
        performance.put("totalRenders", snapshot.totalRenders());
        result.put("performance", performance);

        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("currentUsage", snapshot.hasSamples() ? formatBytes(snapshot.currentMemoryBytes()) : null);
        memory.put("peakUsage", snapshot.hasSamples() ? formatBytes(snapshot.peakMemoryBytes()) : null);
        result.put("memory", memory);
        return result;
    }

    /**
     * Returns one section of the recorded statistics.
     *
     * <p>Access via {@code /actuator/charts/{section}}. Renderer measurements are
     * unavailable because this endpoint does not observe the renderer registry.
     *
     * @param section the section to query (performance, memory, renderers)
     * @return the requested section, or an error map for an unknown section
     */
    @ReadOperation
    public Map<String, Object> chartsBySection(@Selector String section) {
        return switch (section) {
            case "performance" -> performanceMetrics(snapshot());
            case "memory" -> memoryMetrics(snapshot());
            case "renderers" -> Map.of("measurementSource", "unavailable");
            default -> {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Unknown section: " + section);
                error.put("available", new String[]{"performance", "memory", "renderers"});
                yield error;
            }
        };
    }

    private static Map<String, Object> performanceMetrics(Snapshot snapshot) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("measurementSource", snapshot.measurementSource());
        metrics.put("avgRenderTime", snapshot.hasSamples() ? snapshot.avgRenderTimeMs() : null);
        metrics.put("maxRenderTime", snapshot.hasSamples() ? snapshot.maxRenderTimeMs() : null);
        metrics.put("totalRenders", snapshot.totalRenders());
        return metrics;
    }

    private static Map<String, Object> memoryMetrics(Snapshot snapshot) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("measurementSource", snapshot.measurementSource());
        metrics.put("currentBytes", snapshot.hasSamples() ? snapshot.currentMemoryBytes() : null);
        metrics.put("peakBytes", snapshot.hasSamples() ? snapshot.peakMemoryBytes() : null);
        return metrics;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1fKB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1fMB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.ROOT, "%.1fGB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Records a caller-supplied duration and memory value atomically.
     *
     * <p>Updates the count, mean duration, maximum duration, and current/peak
     * memory values. No percentile distribution is maintained. Invalid samples
     * are rejected without changing any statistic.
     *
     * @param renderTimeMs finite, nonnegative render time in milliseconds
     * @param memoryBytes nonnegative memory used in bytes
     * @throws IllegalArgumentException if either argument is negative or the duration is not finite
     */
    public synchronized void recordRender(double renderTimeMs, long memoryBytes) {
        if (!Double.isFinite(renderTimeMs) || renderTimeMs < 0 || memoryBytes < 0) {
            throw new IllegalArgumentException("Render duration must be finite and nonnegative; memory must be nonnegative");
        }
        totalRenders++;
        avgRenderTimeMs += (renderTimeMs - avgRenderTimeMs) / totalRenders;
        maxRenderTimeMs = Math.max(maxRenderTimeMs, renderTimeMs);
        currentMemoryBytes = memoryBytes;
        peakMemoryBytes = Math.max(peakMemoryBytes, memoryBytes);
    }

    private synchronized Snapshot snapshot() {
        return new Snapshot(totalRenders, avgRenderTimeMs, maxRenderTimeMs, currentMemoryBytes, peakMemoryBytes);
    }

    private record Snapshot(long totalRenders, double avgRenderTimeMs, double maxRenderTimeMs,
                            long currentMemoryBytes, long peakMemoryBytes) {
        private boolean hasSamples() {
            return totalRenders > 0;
        }

        private String measurementSource() {
            return hasSamples() ? "manual" : "unavailable";
        }
    }
}
