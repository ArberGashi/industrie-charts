package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.MultiDimensionalChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ColorUtils;

import java.util.List;
import java.util.Optional;

/**
 * Renders a Parallel Coordinates plot, used for visualizing high-dimensional data.
 * It expects a MultiDimensionalChartModel.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class ParallelCoordinatesRenderer extends BaseRenderer {

    private int hoverIndex = -1;
    private static final double HIT_RADIUS = 8.0;

    public ParallelCoordinatesRenderer() {
        super("parallel_coordinates");
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!(model instanceof MultiDimensionalChartModel mdModel)) {
            return;
        }

        List<double[]> data = mdModel.getMultiDimensionalData();
        List<String> labels = mdModel.getDimensionLabels();
        if (data.isEmpty()) return;

        int dimensions = labels.size();
        ArberRect bounds = context.getPlotBounds();
        double axisSpacing = bounds.width() / (dimensions - 1);
        double plotX = bounds.x();
        double plotY = bounds.y();
        double plotHeight = bounds.height();

        // Find min/max for each dimension for normalization
        double[] minValues = RendererAllocationCache.getDoubleArray(this, "minValues", dimensions);
        double[] maxValues = RendererAllocationCache.getDoubleArray(this, "maxValues", dimensions);
        for (int i = 0; i < dimensions; i++) {
            minValues[i] = Double.MAX_VALUE;
            maxValues[i] = Double.MIN_VALUE;
        }
        for (double[] point : data) {
            for (int i = 0; i < dimensions; i++) {
                if (point[i] < minValues[i]) minValues[i] = point[i];
                if (point[i] > maxValues[i]) maxValues[i] = point[i];
            }
        }

        float baseAlpha = 0.28f;
        float dimAlpha = 0.06f;
        float hoverAlpha = 0.85f;
        ArberColor baseColor = seriesOrBase(model, context, 0);

        float[] xs = RendererAllocationCache.getFloatArray(this, "pc.x", dimensions);
        float[] ys = RendererAllocationCache.getFloatArray(this, "pc.y", dimensions);

        canvas.setStroke(getSeriesStrokeWidth());

        // Draw the polylines for each data point
        for (int p = 0; p < data.size(); p++) {
            double[] point = data.get(p);
            for (int i = 0; i < dimensions; i++) {
                double x = plotX + i * axisSpacing;
                double range = maxValues[i] - minValues[i];
                double normalized = (range == 0) ? 0.5 : (point[i] - minValues[i]) / range;
                double y = plotY + plotHeight * (1 - normalized);
                xs[i] = (float) x;
                ys[i] = (float) y;
            }
            float alpha = baseAlpha;
            if (hoverIndex >= 0) {
                alpha = (p == hoverIndex) ? hoverAlpha : dimAlpha;
            }
            ArberColor lineColor = isMultiColor() ? themeSeries(context, p) : baseColor;
            if (lineColor == null) lineColor = baseColor;
            canvas.setColor(ColorUtils.applyAlpha(lineColor, alpha));
            canvas.drawPolyline(xs, ys, dimensions);
        }

        // Draw the vertical axes
        canvas.setColor(themeGrid(context));
        float[] axisX = RendererAllocationCache.getFloatArray(this, "pc.axis.x", 2);
        float[] axisY = RendererAllocationCache.getFloatArray(this, "pc.axis.y", 2);
        for (int i = 0; i < dimensions; i++) {
            float x = (float) (plotX + i * axisSpacing);
            axisX[0] = x;
            axisY[0] = (float) plotY;
            axisX[1] = x;
            axisY[1] = (float) (plotY + plotHeight);
            canvas.drawPolyline(axisX, axisY, 2);
        }
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        if (!(model instanceof MultiDimensionalChartModel mdModel)) return Optional.empty();
        List<double[]> data = mdModel.getMultiDimensionalData();
        List<String> labels = mdModel.getDimensionLabels();
        if (data.isEmpty() || labels.isEmpty()) return Optional.empty();

        int dimensions = labels.size();
        ArberRect bounds = context.getPlotBounds();
        double axisSpacing = bounds.width() / (dimensions - 1);
        double plotX = bounds.x();
        double plotY = bounds.y();
        double plotHeight = bounds.height();

        double[] minValues = RendererAllocationCache.getDoubleArray(this, "minValues", dimensions);
        double[] maxValues = RendererAllocationCache.getDoubleArray(this, "maxValues", dimensions);
        for (int i = 0; i < dimensions; i++) {
            minValues[i] = Double.MAX_VALUE;
            maxValues[i] = Double.MIN_VALUE;
        }
        for (double[] point : data) {
            for (int i = 0; i < dimensions; i++) {
                if (point[i] < minValues[i]) minValues[i] = point[i];
                if (point[i] > maxValues[i]) maxValues[i] = point[i];
            }
        }

        double minDistSq = Double.MAX_VALUE;
        int bestIndex = -1;
        double px = pixel.x();
        double py = pixel.y();

        for (int p = 0; p < data.size(); p++) {
            double[] point = data.get(p);
            double prevX = 0.0;
            double prevY = 0.0;
            boolean hasPrev = false;
            for (int i = 0; i < dimensions; i++) {
                double x = plotX + i * axisSpacing;
                double range = maxValues[i] - minValues[i];
                double normalized = (range == 0) ? 0.5 : (point[i] - minValues[i]) / range;
                double y = plotY + plotHeight * (1 - normalized);

                if (hasPrev) {
                    double distSq = distanceToSegmentSq(px, py, prevX, prevY, x, y);
                    if (distSq < minDistSq) {
                        minDistSq = distSq;
                        bestIndex = p;
                    }
                }
                prevX = x;
                prevY = y;
                hasPrev = true;
            }
        }

        if (minDistSq <= HIT_RADIUS * HIT_RADIUS) {
            hoverIndex = bestIndex;
            return Optional.of(bestIndex);
        }

        hoverIndex = -1;
        return Optional.empty();
    }

    @Override
    public void clearHover() {
        hoverIndex = -1;
    }

    private static double distanceToSegmentSq(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx == 0 && dy == 0) {
            double sx = px - x1;
            double sy = py - y1;
            return sx * sx + sy * sy;
        }
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        double ix = x1 + t * dx;
        double iy = y1 + t * dy;
        double vx = px - ix;
        double vy = py - iy;
        return vx * vx + vy * vy;
    }
}
