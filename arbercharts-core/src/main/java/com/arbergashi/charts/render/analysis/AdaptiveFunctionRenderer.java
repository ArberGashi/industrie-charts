package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ChartUtils;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.DoubleUnaryOperator;
/**
 * Adaptive function renderer (JDK 25 standard).
 * Draws mathematical functions f(x) with adaptive sampling.
 * Uses virtual threads for parallel background computation of points.
 * <p>
 * Reuses sampling and display buffers. Nonfinite samples separate curve paths;
 * cancelled or superseded computations never replace the current frame.
 *
 * @author Arber Gashi
 * @since 2026-01-01
 *
 */
public class AdaptiveFunctionRenderer extends BaseRenderer implements AutoCloseable {

    private final DoubleUnaryOperator function;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    // Buffers are exclusively owned by a worker or protected by this renderer's monitor.
    private final Deque<PointBuffer> bufferPool = new ArrayDeque<>();
    private PointBuffer renderBuffer = new PointBuffer();
    private CompletableFuture<Void> calculationTask;
    private PlotContext lastContext;
    private volatile Runnable repaintCallback;
    private volatile long generation;
    private volatile boolean closed;
    private final double[] pixel = new double[2];
    public AdaptiveFunctionRenderer(DoubleUnaryOperator function) {
        super("function");
        this.function = java.util.Objects.requireNonNull(function, "function");
        // Prime the pool (2 buffers for double-buffering + reserve).
        bufferPool.push(new PointBuffer());
        bufferPool.push(new PointBuffer());
        bufferPool.push(new PointBuffer());
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        // Recalculate on context changes (zoom/resize/scroll).
        if (isContextChanged(context)) {
            startAsyncCalculation(context);
        }

        // Cold start: no frame has ever been produced. Wait briefly for the
        // running calculation so the first frame (and single-frame headless
        // rendering) shows the curve. Later recalculations keep the previous
        // buffer on screen and stay fully asynchronous.
        if (!hasRenderPoints()) {
            awaitInitialCalculation();
        }

        // If no points are available yet, skip drawing.
        if (!hasRenderPoints()) {
            return;
        }

        // Theme-aware color.
        ArberColor curveColor = getResolvedTheme(context).getAccentColor();
        if (isMultiColor()) {
            curveColor = themeSeries(context, 0);
        }
        if (curveColor == null) curveColor = seriesOrBase(model, context, 0);

        canvas.setColor(curveColor);
        canvas.setStroke(ChartScale.scale(1.0f));

        float[] xs;
        float[] ys;
        int count;
        synchronized (this) {
            count = renderBuffer.count;
            xs = RendererAllocationCache.getFloatArray(this, "func.line.x", count);
            ys = RendererAllocationCache.getFloatArray(this, "func.line.y", count);
            for (int i = 0; i < count; i++) {
                if (!Double.isFinite(renderBuffer.x[i]) || !Double.isFinite(renderBuffer.y[i])) {
                    xs[i] = Float.NaN;
                    ys[i] = Float.NaN;
                    continue;
                }
                context.mapToPixel(renderBuffer.x[i], renderBuffer.y[i], pixel);
                xs[i] = (float) pixel[0];
                ys[i] = (float) pixel[1];
            }
        }
        int used = 0;
        for (int i = 0; i < count; i++) {
            if (!Float.isFinite(xs[i]) || !Float.isFinite(ys[i])) {
                if (used > 1) canvas.drawPolyline(xs, ys, used);
                used = 0;
                continue;
            }
            xs[used] = xs[i];
            ys[used++] = ys[i];
        }
        if (used > 1) canvas.drawPolyline(xs, ys, used);
    }

    /**
     * Sets a callback invoked when new data becomes available.
     *
     * @param callback Runnable for repaint (e.g., chart::repaint)
     */
    public AdaptiveFunctionRenderer setRepaintCallback(Runnable callback) {
        this.repaintCallback = callback;
        return this;
    }

    /** Cancels pending sampling and releases this renderer's virtual-thread executor. */
    @Override
    public synchronized void close() {
        closed = true;
        generation++;
        if (calculationTask != null) calculationTask.cancel(true);
        executor.shutdownNow();
    }

    private synchronized boolean hasRenderPoints() {
        return renderBuffer.count >= 2;
    }

