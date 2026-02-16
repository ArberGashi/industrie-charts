/**
 * Concurrent rendering support using Virtual Threads (Project Loom).
 *
 * <p>This package provides infrastructure for massively concurrent chart
 * rendering without traditional thread pool limitations.
 *
 * <h2>Virtual Threads (Java 21+)</h2>
 * <p>Virtual Threads enable 100+ concurrent chart renders without exhausting
 * platform threads. Each render operation runs in its own lightweight virtual
 * thread with ~1KB memory overhead (vs ~1MB for platform threads).
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.arbergashi.charts.engine.concurrent.VirtualThreadRenderer} -
 *       Async rendering with CompletableFuture API</li>
 * </ul>
 *
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li><strong>Throughput:</strong> 100+ concurrent renders without pool exhaustion</li>
 *   <li><strong>Memory:</strong> ~1KB per virtual thread vs ~1MB for platform threads</li>
 *   <li><strong>Latency:</strong> No thread pool contention delays</li>
 *   <li><strong>Scheduling:</strong> Automatic work-stealing via ForkJoinPool</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * VirtualThreadRenderer renderer = VirtualThreadRenderer.create();
 *
 * // Render multiple charts concurrently
 * List<CompletableFuture<byte[]>> futures = models.stream()
 *     .map(renderer::renderPngAsync)
 *     .toList();
 *
 * // Wait for all to complete
 * CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
 *     .join();
 * }</pre>
 *
 * <h2>Spring Boot Integration</h2>
 * <pre>{@code
 * @Configuration
 * public class ChartConfig {
 *     @Bean
 *     public VirtualThreadRenderer virtualThreadRenderer() {
 *         return VirtualThreadRenderer.create();
 *     }
 * }
 *
 * @RestController
 * class ChartController {
 *     @Autowired VirtualThreadRenderer renderer;
 *
 *     @GetMapping("/chart.png")
 *     public CompletableFuture<byte[]> renderChart() {
 *         return renderer.renderPngAsync(model);
 *     }
 * }
 * }</pre>
 *
 * <h2>Compatibility</h2>
 * <ul>
 *   <li><strong>Java 21+:</strong> Full Virtual Threads support</li>
 *   <li><strong>Java 17-20:</strong> Automatic fallback to ForkJoinPool</li>
 * </ul>
 *
 * @see com.arbergashi.charts.engine.concurrent.VirtualThreadRenderer
 * @see java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor()
 * @since 2.0.0
 */
package com.arbergashi.charts.engine.concurrent;

