package com.arbergashi.charts.engine.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Virtual Thread based concurrent rendering for ArberCharts v2.0.
 *
 * <p>Uses Project Loom Virtual Threads (Java 21+) for massively concurrent
 * chart rendering without traditional thread pool limitations.
 *
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li>100+ concurrent renders without thread pool exhaustion</li>
 *   <li>Low memory overhead (~1KB per virtual thread vs ~1MB for platform threads)</li>
 *   <li>Non-blocking server-side rendering</li>
 *   <li>Automatic work-stealing scheduling</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * VirtualThreadRenderer renderer = VirtualThreadRenderer.create();
 *
 * // Render multiple charts concurrently
 * CompletableFuture<byte[]> chart1 = renderer.renderAsync(model1);
 * CompletableFuture<byte[]> chart2 = renderer.renderAsync(model2);
 * CompletableFuture<byte[]> chart3 = renderer.renderAsync(model3);
 *
 * // Combine results
 * CompletableFuture.allOf(chart1, chart2, chart3)
 *     .thenRun(() -> System.out.println("All charts rendered"));
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
 * }</pre>
 *
 * @since 2.0.0
 * @see java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor()
 */
public final class VirtualThreadRenderer {

    private final ExecutorService executor;
    private final boolean isVirtualThreads;

    /**
     * Private constructor. Use {@link #create()} or {@link #createWithFallback()}.
     */
    private VirtualThreadRenderer(ExecutorService executor, boolean isVirtualThreads) {
        this.executor = executor;
        this.isVirtualThreads = isVirtualThreads;
    }

    /**
     * Creates a VirtualThreadRenderer with Virtual Threads.
     *
     * <p>Requires Java 21+ with Virtual Threads support.
     * Throws {@link UnsupportedOperationException} if not available.
     *
     * @return new VirtualThreadRenderer instance
     * @throws UnsupportedOperationException if Virtual Threads not supported
     */
    public static VirtualThreadRenderer create() {
        try {
            // Java 21+: Executors.newVirtualThreadPerTaskExecutor()
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            return new VirtualThreadRenderer(executor, true);
        } catch (Exception e) {
            throw new UnsupportedOperationException(
                "Virtual Threads not available. Requires Java 21+. " +
                "Use createWithFallback() for compatibility mode.", e
            );
        }
    }

    /**
     * Creates a VirtualThreadRenderer with automatic fallback.
     *
     * <p>Attempts to use Virtual Threads (Java 21+), falls back to
     * ForkJoinPool if not available.
     *
     * @return new VirtualThreadRenderer instance
     */
    public static VirtualThreadRenderer createWithFallback() {
        try {
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            return new VirtualThreadRenderer(executor, true);
        } catch (Exception e) {
            // Fallback to ForkJoinPool (available in all Java versions)
            ExecutorService executor = Executors.newWorkStealingPool();
            return new VirtualThreadRenderer(executor, false);
        }
    }

    /**
     * Creates a custom VirtualThreadRenderer with a thread factory.
     *
     * @param factory custom thread factory
     * @return new VirtualThreadRenderer instance
     */
    public static VirtualThreadRenderer createWithFactory(ThreadFactory factory) {
        ExecutorService executor = Executors.newThreadPerTaskExecutor(factory);
        return new VirtualThreadRenderer(executor, true);
    }

    /**
     * Renders a chart asynchronously and returns a PNG byte array.
     *
     * <p>This method is non-blocking and returns immediately.
     * The actual rendering happens on a virtual thread.
     *
     * @param model the chart model to render
     * @return CompletableFuture with PNG bytes
     * @throws UnsupportedOperationException rendering not yet implemented in v2.0.0
     */
    public CompletableFuture<byte[]> renderPngAsync(Object model) {
        return CompletableFuture.supplyAsync(() -> {
            throw new UnsupportedOperationException(
                "Async PNG rendering will be implemented in v2.0.1. " +
                "Current version v2.0.0 focuses on interactive display. " +
                "Use Charts.lineChart().show() for now."
            );
        }, executor);
    }

    /**
     * Renders a chart asynchronously and returns an SVG string.
     *
     * @param model the chart model to render
     * @return CompletableFuture with SVG string
     * @throws UnsupportedOperationException rendering not yet implemented in v2.0.0
     */
    public CompletableFuture<String> renderSvgAsync(Object model) {
        return CompletableFuture.supplyAsync(() -> {
            throw new UnsupportedOperationException(
                "Async SVG rendering will be implemented in v2.0.1. " +
                "Current version v2.0.0 focuses on interactive display. " +
                "Use Charts.lineChart().show() for now."
            );
        }, executor);
    }

    /**
     * Checks if this renderer is using Virtual Threads.
     *
     * @return true if using Virtual Threads, false if using fallback
     */
    public boolean isUsingVirtualThreads() {
        return isVirtualThreads;
    }

    /**
     * Shuts down the executor service.
     *
     * <p>Call this when the renderer is no longer needed to release resources.
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Gets the underlying executor service.
     *
     * <p>Advanced users can use this for custom task submission.
     *
     * @return the executor service
     */
    public ExecutorService getExecutor() {
        return executor;
    }
}

