package com.arbergashi.charts.visualverifier.controller;

import com.arbergashi.charts.visualverifier.service.RendererCatalogService;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health and status endpoints for monitoring and diagnostics.
 *
 * <p>Provides comprehensive health information including:
 * <ul>
 *   <li>Application status and version</li>
 *   <li>JVM memory statistics</li>
 *   <li>Renderer catalog availability</li>
 *   <li>Vector API status</li>
 *   <li>Uptime information</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 2.0.0
 * @since 2026-01-01
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private static final Instant START_TIME = Instant.now();
    private static final String VERSION = "2.0.0";

    private final RendererCatalogService catalogService;

    public HealthController(RendererCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Returns comprehensive application health status.
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("application", "ArberCharts Visual Verifier");
        result.put("version", VERSION);
        result.put("timestamp", Instant.now().toString());

        // JVM Info
        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("version", System.getProperty("java.version"));
        jvm.put("vendor", System.getProperty("java.vendor"));
        jvm.put("runtime", System.getProperty("java.runtime.name"));
        result.put("jvm", jvm);

        // Memory
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        Map<String, Object> memory = new LinkedHashMap<>();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        memory.put("heapUsedMb", heapUsed / (1024 * 1024));
        memory.put("heapMaxMb", heapMax / (1024 * 1024));
        memory.put("heapUsagePercent", Math.round(100.0 * heapUsed / heapMax));
        result.put("memory", memory);

        // Uptime
        Duration uptime = Duration.between(START_TIME, Instant.now());
        result.put("uptimeSeconds", uptime.getSeconds());
        result.put("uptimeFormatted", formatDuration(uptime));

        // Capabilities
        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("vectorApi", isVectorApiAvailable());
        capabilities.put("rendererCount", catalogService.getTotalCount());
        capabilities.put("categories", catalogService.getCategoryStats().size());
        result.put("capabilities", capabilities);

        return result;
    }

    /**
     * Returns a simple liveness probe (for Kubernetes).
     */
    @GetMapping("/health/live")
    public Map<String, String> liveness() {
        return Map.of("status", "UP");
    }

    /**
     * Returns a readiness probe (for Kubernetes).
     */
    @GetMapping("/health/ready")
    public Map<String, Object> readiness() {
        boolean catalogReady = catalogService.getTotalCount() > 0;
        String status = catalogReady ? "UP" : "DOWN";

        return Map.of(
                "status", status,
                "checks", Map.of(
                        "catalog", catalogReady ? "OK" : "EMPTY",
                        "vectorApi", isVectorApiAvailable() ? "AVAILABLE" : "UNAVAILABLE"
                )
        );
    }

    /**
     * Returns detailed system information.
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("application", Map.of(
                "name", "ArberCharts Visual Verifier",
                "version", VERSION,
                "description", "Server-side visual testing platform for ArberCharts renderers"
        ));

        result.put("build", Map.of(
                "java", System.getProperty("java.version"),
                "os", System.getProperty("os.name") + " " + System.getProperty("os.version"),
                "arch", System.getProperty("os.arch")
        ));

        result.put("runtime", Map.of(
                "processors", Runtime.getRuntime().availableProcessors(),
                "maxMemoryMb", Runtime.getRuntime().maxMemory() / (1024 * 1024),
                "vectorApi", isVectorApiAvailable()
        ));

        return result;
    }

    private boolean isVectorApiAvailable() {
        try {
            Class.forName("jdk.incubator.vector.FloatVector");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
