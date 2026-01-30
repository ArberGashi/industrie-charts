package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ColorRegistry;
import com.arbergashi.charts.util.MathUtils;
/**
 * Liquidity heatmap renderer for level-2 style depth visualization.
 *
 * <p>Uses X as time/index, Y as price level, and weight as depth.</p>
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class LiquidityHeatmapRenderer extends BaseRenderer {
    static {
        RendererRegistry.register(
                "liquidity_heatmap",
                new RendererDescriptor("liquidity_heatmap", "renderer.liquidity_heatmap", "/icons/heatmap.svg"),
                LiquidityHeatmapRenderer::new
        );
    }

    private static final String KEY_ENABLED = "Chart.financial.liquidityHeatmap.enabled";
    private static final String KEY_BINS_X = "Chart.financial.liquidityHeatmap.binsX";
    private static final String KEY_BINS_Y = "Chart.financial.liquidityHeatmap.binsY";
    private static final String KEY_ALPHA = "Chart.financial.liquidityHeatmap.alpha";
    private static final String KEY_COLOR = "Chart.financial.liquidityHeatmap.color";

    private static final Snapshot SNAPSHOT = new Snapshot();

    private double[] bins;
    private int[] stamps;
    private int stamp = 1;
    private int lastCols = -1;
    private int lastRows = -1;

    public LiquidityHeatmapRenderer() {
        super("liquidityHeatmap");
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!ChartAssets.getBoolean(KEY_ENABLED, true)) return;
        int count = model.getPointCount();
        if (count <= 0) return;

        int cols = Math.max(8, ChartAssets.getInt(KEY_BINS_X, 60));
        int rows = Math.max(8, ChartAssets.getInt(KEY_BINS_Y, 40));
        ensureBins(cols, rows);

        double[] xs = model.getXData();
        double[] ys = model.getYData();
        double[] ws = model.getWeightData();
        int n = Math.min(count, Math.min(xs.length, Math.min(ys.length, ws.length)));
        if (n <= 0) return;

        double minX = context.getMinX();
        double maxX = context.getMaxX();
        double minY = context.getMinY();
        double maxY = context.getMaxY();
        if (!(Double.isFinite(minX) && Double.isFinite(maxX) && maxX > minX)) return;
        if (!(Double.isFinite(minY) && Double.isFinite(maxY) && maxY > minY)) return;

        double stepX = (maxX - minX) / cols;
        double stepY = (maxY - minY) / rows;
        if (stepX <= 0 || stepY <= 0) return;

        stamp = (stamp == Integer.MAX_VALUE) ? 1 : stamp + 1;
        double maxBin = 0.0;

        for (int i = 0; i < n; i++) {
            double x = xs[i];
            double y = ys[i];
            double w = ws[i];
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(w)) continue;
            int c = (int) ((x - minX) / stepX);
            int r = (int) ((y - minY) / stepY);
            if (c < 0 || c >= cols || r < 0 || r >= rows) continue;
            int idx = r * cols + c;
            if (stamps[idx] != stamp) {
                stamps[idx] = stamp;
                bins[idx] = 0.0;
            }
            double v = bins[idx] + Math.max(0.0, w);
            bins[idx] = v;
            if (v > maxBin) maxBin = v;
        }

        if (maxBin <= 0.0) return;

        ChartTheme theme = getResolvedTheme(context);
        ArberColor base = ChartAssets.getColor(KEY_COLOR, theme.getAccentColor());
        float alpha = (float) MathUtils.clamp(ChartAssets.getFloat(KEY_ALPHA, 0.35f), 0.05f, 0.85f);

        setSnapshot(minX, maxX, minY, maxY, cols, rows, maxBin);

        double[] p0 = pBuffer();
        double[] p1 = pBuffer4();

        for (int r = 0; r < rows; r++) {
            double y0 = minY + r * stepY;
            double y1 = y0 + stepY;
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                if (stamps[idx] != stamp) continue;
                double v = bins[idx];
                if (v <= 0) continue;
                float a = alpha * (float) Math.sqrt(v / maxBin);
                if (a <= 0.01f) continue;
                canvas.setColor(ColorRegistry.applyAlpha(base, a));

                double x0 = minX + c * stepX;
                double x1 = x0 + stepX;
                context.mapToPixel(x0, y0, p0);
                context.mapToPixel(x1, y1, p1);

                double px = Math.min(p0[0], p1[0]);
                double py = Math.min(p0[1], p1[1]);
                double pw = Math.abs(p1[0] - p0[0]);
                double ph = Math.abs(p1[1] - p0[1]);
                canvas.fillRect((float) px, (float) py, (float) pw, (float) ph);
            }
        }
    }

    private void ensureBins(int cols, int rows) {
        if (cols == lastCols && rows == lastRows && bins != null && stamps != null) return;
        lastCols = cols;
        lastRows = rows;
        bins = new double[cols * rows];
        stamps = new int[cols * rows];
        stamp = 1;
    }

    private void setSnapshot(double minX, double maxX, double minY, double maxY,
                                int cols, int rows, double maxBin) {
        SNAPSHOT.minX = minX;
        SNAPSHOT.maxX = maxX;
        SNAPSHOT.minY = minY;
        SNAPSHOT.maxY = maxY;
        SNAPSHOT.cols = cols;
        SNAPSHOT.rows = rows;
        SNAPSHOT.maxBin = maxBin;
        SNAPSHOT.bins = bins;
        SNAPSHOT.stamps = stamps;
        SNAPSHOT.stamp = stamp;
    }

    public static double sampleNormalized(double x, double y) {
        if (SNAPSHOT.cols <= 0 || SNAPSHOT.rows <= 0 || SNAPSHOT.maxBin <= 0.0) return 0.0;
        if (x < SNAPSHOT.minX || x > SNAPSHOT.maxX || y < SNAPSHOT.minY || y > SNAPSHOT.maxY) return 0.0;

        double stepX = (SNAPSHOT.maxX - SNAPSHOT.minX) / SNAPSHOT.cols;
        double stepY = (SNAPSHOT.maxY - SNAPSHOT.minY) / SNAPSHOT.rows;
        if (stepX <= 0 || stepY <= 0) return 0.0;

        int c = (int) ((x - SNAPSHOT.minX) / stepX);
        int r = (int) ((y - SNAPSHOT.minY) / stepY);
        if (c < 0 || c >= SNAPSHOT.cols || r < 0 || r >= SNAPSHOT.rows) return 0.0;
        int idx = r * SNAPSHOT.cols + c;
        if (SNAPSHOT.stamps == null || SNAPSHOT.bins == null) return 0.0;
        if (SNAPSHOT.stamps[idx] != SNAPSHOT.stamp) return 0.0;
        return SNAPSHOT.bins[idx] / SNAPSHOT.maxBin;
    }

    private static final class Snapshot {
        double minX;
        double maxX;
        double minY;
        double maxY;
        int cols;
        int rows;
        double maxBin;
        double[] bins;
        int[] stamps;
        int stamp;
    }
}
