package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
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
import java.util.Optional;
/**
 * ParetoRenderer - combines bar chart with cumulative line.
 *
 * <p><b>Performance notes</b>:
 * Sorting is cached using the model identity, update stamp, and point count.
 * Models without an update stamp are sorted on each draw. Values are displayed in
 * descending order at the sorted X positions; hit testing retains the original data index.
 *
 * @author Arber Gashi
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class ParetoRenderer extends BaseRenderer {

    private static final int FAST_MODE_THRESHOLD = 500; // categories

    static {
        RendererRegistry.register("pareto", new RendererDescriptor("pareto", "renderer.pareto", "/icons/pareto.svg"), ParetoRenderer::new);
    }

    private transient ChartModel cachedModel;
    private transient long cachedStamp;
    private transient int cachedCount;
    private transient double[] categoryX;
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
        n = Math.min(n, Math.min(xData.length, yData.length));
        if (n == 0 || context.getPlotBounds().width() <= 0 || context.getPlotBounds().height() <= 0) return;

        ChartTheme theme = getResolvedTheme(context);
        int[] sortedIndices = getOrBuildSortedIndices(model, xData, yData, n);
        double totalSum = cachedTotal;
        if (totalSum <= 0) return;

        boolean fast = n >= FAST_MODE_THRESHOLD;
        if (isMultiColor()) {
            fast = false;
        }

        ArberColor barColor = theme.getSeriesColor(getLayerIndex());
        ArberColor lineEdgeColor = theme.getAccentColor();
        drawBars(canvas, categoryX, yData, sortedIndices, n, context, fast, barColor, theme);
        drawCumulativeLine(canvas, categoryX, yData, sortedIndices, n, totalSum, context, fast, lineEdgeColor);
    }

    private int[] getOrBuildSortedIndices(ChartModel model, double[] xData, double[] yData, int n) {
        long stamp = model.getUpdateStamp();
        if (cachedSortedIndices != null && stamp != 0 && model == cachedModel
                && stamp == cachedStamp && n == cachedCount) {
            return cachedSortedIndices;
        }

        int[] indices = cachedSortedIndices;
        if (indices == null || indices.length < n) indices = new int[n];
        if (categoryX == null || categoryX.length < n) categoryX = new double[n];
        System.arraycopy(xData, 0, categoryX, 0, n);
        Arrays.sort(categoryX, 0, n);
        for (int i = 0; i < n; i++) indices[i] = i;

        // Primitive sort (quicksort indices based on yData descending)
        sortIndices(indices, yData, 0, n - 1);

        double total = 0;
        for (int i = 0; i < n; i++) total += yData[i];

        cachedModel = model;
        cachedStamp = stamp;
        cachedCount = n;
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
                context.mapToPixel(xData[i], yData[idx], buf);
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
        bucketCount = w;
        Arrays.fill(bucketUsed, 0, bucketCount, false);

        double bx = bounds.x();
        double bw = bounds.width();
        double invW = (bw <= 0) ? 0.0 : (bucketCount / bw);

        double[] pixBuf = pBuffer();
        for (int i = 0; i < n; i++) {
            int idx = indices[i];
            context.mapToPixel(xData[i], yData[idx], pixBuf);
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
            double y = context.isInvertedY() ? baselineY : baselineY - height;
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
            context.mapToPixel(xData[i], 0, buf);
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
        if (low >= high) return;
        int left = low;
        int right = high;
        double pivot = values[indices[(low + high) >>> 1]];
        while (left <= right) {
            while (Double.compare(values[indices[left]], pivot) > 0) left++;
            while (Double.compare(values[indices[right]], pivot) < 0) right--;
            if (left <= right) {
                int swap = indices[left];
                indices[left++] = indices[right];
                indices[right--] = swap;
            }
        }
        if (low < right) sortIndices(indices, values, low, right);
        if (left < high) sortIndices(indices, values, left, high);
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        double[] xData = model.getXData();
        double[] yData = model.getYData();
        int n = Math.min(model.getPointCount(), Math.min(xData.length, yData.length));
        if (n == 0) return Optional.empty();
        int[] indices = getOrBuildSortedIndices(model, xData, yData, n);
        ArberRect bounds = context.getPlotBounds();
        double width = ChartUtils.getCalculatedBestBarWidth(n, bounds.width(),
                ChartAssets.getFloat("chart.bar.padding", 0.2f));
        double[] point = pBuffer();
        context.mapToPixel(0, 0, point);
        double baseline = Math.clamp(point[1], bounds.y(), bounds.maxY());
        for (int i = 0; i < n; i++) {
            int index = indices[i];
            context.mapToPixel(categoryX[i], yData[index], point);
            if (Math.abs(pixel.x() - point[0]) <= width / 2
                    && pixel.y() >= Math.min(point[1], baseline)
                    && pixel.y() <= Math.max(point[1], baseline)) {
                return Optional.of(index);
            }
        }
        return Optional.empty();
    }

    public ParetoRenderer setMultiColor(boolean enabled){
        super.setMultiColor(enabled);
        return this;
        
    }
}
