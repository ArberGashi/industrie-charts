package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ColorUtils;
import com.arbergashi.charts.util.MathUtils;
/**
 * Renderer for 2D heatmaps.
 *
 * <p>Unified renderer for density and grid visualizations.
 * Supports two modes:
 * 1. <b>Grid mode:</b> Direct rendering of a double[][] matrix (via {@link #setGridData}).
 * 2. <b>Density mode:</b> Automatic binning of {@link ChartModel} points into a grid.</p>
 *
 * <p>Optimized for performance via direct pixel operations and a color LUT.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class HeatmapRenderer extends BaseRenderer implements ChartRenderer {
    static {
        RendererRegistry.register(
                "heatmap",
                new RendererDescriptor("heatmap", "renderer.heatmap", "/icons/heatmap.svg"),
                HeatmapRenderer::new
        );
    }

    // Mapping Buffer
    private final double[] p0 = new double[2];
    private final double[] p1 = new double[2];
    // Zero-Allocation: Color Lookup Table (LUT)
    private final ArberColor[] colorLut = new ArberColor[256];
    private ChartTheme lastTheme;
    private boolean lastMultiColor;
    private double[][] gridData;
    private double minVal = 0;
    private double maxVal = 1;
    private double gridMinX, gridMaxX, gridMinY, gridMaxY;
    private transient double lastHoverX;
    private transient double lastHoverY;
    private transient double lastHoverValue;
    private transient boolean lastHoverValid;

    public HeatmapRenderer() {
        super("heatmap");
    }

    @Override
    public String getName() {
        return "Heatmap";
    }

    @Override
    public String getTooltipText(int index, ChartModel model) {
        if (lastHoverValid) {
            return String.format("(%.2f, %.2f) = %.3f", lastHoverX, lastHoverY, lastHoverValue);
        }
        // Simple tooltip: show value at index if available
        if (model == null || index < 0 || index >= model.getPointCount()) return null;
        double x = model.getXData()[index];
        double y = model.getYData()[index];
        return String.format("(%.2f, %.2f)", x, y);
    }

    @Override
    public java.util.Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        lastHoverValid = false;
        if (pixel == null || context == null) return java.util.Optional.empty();

        double[] data = pBuffer();
        context.mapToData(pixel.x(), pixel.y(), data);

        if (gridData != null && gridData.length > 0) {
            int rows = gridData.length;
            int cols = gridData[0].length;
            if (data[0] < gridMinX || data[0] > gridMaxX || data[1] < gridMinY || data[1] > gridMaxY) {
                return java.util.Optional.empty();
            }
            double stepX = (gridMaxX - gridMinX) / cols;
            double stepY = (gridMaxY - gridMinY) / rows;
            int c = (int) ((data[0] - gridMinX) / stepX);
            int r = (int) ((data[1] - gridMinY) / stepY);
            if (c < 0 || c >= cols || r < 0 || r >= rows) return java.util.Optional.empty();
            lastHoverX = gridMinX + (c + 0.5) * stepX;
            lastHoverY = gridMinY + (r + 0.5) * stepY;
            lastHoverValue = gridData[r][c];
            lastHoverValid = true;
            return java.util.Optional.of(r * cols + c);
        }

        if (model == null || model.getPointCount() == 0) return java.util.Optional.empty();

        int cols = ChartAssets.getInt("chart.heatmap.cols", 64);
        int rows = ChartAssets.getInt("chart.heatmap.rows", 64);
        if (cols <= 0 || rows <= 0) return java.util.Optional.empty();

        double minX = context.getMinX();
        double maxX = context.getMaxX();
        double minY = context.getMinY();
        double maxY = context.getMaxY();
        if (data[0] < minX || data[0] > maxX || data[1] < minY || data[1] > maxY) {
            return java.util.Optional.empty();
        }

        double stepX = (maxX - minX) / cols;
        double stepY = (maxY - minY) / rows;
        int c = (int) ((data[0] - minX) / stepX);
        int r = (int) ((data[1] - minY) / stepY);
        if (c < 0 || c >= cols || r < 0 || r >= rows) return java.util.Optional.empty();

        double binMinX = minX + c * stepX;
        double binMaxX = binMinX + stepX;
        double binMinY = minY + r * stepY;
        double binMaxY = binMinY + stepY;

        double[] xData = model.getXData();
        double[] yData = model.getYData();
        int count = Math.min(model.getPointCount(), Math.min(xData.length, yData.length));
        int limit = count;
        if (limit <= 0) return java.util.Optional.empty();
        int hits = 0;
        for (int i = 0; i < limit; i++) {
            double x = xData[i];
            double y = yData[i];
            if (x >= binMinX && x < binMaxX && y >= binMinY && y < binMaxY) {
                hits++;
            }
        }

        lastHoverX = binMinX + stepX * 0.5;
        lastHoverY = binMinY + stepY * 0.5;
        lastHoverValue = hits;
        lastHoverValid = true;
        return java.util.Optional.of(r * cols + c);
    }

    /**
     * Sets the grid data.
     *
     * @param data [rows][cols] array (row = y, col = x).
     * @param minX Start X.
     * @param maxX End X.
     * @param minY Start Y.
     * @param maxY End Y.
     */
    public HeatmapRenderer setGridData(double[][] data, double minX, double maxX, double minY, double maxY) {
        this.gridData = data;
        this.gridMinX = minX;
        this.gridMaxX = maxX;
        this.gridMinY = minY;
        this.gridMaxY = maxY;

        // Auto range for colors.
        minVal = Double.MAX_VALUE;
        maxVal = -Double.MAX_VALUE;
        for (double[] row : data) {
            for (double v : row) {
                if (v < minVal) minVal = v;
                if (v > maxVal) maxVal = v;
            }
        }
        return this;
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        ensureLut(context);
        // Mode decision: explicit grid or automatic binning?
        if (gridData != null && gridData.length > 0) {
            drawGrid(canvas, gridData, gridMinX, gridMaxX, gridMinY, gridMaxY, context);
        } else {
            drawDensityFromModel(canvas, model, context);
        }
    }

    private void drawDensityFromModel(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        int cols = ChartAssets.getInt("chart.heatmap.cols", 64);
        int rows = ChartAssets.getInt("chart.heatmap.rows", 64);

        // Temporary grid for binning (stack/heap, depends on resolution).
        // For zero-allocation, this could be cached when cols/rows do not change.
        double[][] bins = new double[rows][cols];

        double minX = context.getMinX();
        double maxX = context.getMaxX();
        double minY = context.getMinY();
        double maxY = context.getMaxY();

        double rangeX = maxX - minX;
        double rangeY = maxY - minY;
        if (rangeX <= 0 || rangeY <= 0) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();

        double maxBin = 0;

        for (int i = 0; i < count; i++) {
            double x = xData[i];
            double y = yData[i];

            if (x < minX || x >= maxX || y < minY || y >= maxY) continue;

            int c = (int) ((x - minX) / rangeX * cols);
            int r = (int) ((y - minY) / rangeY * rows);

            // Clamp indices
            if (c >= cols) c = cols - 1;
            if (r >= rows) r = rows - 1;

            bins[r][c]++;
            if (bins[r][c] > maxBin) maxBin = bins[r][c];
        }

        if (maxBin > 0) {
            // Temporarily override min/max for this render pass.
            double oldMin = this.minVal;
            double oldMax = this.maxVal;
            this.minVal = 0;
            this.maxVal = maxBin;

            drawGrid(canvas, bins, minX, maxX, minY, maxY, context);

            this.minVal = oldMin;
            this.maxVal = oldMax;
        }
    }

    private void drawGrid(ArberCanvas canvas, double[][] data, double minX, double maxX, double minY, double maxY, PlotContext context) {
        int rows = data.length;
        int cols = data[0].length;

        double stepX = (maxX - minX) / cols;
        double stepY = (maxY - minY) / rows;

        // Visibility check (culling).
        if (maxX < context.getMinX() || minX > context.getMaxX() ||
                maxY < context.getMinY() || minY > context.getMaxY()) {
            return;
        }

        // Iterate over the grid.
        for (int r = 0; r < rows; r++) {
            double y = minY + r * stepY;
            // Y-Culling
            if (y + stepY < context.getMinY() || y > context.getMaxY()) continue;

            for (int c = 0; c < cols; c++) {
                double x = minX + c * stepX;
                // X-Culling
                if (x + stepX < context.getMinX() || x > context.getMaxX()) continue;

                double val = data[r][c];
                if (val <= minVal) continue; // Skip empty/low cells

                // Compute color (simple heatmap gradient: blue -> green -> red).
                float norm = (float) MathUtils.clamp((val - minVal) / (maxVal - minVal), 0, 1);
                int colorIdx = (int) (norm * 255);
                canvas.setColor(colorLut[colorIdx]);

                // Pixel-Koordinaten berechnen
                context.mapToPixel(x, y, p0);
                context.mapToPixel(x + stepX, y + stepY, p1);

                float px = (float) p0[0];
                float py = (float) p1[1]; // p1 is top in plot coordinates
                float pw = (float) Math.ceil(p1[0] - p0[0]);
                float ph = (float) Math.ceil(p0[1] - p1[1]);

                canvas.fillRect(px, py, pw, ph);
            }
        }
    }

    private void ensureLut(PlotContext context) {
        ChartTheme theme = getResolvedTheme(context);
        boolean multi = isMultiColor();
        if (theme == lastTheme && lastMultiColor == multi) return;
        lastTheme = theme;
        lastMultiColor = multi;
        rebuildLut(theme, multi);
    }

    private void rebuildLut(ChartTheme theme, boolean multiColor) {
        ArberColor c0;
        ArberColor c1;
        ArberColor c2;
        ArberColor c3;
        if (multiColor) {
            c0 = theme.getSeriesColor(0);
            c1 = theme.getSeriesColor(1);
            c2 = theme.getSeriesColor(2);
            c3 = theme.getSeriesColor(3);
        } else {
            ArberColor base = theme.getAccentColor();
            c0 = ColorUtils.applyAlpha(base, 0.25f);
            c1 = ColorUtils.applyAlpha(base, 0.45f);
            c2 = ColorUtils.applyAlpha(base, 0.65f);
            c3 = ColorUtils.applyAlpha(base, 0.85f);
        }

        for (int i = 0; i < 256; i++) {
            float t = i / 255.0f;
            if (t < 0.33f) {
                colorLut[i] = ColorUtils.interpolate(c0, c1, t / 0.33f);
            } else if (t < 0.66f) {
                colorLut[i] = ColorUtils.interpolate(c1, c2, (t - 0.33f) / 0.33f);
            } else {
                colorLut[i] = ColorUtils.interpolate(c2, c3, (t - 0.66f) / 0.34f);
            }
        }
    }
}
