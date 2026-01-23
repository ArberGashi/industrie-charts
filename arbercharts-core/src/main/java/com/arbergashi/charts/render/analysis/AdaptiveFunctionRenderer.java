package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ChartUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
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
 * Optimized for zero-allocation rendering and FlatLaf integration.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class AdaptiveFunctionRenderer extends BaseRenderer {

    private final DoubleUnaryOperator function;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    // Buffer pool to preserve zero-allocation even with parallel tasks.
    private final Deque<PointBuffer> bufferPool = new ArrayDeque<>();
    private PointBuffer renderBuffer = new PointBuffer();
    private CompletableFuture<Void> calculationTask;
    private PlotContext lastContext;
    private Runnable repaintCallback;
    public AdaptiveFunctionRenderer(DoubleUnaryOperator function) {
        super("function");
        this.function = function;
        // Prime the pool (2 buffers for double-buffering + reserve).
        bufferPool.push(new PointBuffer());
        bufferPool.push(new PointBuffer());
        bufferPool.push(new PointBuffer());
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        // Recalculate on context changes (zoom/resize/scroll).
        if (isContextChanged(context)) {
            startAsyncCalculation(context);
            lastContext = context;
        }

        // If no points are available yet, skip drawing.
        if (renderBuffer.count < 2) {
            return;
        }

        // Use reusable Path2D from BaseRenderer (zero allocation).
        Path2D path = getPathCache();

        // Theme-aware color that fits FlatLaf palettes.
        Color curveColor = UIManager.getColor("Chart.accent.blue");
        if (isMultiColor()) {
            curveColor = themeSeries(context, 0);
        }
        if (curveColor == null) curveColor = seriesOrBase(model, context, 0);

        g2.setColor(curveColor);
        g2.setStroke(getSeriesStroke());

        // Zeichne den Pfad aus dem aktuellen Render-Buffer
        for (int i = 0; i < renderBuffer.count; i++) {
            double px = ChartUtils.transformX(renderBuffer.x[i], context);
            double py = ChartUtils.transformY(renderBuffer.y[i], context);

            if (i == 0) {
                path.moveTo(px, py);
            } else {
                path.lineTo(px, py);
            }
        }

        g2.draw(path);
    }

    /**
     * Sets a callback invoked when new data becomes available.
     *
     * @param callback Runnable for repaint (e.g., chart::repaint)
     */
    public void setRepaintCallback(Runnable callback) {
        this.repaintCallback = callback;
    }

    private boolean isContextChanged(PlotContext context) {
        if (lastContext == null) return true;
        return lastContext.minX() != context.minX() ||
                lastContext.maxX() != context.maxX() ||
                lastContext.minY() != context.minY() ||
                lastContext.maxY() != context.maxY() ||
                !lastContext.plotBounds().equals(context.plotBounds());
    }

    /**
     * Starts asynchronous computation of function samples in virtual threads.
     * Uses double-buffering to avoid blocking the EDT.
     */
    private synchronized void startAsyncCalculation(PlotContext context) {
        if (calculationTask != null && !calculationTask.isDone()) {
            calculationTask.cancel(true);
        }

        calculationTask = CompletableFuture.runAsync(() -> {
            // Get a buffer from the pool or create a new one (avoids races on shared state).
            PointBuffer workingBuffer;
            synchronized (bufferPool) {
                workingBuffer = bufferPool.isEmpty() ? new PointBuffer() : bufferPool.pop();
            }
            workingBuffer.clear();

            double minX = context.minX();
            double maxX = context.maxX();
            double width = context.plotBounds().getWidth();
            int baseSamples = (int) Math.max(100, width);
            double step = (maxX - minX) / baseSamples;
            double thresholdPx = ChartScale.scale(0.25f);

            double lastX = minX;
            double lastY = function.applyAsDouble(lastX);
            if (Double.isFinite(lastY)) {
                workingBuffer.add(lastX, lastY);
            }

            for (int i = 1; i <= baseSamples; i++) {
                if (Thread.currentThread().isInterrupted()) return;

                double x = minX + i * step;
                double y = function.applyAsDouble(x);

                if (Double.isFinite(y)) {
                    if (workingBuffer.count > 0) {
                        sampleRecursive(workingBuffer, workingBuffer.x[workingBuffer.count - 1],
                                workingBuffer.y[workingBuffer.count - 1],
                                x, y, context, thresholdPx, 0);
                    }
                    workingBuffer.add(x, y);
                }
            }

            // Safe buffer swap on the EDT.
            SwingUtilities.invokeLater(() -> {
                synchronized (this) {
                    PointBuffer temp = renderBuffer;
                    renderBuffer = workingBuffer;
                    // Return old buffer to the pool.
                    synchronized (bufferPool) {
                        temp.clear();
                        bufferPool.push(temp);
                    }
                }
                if (repaintCallback != null) repaintCallback.run();
            });
        }, executor);
    }

    /**
     * Recursive adaptive sampling to increase curve detail.
     * Runs on a background thread.
     */
    private void sampleRecursive(PointBuffer buffer, double x1, double y1, double x2, double y2, PlotContext context, double thresholdPx, int depth) {
        if (depth > 12 || Thread.currentThread().isInterrupted()) return;

        double midX = Math.fma(x1 + x2, 0.5, 0);
        double actualY = function.applyAsDouble(midX);

        if (!Double.isFinite(actualY)) return;

        double pix1x = ChartUtils.transformX(x1, context);
        double pix1y = ChartUtils.transformY(y1, context);
        double pix2x = ChartUtils.transformX(x2, context);
        double pix2y = ChartUtils.transformY(y2, context);
        double pixMidX = ChartUtils.transformX(midX, context);
        double pixMidY = ChartUtils.transformY(actualY, context);

        double dist = pointToLineDistance(pixMidX, pixMidY, pix1x, pix1y, pix2x, pix2y);

        if (dist > thresholdPx) {
            sampleRecursive(buffer, x1, y1, midX, actualY, context, thresholdPx, depth + 1);
            buffer.add(midX, actualY);
            sampleRecursive(buffer, midX, actualY, x2, y2, context, thresholdPx, depth + 1);
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

    // Double-buffer for points (zero-allocation after initialization).
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
