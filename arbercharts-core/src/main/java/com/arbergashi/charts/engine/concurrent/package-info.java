/**
 * Executor infrastructure and reserved asynchronous rendering methods.
 *
 * <p>{@link com.arbergashi.charts.engine.concurrent.VirtualThreadRenderer}
 * creates virtual-thread, custom-thread or fallback work-stealing executors.
 * Applications can submit their own tasks through its executor accessor and
 * must call {@code shutdown()} when finished.
 *
 * <p>The PNG and SVG convenience operations are not implemented. Their tasks
 * complete exceptionally with {@link java.lang.UnsupportedOperationException};
 * they do not generate images.
 *
 * <p>For server PNG rendering, use {@code ServerRenderService.renderToPng} from
 * {@code arbercharts-server-bridge}, optionally scheduled on an application-owned
 * executor. Coordinate model updates and avoid sharing mutable renderer state
 * across simultaneous requests. Swing applications can use
 * {@code ChartExportService.exportSvg} with the optional JFreeSVG backend.
 *
 * <p>The library requires Java 25. The executor fallback does not provide
 * compatibility with older Java runtimes or establish rendering throughput,
 * memory or latency guarantees.
 *
 * @see com.arbergashi.charts.engine.concurrent.VirtualThreadRenderer
 * @see java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor()
 * @since 2.0.0
 */
package com.arbergashi.charts.engine.concurrent;

