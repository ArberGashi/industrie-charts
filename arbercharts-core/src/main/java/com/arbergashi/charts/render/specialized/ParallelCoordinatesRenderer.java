package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.MultiDimensionalChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import com.arbergashi.charts.tools.RendererAllocationCache;
import java.util.Optional;

/**
 * Renders a Parallel Coordinates plot, used for visualizing high-dimensional data.
 * It expects a MultiDimensionalChartModel.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class ParallelCoordinatesRenderer extends BaseRenderer {

    private Font axisFont;
    private Stroke polylineStroke;
    private int hoverIndex = -1;
    private static final double HIT_RADIUS = 8.0;

    public ParallelCoordinatesRenderer() {
        super("parallel_coordinates");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        if (!(model instanceof MultiDimensionalChartModel mdModel)) {
            drawErrorMessage(g2, context, "ParallelCoordinatesRenderer requires a MultiDimensionalChartModel");
            return;
        }

        List<double[]> data = mdModel.getMultiDimensionalData();
        List<String> labels = mdModel.getDimensionLabels();
        if (data.isEmpty()) return;

        int dimensions = labels.size();
        Rectangle2D bounds = context.plotBounds();
        double axisSpacing = bounds.getWidth() / (dimensions - 1);
        double plotX = bounds.getX();
        double plotY = bounds.getY();
        double plotHeight = bounds.getHeight();

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

        // Draw the polylines for each data point
        if (polylineStroke == null) {
            polylineStroke = RendererAllocationCache.getBasicStroke(this, "polylineStroke", 1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, null, 0f);
        }
        g2.setStroke(polylineStroke);
        Path2D path = getPathCache();
        float baseAlpha = 0.28f;
        float dimAlpha = 0.06f;
        float hoverAlpha = 0.85f;
        Color baseColor = seriesOrBase(model, context, 0);

        for (int p = 0; p < data.size(); p++) {
            double[] point = data.get(p);
            path.reset();
            for (int i = 0; i < dimensions; i++) {
                double x = plotX + i * axisSpacing;
                double range = maxValues[i] - minValues[i];
                double normalized = (range == 0) ? 0.5 : (point[i] - minValues[i]) / range;
                double y = plotY + plotHeight * (1 - normalized);

                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            float alpha = baseAlpha;
            if (hoverIndex >= 0) {
                alpha = (p == hoverIndex) ? hoverAlpha : dimAlpha;
            }
            Color lineColor = isMultiColor() ? themeSeries(context, p) : baseColor;
            if (lineColor == null) lineColor = baseColor;
            g2.setColor(ColorUtils.withAlpha(lineColor, alpha));
            g2.draw(path);
        }

        // Draw the vertical axes
        g2.setColor(themeGrid(context));
        if (axisFont == null) {
            axisFont = RendererAllocationCache.getFont(this, "axisFont", "SansSerif", Font.PLAIN, 10);
        }
        g2.setFont(axisFont);
        for (int i = 0; i < dimensions; i++) {
            double x = plotX + i * axisSpacing;
            g2.drawLine((int) x, (int) plotY, (int) x, (int) (plotY + plotHeight));
            g2.drawString(labels.get(i), (int) x - 10, (int) (plotY + plotHeight + 15));
        }
    }

    private void drawErrorMessage(Graphics2D g2, PlotContext context, String message) {
        g2.setColor(themeAccent(context));
        Rectangle2D bounds = context.plotBounds();
        g2.drawString(message, (float) bounds.getX() + 10, (float) bounds.getY() + 20);
    }

    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        if (!(model instanceof MultiDimensionalChartModel mdModel)) return Optional.empty();
        List<double[]> data = mdModel.getMultiDimensionalData();
        List<String> labels = mdModel.getDimensionLabels();
        if (data.isEmpty() || labels.isEmpty()) return Optional.empty();

        int dimensions = labels.size();
        Rectangle2D bounds = context.plotBounds();
        double axisSpacing = bounds.getWidth() / (dimensions - 1);
        double plotX = bounds.getX();
        double plotY = bounds.getY();
        double plotHeight = bounds.getHeight();

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
        double px = pixel.getX();
        double py = pixel.getY();

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
        if (t < 0) t = 0;
        else if (t > 1) t = 1;
        double cx = x1 + t * dx;
        double cy = y1 + t * dy;
        double sx = px - cx;
        double sy = py - cy;
        return sx * sx + sy * sy;
    }
}
