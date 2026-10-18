/**
 * Spring Boot Actuator integration for manually recorded chart statistics.
 *
 * <p>{@link com.arbergashi.charts.spring.actuator.ChartActuatorEndpoint} exposes
 * a render count, mean duration, maximum duration and caller-supplied memory
 * values. These change only when its {@code recordRender} method is called;
 * the rendering services do not call it automatically.
 *
 * <p>Since 2.5.0, the JSON field {@code maxRenderTime} replaces the misleading
 * {@code p99RenderTime}. Fixed health, renderer counts, percentiles, allocations,
 * failures and concurrency values have been removed. Responses identify their
 * {@code measurementSource} as {@code manual} after a sample is recorded, or
 * {@code unavailable} before it. Unrecorded durations and memory values are
 * {@code null}; the render count starts at zero. Each response is an atomic
 * snapshot. Root responses retain formatted values, while performance and
 * memory sections return numeric milliseconds and bytes.
 *
 * <h2>Available Endpoints</h2>
 * <ul>
 *   <li><strong>/actuator/charts:</strong> Recorded statistics and measurement source</li>
 *   <li><strong>/actuator/charts/performance:</strong> Mean, maximum and count of recorded renders</li>
 *   <li><strong>/actuator/charts/memory:</strong> Supplied current and peak memory values</li>
 *   <li><strong>/actuator/charts/renderers:</strong> Measurement source {@code unavailable}</li>
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
 * </pre>
 *
 * <h2>Micrometer Integration</h2>
 * <p>{@link com.arbergashi.charts.bridge.server.ServerRenderService} registers
 * separate meters when it receives a Micrometer {@code MeterRegistry}:
 * <ul>
 *   <li>{@code arber.charts.render.time}</li>
 *   <li>{@code arber.charts.render.count}</li>
 *   <li>{@code arber.charts.pool.active}</li>
 *   <li>{@code arber.charts.pool.idle}</li>
 * </ul>
 * <p>Those meters are independent of this endpoint's stored values.
 *
 * <h2>Dependencies</h2>
 * <p>This package requires:
 * <ul>
 *   <li>spring-boot-actuator</li>
 * </ul>
 *
 * @see com.arbergashi.charts.spring.actuator.ChartActuatorEndpoint
 * @since 2.0.0
 */
package com.arbergashi.charts.spring.actuator;