    private void awaitInitialCalculation() {
        CompletableFuture<Void> task;
        synchronized (this) {
            task = calculationTask;
        }
        if (task == null) {
            return;
        }
        try {
            task.get(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
            // Timeout, cancellation, or calculation failure: draw nothing this frame.
        }
    }

    private boolean isContextChanged(PlotContext context) {
        if (lastContext == null) return true;
        return lastContext.getMinX() != context.getMinX() ||
                lastContext.getMaxX() != context.getMaxX() ||
                lastContext.getMinY() != context.getMinY() ||
                lastContext.getMaxY() != context.getMaxY() ||
                lastContext.isInvertedX() != context.isInvertedX() ||
                lastContext.isInvertedY() != context.isInvertedY() ||
                lastContext.getScaleModeX() != context.getScaleModeX() ||
                lastContext.getScaleModeY() != context.getScaleModeY() ||
                lastContext.isLogarithmicY() != context.isLogarithmicY() ||
                !lastContext.getPlotBounds().equals(context.getPlotBounds());
    }

    /**
     * Starts asynchronous computation of function samples in virtual threads.
     * Uses double-buffering to avoid blocking the EDT.
     */
    private synchronized void startAsyncCalculation(PlotContext context) {
        if (closed) return;
        if (calculationTask != null && !calculationTask.isDone()) {
            calculationTask.cancel(true);
        }

        PlotContext snapshot = new DefaultPlotContext(context.getPlotBounds(),
                context.getMinX(), context.getMaxX(), context.getMinY(), context.getMaxY(),
                context.isLogarithmicY(), context.isInvertedX(), context.isInvertedY(),
                context.getScaleModeX(), context.getScaleModeY(), context.getTheme(),
                context.getRenderHints(), context.getGapModel(), context.getAnimationProfile());
        lastContext = snapshot;
        long requestedGeneration = ++generation;
        calculationTask = CompletableFuture.runAsync(() -> {
            // Get a buffer from the pool or create a new one (avoids races on shared state).
            PointBuffer workingBuffer;
            synchronized (bufferPool) {
                workingBuffer = bufferPool.isEmpty() ? new PointBuffer() : bufferPool.pop();
            }
            workingBuffer.clear();
            boolean published = false;
            try {
                double minX = snapshot.getMinX();
                double maxX = snapshot.getMaxX();
                double width = snapshot.getPlotBounds().getWidth();
                if (!Double.isFinite(minX) || !Double.isFinite(maxX) || !(maxX > minX)
                        || !Double.isFinite(width) || width <= 0) return;
                int baseSamples = (int) Math.clamp(width, 100, 16_384);
                double step = (maxX - minX) / baseSamples;
                double thresholdPx = ChartScale.scale(0.25f);

                double lastX = minX;
                double lastY = function.applyAsDouble(lastX);
                workingBuffer.add(lastX, lastY);

                for (int i = 1; i <= baseSamples; i++) {
                    if (Thread.currentThread().isInterrupted() || requestedGeneration != generation) return;

                    double x = minX + i * step;
                    double y = function.applyAsDouble(x);

                    if (Double.isFinite(y)) {
                        if (workingBuffer.count > 0 && Double.isFinite(workingBuffer.y[workingBuffer.count - 1])) {
                            sampleRecursive(workingBuffer, workingBuffer.x[workingBuffer.count - 1],
                                    workingBuffer.y[workingBuffer.count - 1],
                                    x, y, snapshot, thresholdPx, 0, requestedGeneration);
                        }
                        workingBuffer.add(x, y);
                    } else {
                        workingBuffer.add(x, Double.NaN);
                    }
                }

                synchronized (this) {
                    if (requestedGeneration != generation || closed) return;
                    PointBuffer temp = renderBuffer;
                    renderBuffer = workingBuffer;
                    published = true;
                    // Return old buffer to the pool.
                    synchronized (bufferPool) {
                        temp.clear();
                        bufferPool.push(temp);
                    }
                }
                Runnable callback = repaintCallback;
                if (callback != null) callback.run();
            } finally {
                if (!published) {
                    synchronized (bufferPool) {
                        workingBuffer.clear();
                        bufferPool.push(workingBuffer);
                    }
                }
            }
        }, executor);
    }

    /**
     * Recursive adaptive sampling to increase curve detail.
     * Runs on a background thread.
     */
    private void sampleRecursive(PointBuffer buffer, double x1, double y1, double x2, double y2, PlotContext context,
                                 double thresholdPx, int depth, long requestedGeneration) {
        if (depth > 12 || Thread.currentThread().isInterrupted() || requestedGeneration != generation) return;

        double midX = Math.fma(x1 + x2, 0.5, 0);
        double actualY = function.applyAsDouble(midX);

        if (!Double.isFinite(actualY)) {
            buffer.add(midX, Double.NaN);
            return;
        }

        double pix1x = ChartUtils.transformX(x1, context);
        double pix1y = ChartUtils.transformY(y1, context);
        double pix2x = ChartUtils.transformX(x2, context);
        double pix2y = ChartUtils.transformY(y2, context);
        double pixMidX = ChartUtils.transformX(midX, context);
        double pixMidY = ChartUtils.transformY(actualY, context);

        double dist = pointToLineDistance(pixMidX, pixMidY, pix1x, pix1y, pix2x, pix2y);

        if (dist > thresholdPx) {
            sampleRecursive(buffer, x1, y1, midX, actualY, context, thresholdPx, depth + 1, requestedGeneration);
            buffer.add(midX, actualY);
            sampleRecursive(buffer, midX, actualY, x2, y2, context, thresholdPx, depth + 1, requestedGeneration);
        }
    }

    /**
     * Computes the distance from a point to a line segment.
     * Optimized for performance and precision.
     */
    private double pointToLineDistance(double px, double py, double l1x, double l1y, double l2x, double l2y) {
        double dx = l2x - l1x;
        double dy = l2y - l1y;
        if (dx == 0 && dy == 0) {
            double ddx = px - l1x;
            double ddy = py - l1y;
            return Math.sqrt(ddx * ddx + ddy * ddy);
        }

        return Math.abs(dy * px - dx * py + l2x * l1y - l2y * l1x)
                / Math.sqrt(Math.fma(dx, dx, dy * dy));
    }

    // Sampling storage grows only when a more detailed curve needs additional points.
    private static class PointBuffer {
        double[] x = new double[1024];
        double[] y = new double[1024];
        int count = 0;

        void ensureCapacity(int capacity) {
            if (x.length < capacity) {
                int newCap = Math.max(capacity, x.length * 2);
                x = java.util.Arrays.copyOf(x, newCap);
                y = java.util.Arrays.copyOf(y, newCap);
            }
        }

        void add(double px, double py) {
            ensureCapacity(count + 1);
            x[count] = px;
            y[count] = py;
            count++;
        }

        void clear() {
            count = 0;
        }
    }
}
