package com.arbergashi.charts.spring.actuator;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Boot Actuator endpoint exposing manually recorded render statistics.
 *
 * <p>Available at: {@code /actuator/charts}
 *
 * <p>The endpoint is not connected to a render metrics collector. Calls to
 * {@link #recordRender(double, long)} update the render count, mean duration,
 * maximum duration, and supplied memory values. The field named
 * {@code p99RenderTime} contains the maximum recorded duration, not a percentile.
 *
 * <p>The status and renderer counts are constants. Other percentile, allocation,
 * failure and concurrency fields are placeholders. These responses do not
 * establish rendering health or describe live renderer registrations.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Enable in application.yml:
 * management:
 *   endpoints:
 *     web:
 *       exposure:
 *         include: health,info,charts
 *
 * // Access endpoint:
 * curl http://localhost:8080/actuator/charts
 * }</pre>
 *
 * @since 2.0.0
 * @see org.springframework.boot.actuate.endpoint.annotation.Endpoint
 */
@Endpoint(id = "charts")
public class ChartActuatorEndpoint {

    /**
     * Statistics updated only by explicit calls to {@link #recordRender(double, long)}.
     * The p99-named field stores the maximum duration.
     */
    private long totalRenders = 0;
    private double avgRenderTimeMs = 0.0;
    private double p99RenderTimeMs = 0.0;
    private long currentMemoryBytes = 0;
    private long peakMemoryBytes = 0;

    /**
     * Returns recorded render statistics alongside fixed status and renderer counts.
     *
     * @return map with fixed status and renderer counts, plus recorded statistics
     */
    @ReadOperation
    public Map<String, Object> charts() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Fixed status; no rendering health check is performed
        result.put("status", "UP");

        // Fixed renderer counts; the registry is not queried
        Map<String, Object> renderers = new LinkedHashMap<>();
        renderers.put("total", 158);
        renderers.put("active", 12);
        result.put("renderers", renderers);

        // Performance metrics
        Map<String, Object> performance = new LinkedHashMap<>();
        performance.put("avgRenderTime", String.format("%.1fms", avgRenderTimeMs));
        performance.put("p99RenderTime", String.format("%.1fms", p99RenderTimeMs));
        performance.put("totalRenders", totalRenders);
        result.put("performance", performance);

        // Memory metrics
        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("currentUsage", formatBytes(currentMemoryBytes));
        memory.put("peakUsage", formatBytes(peakMemoryBytes));
        result.put("memory", memory);

        return result;
    }

    /**
     * Returns a section of recorded statistics and placeholder values.
     *
     * <p>Access via: {@code /actuator/charts/{section}}
     *
     * @param section the section to query (performance, memory, renderers)
     * @return the requested section, or an error map for an unknown section
     */
    @ReadOperation
    public Map<String, Object> chartsBySection(@Selector String section) {
        return switch (section) {
            case "performance" -> getPerformanceMetrics();
            case "memory" -> getMemoryMetrics();
            case "renderers" -> getRendererMetrics();
            default -> {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Unknown section: " + section);
                error.put("available", new String[]{"performance", "memory", "renderers"});
                yield error;
            }
        };
    }

    /**
     * Gets recorded render statistics and fixed performance placeholders.
     *
     * <p>The p99-named value is the recorded maximum. The other percentile,
     * failure and concurrency values are constants.
     */
    private Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // Recorded values are mixed with fixed placeholders
        metrics.put("avgRenderTime", avgRenderTimeMs);
        metrics.put("p50RenderTime", 1.5);
        metrics.put("p95RenderTime", 3.2);
        metrics.put("p99RenderTime", p99RenderTimeMs);
        metrics.put("p999RenderTime", 8.7);
        metrics.put("totalRenders", totalRenders);
        metrics.put("failedRenders", 0);
        metrics.put("concurrentRenders", 0);

        return metrics;
    }

    /**
     * Gets supplied memory values and fixed memory placeholders.
     *
     * <p>Current and peak bytes reflect explicit recordRender calls. Average
     * bytes and allocations per render are constants, not measurements.
     */
    private Map<String, Object> getMemoryMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // Memory usage is caller-supplied; average and allocation values are fixed
        metrics.put("currentBytes", currentMemoryBytes);
        metrics.put("peakBytes", peakMemoryBytes);
        metrics.put("averageBytes", 35_000_000L);
        metrics.put("allocationsPerRender", 0); // Placeholder, not an allocation measurement

        return metrics;
    }

    /**
     * Gets fixed renderer counts and category placeholders.
     *
     * <p>The renderer registry is not queried.
     */
    private Map<String, Object> getRendererMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // Fixed values, independent of the renderer registry
        metrics.put("totalRenderers", 158);
        metrics.put("activeRenderers", 12);
        metrics.put("categories", Map.of(
            "Standard", 17,
            "Financial", 29,
            "Statistical", 17,
            "Medical", 17,
            "Analysis", 19,
            "Specialized", 20,
            "Circular", 13,
            "Geo", 10,
            "Other", 16
        ));

        return metrics;
    }

    /**
     * Formats bytes to human-readable string.
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1fKB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1fGB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Records a caller-supplied duration and memory value.
     *
     * <p>Updates the count, mean duration, maximum duration, and current/peak
     * memory values. No percentile distribution is maintained.
     *
     * @param renderTimeMs render time in milliseconds
     * @param memoryBytes memory used in bytes
     */
    public void recordRender(double renderTimeMs, long memoryBytes) {
        totalRenders++;

        // Update running average
        avgRenderTimeMs = (avgRenderTimeMs * (totalRenders - 1) + renderTimeMs) / totalRenders;

        // Update peak memory
        if (memoryBytes > peakMemoryBytes) {
            peakMemoryBytes = memoryBytes;
        }

        currentMemoryBytes = memoryBytes;

        // Retains the maximum duration despite the historical p99 field name
        p99RenderTimeMs = Math.max(p99RenderTimeMs, renderTimeMs);
    }
}
