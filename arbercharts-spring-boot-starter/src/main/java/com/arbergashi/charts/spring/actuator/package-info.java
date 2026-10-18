/**
 * Spring Boot Actuator integration for manually recorded chart statistics.
 *
 * <p>{@link com.arbergashi.charts.spring.actuator.ChartActuatorEndpoint} exposes
 * a render count, mean duration, maximum duration and caller-supplied memory
 * values. These change only when its {@code recordRender} method is called;
 * the rendering services do not call it automatically.
 *
 * <p>The endpoint also returns fixed status, renderer counts and performance
 * placeholders. Its {@code p99RenderTime} field holds the maximum recorded
 * duration, not a calculated percentile. Do not interpret these placeholders
 * as rendering health, live registry contents or measured percentiles.
 *
 * <h2>Available Endpoints</h2>
 * <ul>
 *   <li><strong>/actuator/charts:</strong> Recorded statistics and fixed status/counts</li>
 *   <li><strong>/actuator/charts/performance:</strong> Render statistics and placeholders</li>
 *   <li><strong>/actuator/charts/memory:</strong> Supplied memory values and placeholders</li>
 *   <li><strong>/actuator/charts/renderers:</strong> Fixed renderer counts</li>
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

