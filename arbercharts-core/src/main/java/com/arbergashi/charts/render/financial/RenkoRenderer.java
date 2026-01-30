package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
/**
 * Renko renderer.
 *
 * <p>Performance policy:</p>
 * <ul>
 *   <li>No allocations in the hot drawing loop.</li>
 *   <li>Uses an adaptive brick size derived from data magnitude.</li>
 *   <li>Hard-caps brick generation to avoid pathological spikes causing frame stalls.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class RenkoRenderer extends BaseRenderer {

    static {
        RendererRegistry.register(
                "renko",
                new RendererDescriptor("renko", "renderer.renko", "/icons/renko.svg"),
                RenkoRenderer::new
        );
    }

    private final double[] px = new double[2];
    private transient ChartModel cachedModel;
    private transient int cachedPointCount;
    private transient double cachedBrick;
    private transient int cachedBoundsW;
    private transient int cachedBoundsH;
    private transient double cachedMinY = Double.NaN;
    private transient double cachedMaxY = Double.NaN;
    private double[] brickPrices = new double[0];
    private int brickCount;

    public RenkoRenderer() {
        super("renko");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int nPts = model.getPointCount();
        if (nPts < 2) return;

        final ArberRect bounds2 = context.getPlotBounds();

        final double brick = estimateBrickSize(model);
        ensureCache(model, context, brick, bounds2);
        if (brickCount == 0) return;

        final double w = bounds2.width();
        final int n = brickCount;
        final double boxW = Math.max(4.0, w / (double) Math.max(1, n));

        // Get bullish/bearish colors from theme
        final var theme = getResolvedTheme(context);
        final ArberColor bullish = theme.getBullishColor();
        final ArberColor bearish = theme.getBearishColor();

        final double x0 = bounds2.x();
        final double stableX = model.getX(0);
        final double clipLeft = bounds2.minX();
        final double clipRight = bounds2.maxX();

        for (int i = 0; i < n; i++) {
            final double cx = x0 + i * boxW;
            if (cx + boxW < clipLeft || cx > clipRight) continue;

            context.mapToPixel(stableX, brickPrices[i], px);
            final double y = px[1];

            // Determine if this brick is bullish or bearish
            boolean isBullish = (i == 0)
                ? (brickPrices[0] >= model.getY(0))
                : (brickPrices[i] > brickPrices[i - 1]);

            canvas.setColor(isBullish ? bullish : bearish);

            double rx = cx;
            double ry = y - boxW * 0.5;
            double rw = boxW * 0.9; // Small gap between bricks
            double rh = boxW;
            canvas.fillRect((float) rx, (float) ry, (float) rw, (float) rh);
        }
    }

    private double estimateBrickSize(ChartModel model) {
        // Robust, fast heuristic: brick is a small fraction of observed range.
        // This avoids hard-coded 1.0 and scales with typical price magnitude.
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        final int n = model.getPointCount();
        for (int i = 0; i < n; i++) {
            final double v = model.getY(i);
            if (v < min) min = v;
            if (v > max) max = v;
        }
        final double range = Math.max(1e-12, max - min);

        // (range / 150) gives ~150 bricks across full range; clamp for sanity.
        final double raw = range / 150.0;
        return Math.max(1e-6, raw);
    }

    private void ensureCache(ChartModel model, PlotContext context, double brick, ArberRect bounds) {
        final int nPts = model.getPointCount();

        // cache invalidation: data size + theme-independent geometry + brick size
        if (cachedModel == model
                && cachedPointCount == nPts
                && cachedBrick == brick
                && cachedBoundsW == (int) Math.round(bounds.width())
                && cachedBoundsH == (int) Math.round(bounds.height())
                && Double.compare(cachedMinY, context.getMinY()) == 0
                && Double.compare(cachedMaxY, context.getMaxY()) == 0
                && brickCount > 0) {
            return;
        }

        cachedModel = model;
        cachedPointCount = nPts;
        cachedBrick = brick;
        cachedBoundsW = (int) Math.round(bounds.width());
        cachedBoundsH = (int) Math.round(bounds.height());
        cachedMinY = context.getMinY();
        cachedMaxY = context.getMaxY();

        // hard cap: never generate more bricks than a multiple of pixel width.
        // This prevents worst-case O(huge) loops when data jumps massively.
        final int maxBricks = Math.max(512, Math.min(200_000, ((int) Math.round(bounds.width())) * 8));

        ensureCapacity(Math.max(16, Math.min(nPts, maxBricks)));

        double lastClose = model.getY(0);
        int count = 0;

        for (int i = 1; i < nPts && count < maxBricks; i++) {
            final double close = model.getY(i);
            double delta = close - lastClose;
            if (delta == 0) continue;

            // Generate bricks; stop if we hit cap.
            while (Math.abs(delta) >= brick && count < maxBricks) {
                lastClose += (delta > 0) ? brick : -brick;

                if (count == brickPrices.length) {
                    // grow but respect cap
                    ensureCapacity(Math.min(maxBricks, brickPrices.length * 2));
                }
                brickPrices[count++] = lastClose;
                delta = close - lastClose;
            }
        }

        brickCount = count;

    }

    private void ensureCapacity(int min) {
        if (brickPrices.length >= min) return;
        brickPrices = new double[min];
    }
}
