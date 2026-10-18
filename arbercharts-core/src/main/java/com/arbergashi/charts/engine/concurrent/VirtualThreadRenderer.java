package com.arbergashi.charts.engine.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Executor wrapper with reserved asynchronous rendering methods.
 *
 * <p>The factory methods create an executor for application-owned tasks.
 * {@link #renderPngAsync(Object)} and {@link #renderSvgAsync(Object)} are not
 * implemented: their submitted tasks always fail with
 * {@link UnsupportedOperationException}.
 *
 * <p>For PNG rendering, use {@code ServerRenderService.renderToPng} from
 * {@code arbercharts-server-bridge} and schedule calls with an executor owned by
 * the application. Each task must respect the model and renderer ownership
 * requirements. Swing applications can use {@code ChartExportService.exportSvg}
 * with the optional JFreeSVG backend for SVG output.
 *
 * <p>Call {@link #shutdown()} when the executor is no longer needed. The library
 * requires Java 25; selecting a fallback executor does not change that baseline.
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
     * <p>Throws {@link UnsupportedOperationException} if executor creation fails.
     *
     * @return new VirtualThreadRenderer instance
     * @throws UnsupportedOperationException if the virtual-thread executor cannot be created
     */
    public static VirtualThreadRenderer create() {
        try {
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            return new VirtualThreadRenderer(executor, true);
        } catch (Exception e) {
            throw new UnsupportedOperationException(
                "Could not create the virtual-thread executor. " +
                "Use createWithFallback() to attempt a work-stealing executor instead.", e
            );
        }
    }

    /**
     * Creates a VirtualThreadRenderer with automatic fallback.
     *
     * <p>Attempts to create a virtual-thread executor, falling back to a
     * work-stealing executor if creation fails. Java 25 is still required.
     *
     * @return new VirtualThreadRenderer instance
     */
    public static VirtualThreadRenderer createWithFallback() {
        try {
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            return new VirtualThreadRenderer(executor, true);
        } catch (Exception e) {
            // Fall back to a platform-thread work-stealing executor
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
     * Submits an unsupported PNG-rendering operation.
     *
     * <p>The submitted task always throws {@link UnsupportedOperationException}.
     * No rendering is performed. Use {@code ServerRenderService.renderToPng}
     * from {@code arbercharts-server-bridge} for PNG output.
     *
     * @param model requested chart model; not used
     * @return a future that completes exceptionally with {@code UnsupportedOperationException}
     * @throws java.util.concurrent.RejectedExecutionException if the executor rejects the task
     */
    public CompletableFuture<byte[]> renderPngAsync(Object model) {
        return CompletableFuture.supplyAsync(() -> {
            throw new UnsupportedOperationException(
                "PNG rendering through VirtualThreadRenderer is not implemented. " +
                "Schedule ServerRenderService.renderToPng() with an application-owned executor."
            );
        }, executor);
    }

    /**
     * Submits an unsupported SVG-rendering operation.
     *
     * <p>The submitted task always throws {@link UnsupportedOperationException}.
     * No rendering is performed. Swing applications can use
     * {@code ChartExportService.exportSvg} with the optional JFreeSVG backend.
     *
     * @param model requested chart model; not used
     * @return a future that completes exceptionally with {@code UnsupportedOperationException}
     * @throws java.util.concurrent.RejectedExecutionException if the executor rejects the task
     */
    public CompletableFuture<String> renderSvgAsync(Object model) {
        return CompletableFuture.supplyAsync(() -> {
            throw new UnsupportedOperationException(
                "SVG rendering through VirtualThreadRenderer is not implemented. " +
                "Use ChartExportService.exportSvg() with a Swing panel and the optional JFreeSVG backend."
            );
        }, executor);
    }

    /**
     * Returns the executor-selection flag assigned by the factory.
     *
     * <p>The custom-factory path sets this flag to true without inspecting the
     * threads produced by that factory.
     *
     * @return false for the work-stealing fallback, otherwise true
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
