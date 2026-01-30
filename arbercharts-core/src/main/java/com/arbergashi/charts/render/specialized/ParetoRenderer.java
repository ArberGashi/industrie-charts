package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ChartUtils;

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
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
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

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        ChartTheme theme = getResolvedTheme(context);
        int[] sortedIndices = getOrBuildSortedIndices(yData, n);
        double totalSum = cachedTotal;
        if (totalSum <= 0) return;

        boolean fast = n >= FAST_MODE_THRESHOLD;
        if (isMultiColor()) {
            fast = false;
        }

        ArberColor barColor = theme.getSeriesColor(getLayerIndex());
        ArberColor lineEdgeColor = theme.getAccentColor();
        drawBars(canvas, xData, yData, sortedIndices, n, context, fast, barColor, theme);
        drawCumulativeLine(canvas, xData, yData, sortedIndices, n, totalSum, context, fast, lineEdgeColor);
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

    private void drawBars(ArberCanvas canvas, double[] xData, double[] yData, int[] indices, int n, PlotContext context, boolean fast,
                          ArberColor barColor, ChartTheme theme) {
        float paddingFactor = ChartAssets.getFloat("chart.bar.padding", 0.2f);
        ArberRect bounds = context.getPlotBounds();
        double barWidth = ChartUtils.getCalculatedBestBarWidth(n, bounds.width(), paddingFactor);

        double[] tmp = pBuffer();
        context.mapToPixel(0, 0.0, tmp);
        double zeroY = tmp[1];
        double baselineY = Math.clamp(zeroY, bounds.y(), bounds.maxY());

        if (barColor == null) {
            barColor = theme.getSeriesColor(getLayerIndex());
        }

        if (!fast) {
            for (int i = 0; i < n; i++) {
                int idx = indices[i];
                double[] buf = pBuffer();
                context.mapToPixel(xData[idx], yData[idx], buf);
                double x = buf[0] - barWidth / 2;
                double y = Math.min(buf[1], baselineY);
                double height = Math.abs(buf[1] - baselineY);
                if (height < 1.0) height = 1.0;

                if (x + barWidth < bounds.x() || x > bounds.maxX()) continue;
                if (y > bounds.maxY() || (y + height) < bounds.y()) continue;

                ArberColor perBar = isMultiColor() ? theme.getSeriesColor(i) : barColor;
                if (perBar == null) perBar = barColor;
                canvas.setColor(perBar);
                canvas.fillRect((float) x, (float) y, (float) barWidth, (float) height);
            }
            return;
        }

        int w = Math.max(1, (int) Math.ceil(bounds.width()));
        if (bucketMax == null || bucketMax.length < w) {
            bucketMax = new double[w];
            bucketUsed = new boolean[w];
        }
        if (bucketCount < w) bucketCount = w;
        Arrays.fill(bucketUsed, 0, bucketCount, false);

        double bx = bounds.x();
        double bw = bounds.width();
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

        canvas.setColor(barColor);
        double colW = Math.max(1.0, bw / bucketCount);
        for (int col = 0; col < bucketCount; col++) {
            if (!bucketUsed[col]) continue;
            double height = bucketMax[col];
            double x = bx + col * colW;
            double y = baselineY - height;
            if (x + colW < bounds.x() || x > bounds.maxX()) continue;
            canvas.fillRect((float) x, (float) y, (float) colW, (float) height);
        }
    }

    private void drawCumulativeLine(ArberCanvas canvas, double[] xData, double[] yData, int[] indices, int n, double totalSum, PlotContext context, boolean fast, ArberColor lineEdgeColor) {
        if (lineEdgeColor == null) lineEdgeColor = themeAccent(context);
        canvas.setColor(lineEdgeColor);
        canvas.setStroke(ChartScale.scale(2.5f));

        double currentSum = 0;
        var b = context.getPlotBounds();
        double by = b.y();
        double bh = b.height();

        double[] buf = pBuffer();
        for (int i = 0; i < n; i++) {
            int idx = indices[i];
            currentSum += yData[idx];
            double percentage = (currentSum / totalSum);
            cumY[i] = by + bh * (1.0 - percentage);
            context.mapToPixel(xData[idx], 0, buf);
            cumX[i] = buf[0];
        }

        int lineStep = 1;
        if (fast) lineStep = Math.max(1, n / 1500);
        int count = 0;
        float[] xs = RendererAllocationCache.getFloatArray(this, "pareto.lineX", Math.max(2, n));
        float[] ys = RendererAllocationCache.getFloatArray(this, "pareto.lineY", Math.max(2, n));
        for (int i = 0; i < n; i += lineStep) {
            xs[count] = (float) cumX[i];
            ys[count] = (float) cumY[i];
            count++;
        }
        if (n > 1 && ((n - 1) % lineStep) != 0) {
            xs[count] = (float) cumX[n - 1];
            ys[count] = (float) cumY[n - 1];
            count++;
        }
        if (count > 1) {
            canvas.drawPolyline(xs, ys, count);
        }

        double dotSize = ChartScale.scale(8.0);
        double halfDot = dotSize / 2;
        int dotStep = 1;
        if (n > 2000) dotStep = Math.max(1, n / (fast ? 800 : 2000));

        for (int i = 0; i < n; i += dotStep) {
            double x = cumX[i];
            double y = cumY[i];
            canvas.setColor(lineEdgeColor);
            fillCircle(canvas, x, y, halfDot);
            canvas.setColor(themeBackground(context));
            fillCircle(canvas, x, y, halfDot / 2.0);
        }
    }

    private void fillCircle(ArberCanvas canvas, double cx, double cy, double r) {
        int segments = 12;
        float[] xs = RendererAllocationCache.getFloatArray(this, "pareto.cx", segments);
        float[] ys = RendererAllocationCache.getFloatArray(this, "pareto.cy", segments);
        for (int i = 0; i < segments; i++) {
            double a = (2.0 * Math.PI * i) / segments;
            xs[i] = (float) (cx + Math.cos(a) * r);
            ys[i] = (float) (cy + Math.sin(a) * r);
        }
        canvas.fillPolygon(xs, ys, segments);
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

    public ParetoRenderer setMultiColor(boolean enabled){
        super.setMultiColor(enabled);
        return this;
        
    }
}
