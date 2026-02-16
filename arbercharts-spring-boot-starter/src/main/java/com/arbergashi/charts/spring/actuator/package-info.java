/**
 * Spring Boot Actuator integration for ArberCharts monitoring.
 *
 * <p>This package provides production-ready monitoring endpoints for chart
 * rendering metrics, health status, and performance analytics.
 *
 * <h2>Available Endpoints</h2>
 * <ul>
 *   <li><strong>/actuator/charts:</strong> Overall chart status and metrics</li>
 *   <li><strong>/actuator/charts/performance:</strong> Detailed render metrics</li>
 *   <li><strong>/actuator/charts/memory:</strong> Memory usage statistics</li>
 *   <li><strong>/actuator/charts/renderers:</strong> Active renderer information</li>
 * </ul>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.arbergashi.charts.spring.actuator.ChartActuatorEndpoint} -
 *       Main monitoring endpoint</li>
 * </ul>
 *
 * <h2>Provided Metrics</h2>
 * <ul>
 *   <li><strong>Health Status:</strong> UP/DOWN/DEGRADED</li>
 *   <li><strong>Render Times:</strong> avg, p50, p95, p99, p999</li>
 *   <li><strong>Memory Usage:</strong> current, peak, average</li>
 *   <li><strong>Renderer Stats:</strong> total, active, failed</li>
 *   <li><strong>Zero-GC Stats:</strong> cache hit rates</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>Enable in application.yml:
 * <pre>
 * management:
 *   endpoints:
 *     web:
 *       exposure:
 *         include: health,info,charts
 *   endpoint:
 *     charts:
 *       enabled: true
 * </pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Access main endpoint
 * curl http://localhost:8080/actuator/charts
 *
 * // Response:
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
 * <h2>Micrometer Integration</h2>
 * <p>Metrics are automatically exported to Micrometer registries:
 * <pre>{@code
 * // Prometheus metrics
 * arbercharts_render_time_seconds{quantile="0.99"} 0.0051
 * arbercharts_render_total 1523
 * arbercharts_memory_bytes{type="current"} 47185920
 * }</pre>
 *
 * <h2>Alerting</h2>
 * <p>Recommended alerts:
 * <ul>
 *   <li><strong>High p99 latency:</strong> Alert if > 10ms</li>
 *   <li><strong>Memory growth:</strong> Alert if > 1GB/hour</li>
 *   <li><strong>Failed renders:</strong> Alert if > 1% failure rate</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <p>This package requires:
 * <ul>
 *   <li>spring-boot-actuator</li>
 *   <li>micrometer-core</li>
 * </ul>
 *
 * @see com.arbergashi.charts.spring.actuator.ChartActuatorEndpoint
 * @since 2.0.0
 */
package com.arbergashi.charts.spring.actuator;

