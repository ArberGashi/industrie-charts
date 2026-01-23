package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ChartUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Arrays;

/**
 * ParetoRenderer - combines bar chart with cumulative line.
 *
 * <p><b>Performance notes</b>:
 * Sorting is cached and only recomputed when the model content changes (size / identity hash).
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class ParetoRenderer extends BaseRenderer {

    private static final int FAST_MODE_THRESHOLD = 500; // categories

    static {
        RendererRegistry.register("pareto", new RendererDescriptor("pareto", "renderer.pareto", "/icons/pareto.svg"), ParetoRenderer::new);
    }

    private transient int cachedKey;
    private transient int[] cachedSortedIndices;
    private transient double cachedTotal;
    // Reusable arrays for cumulative line mapping
    private transient double[] cumX;
    private transient double[] cumY;
    // Bucketing buffers for fast bar mode
    private transient double[] bucketMax;
    private transient boolean[] bucketUsed;
    private transient int bucketCount;

    public ParetoRenderer() {
        super("pareto");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        ChartTheme theme = resolveTheme(context);
        int[] sortedIndices = getOrBuildSortedIndices(yData, n);
        double totalSum = cachedTotal;
        if (totalSum <= 0) return;

        boolean fast = n >= FAST_MODE_THRESHOLD;
        if (isMultiColor()) {
            fast = false;
        }

        Color barColor = theme.getSeriesColor(getLayerIndex());
        Color lineEdgeColor = theme.getAccentColor();
        drawBars(g2, xData, yData, sortedIndices, n, context, fast, barColor, theme);
        drawCumulativeLine(g2, xData, yData, sortedIndices, n, totalSum, context, fast, lineEdgeColor);
    }

    private int[] getOrBuildSortedIndices(double[] yData, int n) {
        int key = System.identityHashCode(yData) * 31 + n;
        if (cachedSortedIndices != null && cachedKey == key && cachedSortedIndices.length == n) {
            return cachedSortedIndices;
        }

        int[] indices = new int[n];
        for (int i = 0; i < n; i++) indices[i] = i;

        // Primitive sort (quicksort indices based on yData descending)
        sortIndices(indices, yData, 0, n - 1);

        double total = 0;
        for (int i = 0; i < n; i++) total += yData[i];

        cachedKey = key;
        cachedSortedIndices = indices;
        cachedTotal = total;

        // Ensure reusable arrays are big enough
        if (cumX == null || cumX.length < n) {
            cumX = new double[n];
            cumY = new double[n];
        }

        return indices;
    }

    private void drawBars(Graphics2D g2, double[] xData, double[] yData, int[] indices, int n, PlotContext context, boolean fast, Color barColor, ChartTheme theme) {
        float paddingFactor = ChartAssets.getFloat("chart.bar.padding", 0.2f);
        double barWidth = ChartUtils.calculateBestBarWidth(n, context.plotBounds().getWidth(), paddingFactor);

        double[] tmp = pBuffer();
        context.mapToPixel(0, 0.0, tmp);
        double zeroY = tmp[1];
        // JDK 25: Use Math.clamp() for baseline bounds
        double baselineY = Math.clamp(zeroY, context.plotBounds().getY(), context.plotBounds().getMaxY());

        Rectangle clip = g2.getClipBounds();

        if (!fast) {
            // High-fidelity mode (gradients + optional outlines)
            for (int i = 0; i < n; i++) {
                int idx = indices[i];
                double[] buf = pBuffer();
                context.mapToPixel(xData[idx], yData[idx], buf);
                double x = buf[0] - barWidth / 2;
                double y = Math.min(buf[1], baselineY);
                double height = Math.abs(buf[1] - baselineY);
                if (height < 1.0) height = 1.0;

                if (clip != null) {
                    if (x + barWidth < clip.getX() || x > clip.getX() + clip.getWidth()) continue;
                    if (y > clip.getY() + clip.getHeight() || (y + height) < clip.getY()) continue;
                }

                Shape bar = getRect(x, y, barWidth, height);
                Color perBar = isMultiColor() ? theme.getSeriesColor(i) : barColor;
                if (perBar == null) perBar = barColor;
                g2.setPaint(getCachedGradient(perBar, (float) height));
                g2.fill(bar);

                if (UIManager.getBoolean("Chart.bar.outline")) {
                    g2.setStroke(getCachedStroke(ChartScale.scale(1.0f)));
                    g2.setColor(perBar.darker());
                    g2.draw(bar);
                    g2.setColor(perBar);
                }
            }
            return;
        }

        // Fast mode: bucket bars into pixel columns (keep max height per column).
        int w = Math.max(1, (int) Math.ceil(context.plotBounds().getWidth()));
        if (bucketMax == null || bucketMax.length < w) {
            bucketMax = new double[w];
            bucketUsed = new boolean[w];
        }
        // Reset (only used part)
        if (bucketCount < w) bucketCount = w;
        Arrays.fill(bucketUsed, 0, bucketCount, false);

        double bx = context.plotBounds().getX();
        double bw = context.plotBounds().getWidth();
        double invW = (bw <= 0) ? 0.0 : (bucketCount / bw);

        double[] pixBuf = pBuffer();
        for (int i = 0; i < n; i++) {
            int idx = indices[i];
            context.mapToPixel(xData[idx], yData[idx], pixBuf);
            double xCenter = pixBuf[0];
            int col = (int) ((xCenter - bx) * invW);
            if (col < 0 || col >= bucketCount) continue;
            double height = Math.abs(pixBuf[1] - baselineY);
            if (height < 1.0) height = 1.0;
            if (!bucketUsed[col] || height > bucketMax[col]) {
                bucketMax[col] = height;
                bucketUsed[col] = true;
            }
        }

        // Flat fill only (no gradient cache thrash)
        g2.setColor(barColor);
        double colW = Math.max(1.0, bw / bucketCount);
        for (int col = 0; col < bucketCount; col++) {
            if (!bucketUsed[col]) continue;
            double height = bucketMax[col];
            double x = bx + col * colW;
            double y = baselineY - height;
            Shape bar = getRect(x, y, colW, height);
            if (clip != null) {
                if (x + colW < clip.getX() || x > clip.getX() + clip.getWidth()) continue;
            }
            g2.fill(bar);
        }
    }

    private void drawCumulativeLine(Graphics2D g2, double[] xData, double[] yData, int[] indices, int n, double totalSum, PlotContext context, boolean fast, Color lineEdgeColor) {
        Stroke prevStroke = g2.getStroke();
        Color prevColor = g2.getColor();

        g2.setColor(lineEdgeColor);
        g2.setStroke(getCachedStroke(ChartScale.scale(2.5f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Precompute cumulative mapped points (one pass)
        double currentSum = 0;
        var b = context.plotBounds();
        double by = b.getY();
        double bh = b.getHeight();

        double[] buf = pBuffer();
        for (int i = 0; i < n; i++) {
            int idx = indices[i];
            currentSum += yData[idx];
            double percentage = (currentSum / totalSum);
            cumY[i] = by + bh * (1.0 - percentage);
            // Cheap x mapping: use mapToPixel only on x (y irrelevant)
            context.mapToPixel(xData[idx], 0, buf);
            cumX[i] = buf[0];
        }

        Path2D linePath = getPathCache();
        int lineStep = 1;
        if (fast) lineStep = Math.max(1, n / 1500);
        for (int i = 0; i < n; i += lineStep) {
            if (i == 0) linePath.moveTo(cumX[i], cumY[i]);
            else linePath.lineTo(cumX[i], cumY[i]);
        }
        // ensure last point is included
        if (n > 1 && ((n - 1) % lineStep) != 0) {
            linePath.lineTo(cumX[n - 1], cumY[n - 1]);
        }
        g2.draw(linePath);

        // Draw dots (aggressively decimate in fast mode)
        double dotSize = ChartScale.scale(8.0);
        double halfDot = dotSize / 2;
        double inner = dotSize / 4;
        int dotStep = 1;
        if (n > 2000) dotStep = Math.max(1, n / (fast ? 800 : 2000));

        for (int i = 0; i < n; i += dotStep) {
            double x = cumX[i];
            double y = cumY[i];
            g2.setColor(lineEdgeColor);
            g2.fill(getEllipse(x - halfDot, y - halfDot, dotSize, dotSize));
            g2.setColor(themeBackground(context));
            g2.fill(getEllipse(x - inner, y - inner, dotSize / 2, dotSize / 2));
        }

        g2.setColor(prevColor);
        g2.setStroke(prevStroke);
    }

    private void sortIndices(int[] indices, double[] values, int low, int high) {
        if (low < high) {
            int pi = partition(indices, values, low, high);
            sortIndices(indices, values, low, pi - 1);
            sortIndices(indices, values, pi + 1, high);
        }
    }

    private int partition(int[] indices, double[] values, int low, int high) {
        double pivot = values[indices[high]];
        int i = (low - 1);
        for (int j = low; j < high; j++) {
            if (values[indices[j]] > pivot) { // Descending
                i++;
                int temp = indices[i];
                indices[i] = indices[j];
                indices[j] = temp;
            }
        }
        int temp = indices[i + 1];
        indices[i + 1] = indices[high];
        indices[high] = temp;
        return i + 1;
    }

    public ParetoRenderer setMultiColor(boolean enabled) {
        super.setMultiColor(enabled);
        return this;
    }
}
