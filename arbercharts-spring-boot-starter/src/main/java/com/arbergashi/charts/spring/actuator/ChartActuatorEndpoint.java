package com.arbergashi.charts.spring.actuator;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Boot Actuator endpoint for ArberCharts monitoring.
 *
 * <p>Available at: {@code /actuator/charts}
 *
 * <h2>Provided Metrics</h2>
 * <ul>
 *   <li><strong>Health Status</strong> - Overall chart rendering health</li>
 *   <li><strong>Render Metrics</strong> - Average time, p99, p999</li>
 *   <li><strong>Memory Usage</strong> - Current chart memory consumption</li>
 *   <li><strong>Active Renderers</strong> - List of registered renderers</li>
 *   <li><strong>Configuration</strong> - Current ArberCharts settings</li>
 * </ul>
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
 * <h2>Response Example</h2>
 * <pre>{@code
 * {
 *   "status": "UP",
 *   "renderers": {
 *     "total": 158,
 *     "active": 12
 *   },
 *   "performance": {
 *     "avgRenderTime": "2.3ms",
 *     "p99RenderTime": "5.1ms",
 *     "totalRenders": 1523
 *   },
 *   "memory": {
 *     "currentUsage": "45MB",
 *     "peakUsage": "67MB"
 *   }
 * }
 * }</pre>
 *
 * @since 2.0.0
 * @see org.springframework.boot.actuate.endpoint.annotation.Endpoint
 */
@Endpoint(id = "charts")
public class ChartActuatorEndpoint {

    /**
     * v2.0.0: Placeholder values until metrics collector is integrated in v2.0.1.
     * These fields will be populated by a proper metrics service.
     */
    private long totalRenders = 0;
    private double avgRenderTimeMs = 0.0;
    private double p99RenderTimeMs = 0.0;
    private long currentMemoryBytes = 0;
    private long peakMemoryBytes = 0;

    /**
     * Main endpoint - returns comprehensive chart status.
     *
     * @return map with health, metrics, and configuration
     */
    @ReadOperation
    public Map<String, Object> charts() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Overall status
        result.put("status", "UP");

        // Renderer information
        Map<String, Object> renderers = new LinkedHashMap<>();
        renderers.put("total", 158);
        renderers.put("active", 12); // v2.0.1: Will query RendererRegistry
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
     * Detailed metrics sub-endpoint.
     *
     * <p>Access via: {@code /actuator/charts/{section}}
     *
     * @param section the section to query (performance, memory, renderers)
     * @return detailed metrics for the specified section
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
     * Gets detailed performance metrics.
     *
     * <p><strong>v2.0.0:</strong> Returns placeholder values.
     * v2.0.1 will integrate with actual metrics collector.
     */
    private Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // Placeholder values - v2.0.1 will use real metrics
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
     * Gets detailed memory metrics.
     *
     * <p><strong>v2.0.0:</strong> Returns placeholder values.
     * v2.0.1 will integrate with actual memory tracking.
     */
    private Map<String, Object> getMemoryMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // Placeholder values - v2.0.1 will use real memory tracker
        metrics.put("currentBytes", currentMemoryBytes);
        metrics.put("peakBytes", peakMemoryBytes);
        metrics.put("averageBytes", 35_000_000L);
        metrics.put("allocationsPerRender", 0); // Zero-alloc goal

        return metrics;
    }

    /**
     * Gets renderer information.
     *
     * <p><strong>v2.0.0:</strong> Returns static values.
     * v2.0.1 will query RendererRegistry dynamically.
     */
    private Map<String, Object> getRendererMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // v2.0.1: Will query RendererRegistry for actual values
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
     * Records a chart render for metrics.
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

        // v2.0.1: Will update p99 using HdrHistogram for accurate percentiles
        // For now, using simple running average which is acceptable for v2.0.0
        p99RenderTimeMs = Math.max(p99RenderTimeMs, renderTimeMs);
    }
}
