package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * Hollow Candlestick renderer (specialized variant).
 *
 * <p>Expected point encoding:</p>
 * <ul>
 *   <li>{@code x} = time/index</li>
 *   <li>{@code weight} = open</li>
 *   <li>{@code min} = low</li>
 *   <li>{@code max} = high</li>
 *   <li>{@code y} = close</li>
 * </ul>
 *
 * <p>Performance policy:</p>
 * <ul>
 *   <li>No allocations in the hot drawing loop (no Point2D creation).</li>
 *   <li>For very dense datasets, switches to a level-of-detail (LOD) representation by aggregating
 *       into per-pixel OHLC buckets.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class CandlestickHollowRenderer extends BaseRenderer {

    static {
        RendererRegistry.register(
                "candlestick_hollow",
                new RendererDescriptor("candlestick_hollow", "renderer.candlestick_hollow", "/icons/candlestick_hollow.svg"),
                CandlestickHollowRenderer::new
        );
    }

    private final Line2D.Double wickLine = new Line2D.Double();
    private final Rectangle2D.Double bodyRect = new Rectangle2D.Double();

    private final double[] px = new double[2];
    private final double[] px2 = new double[2];
    private final double[] px3 = new double[2];
    private final double[] px4 = new double[2];

    // Reusable buffers for LOD aggregation (sized to plot width)
    private int[] lodCount = new int[0];
    private double[] lodOpen = new double[0];
    private double[] lodHigh = new double[0];
    private double[] lodLow = new double[0];
    private double[] lodClose = new double[0];

    public CandlestickHollowRenderer() {
        super("candlestick_hollow");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        final ChartTheme theme = resolveTheme(context);
        final Color bullish = theme.getBullishColor();
        final Color bearish = theme.getBearishColor();

        final Rectangle2D bounds2 = context.plotBounds();
        final Rectangle bounds = bounds2.getBounds();
        final Rectangle clip = g2.getClipBounds();

        // width per candle in pixels
        final double candleW = Math.max(2.0, bounds.getWidth() / (double) Math.max(1, n) * 0.8);
        g2.setStroke(getCachedStroke(ChartScale.scale(1.1f)));

        // Dense datasets: render per-column aggregated OHLC (this is fast and deterministic)
        if (n > Math.max(2000, bounds.width)) {
            renderLod(g2, model, context, bounds, clip, candleW, bullish, bearish);
            return;
        }

        // Per-point rendering (no Point2D allocations; reuse buffers and shapes)
        for (int i = 0; i < n; i++) {
            final double xVal = model.getX(i);
            final double close = model.getY(i);

            context.mapToPixel(xVal, close, px);
            final double x = px[0];

            if (clip != null) {
                if (x + candleW / 2 < clip.getX() || x - candleW / 2 > clip.getX() + clip.getWidth()) continue;
            }

            final double open = model.getWeight(i);
            final double high = model.getMax(i);
            final double low = model.getMin(i);

            final double top = Math.min(open, close);
            final double bottom = Math.max(open, close);

            context.mapToPixel(xVal, high, px2);
            context.mapToPixel(xVal, low, px3);
            context.mapToPixel(xVal, top, px4);
            context.mapToPixel(xVal, bottom, px);

            final double yHigh = px2[1];
            final double yLow = px3[1];
            final double yTop = px4[1];
            final double yBottom = px[1];

            g2.setColor(close >= open ? bullish : bearish);

            // upper wick
            wickLine.x1 = x;
            wickLine.y1 = yHigh;
            wickLine.x2 = x;
            wickLine.y2 = yTop;
            g2.draw(wickLine);

            // lower wick
            wickLine.x1 = x;
            wickLine.y1 = yBottom;
            wickLine.x2 = x;
            wickLine.y2 = yLow;
            g2.draw(wickLine);

            bodyRect.x = x - candleW / 2.0;
            bodyRect.y = yTop;
            bodyRect.width = candleW;
            bodyRect.height = Math.max(1.0, yBottom - yTop);

            if (close >= open) g2.draw(bodyRect);
            else g2.fill(bodyRect);
        }
    }

    private void renderLod(Graphics2D g2, ChartModel model, PlotContext context, Rectangle bounds, Rectangle clip,
                           double candleW, Color bullish, Color bearish) {
        final int w = Math.max(1, bounds.width);
        ensureLodCapacity(w);

        Arrays.fill(lodCount, 0);

        // compute x mapping for bucket selection
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        final int n = model.getPointCount();
        for (int i = 0; i < n; i++) {
            final double x = model.getX(i);
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
        }
        final double span = Math.max(1e-12, maxX - minX);

        // aggregate into buckets
        for (int i = 0; i < n; i++) {
            final double pxv = model.getX(i);
            final int xiRaw = (int) ((pxv - minX) / span * (w - 1));
            final int xi = (xiRaw < 0) ? 0 : Math.min(xiRaw, w - 1);

            final double o = model.getWeight(i);
            final double h = model.getMax(i);
            final double l = model.getMin(i);
            final double c = model.getY(i);

            if (lodCount[xi] == 0) {
                lodOpen[xi] = o;
                lodHigh[xi] = h;
                lodLow[xi] = l;
                lodClose[xi] = c;
            } else {
                // Keep extremes and smooth open/close very lightly for stability.
                // (We still want visible OHLC identity in dense mode.)
                lodOpen[xi] = (lodOpen[xi] + o) * 0.5;
                lodClose[xi] = (lodClose[xi] + c) * 0.5;
                if (h > lodHigh[xi]) lodHigh[xi] = h;
                if (l < lodLow[xi]) lodLow[xi] = l;
            }
            lodCount[xi]++;
        }

        final double pixelStep = bounds.getWidth() / (double) w;
        final double x0 = bounds.getX();
        final double clipLeft = (clip != null) ? clip.getX() : Double.NEGATIVE_INFINITY;
        final double clipRight = (clip != null) ? clip.getMaxX() : Double.POSITIVE_INFINITY;

        for (int xi = 0; xi < w; xi++) {
            if (lodCount[xi] == 0) continue;

            final double cx = x0 + xi * pixelStep + pixelStep * 0.5;
            if (cx + candleW / 2 < clipLeft || cx - candleW / 2 > clipRight) continue;

            final double open = lodOpen[xi];
            final double close = lodClose[xi];
            final double high = lodHigh[xi];
            final double low = lodLow[xi];
            final double top = Math.min(open, close);
            final double bottom = Math.max(open, close);

            context.mapToPixel(cx, high, px);
            context.mapToPixel(cx, low, px2);
            context.mapToPixel(cx, top, px3);
            context.mapToPixel(cx, bottom, px4);

            final double yHigh = px[1];
            final double yLow = px2[1];
            final double yTop = px3[1];
            final double yBottom = px4[1];

            g2.setColor(close >= open ? bullish : bearish);

            wickLine.x1 = cx;
            wickLine.y1 = yHigh;
            wickLine.x2 = cx;
            wickLine.y2 = yTop;
            g2.draw(wickLine);

            wickLine.x1 = cx;
            wickLine.y1 = yBottom;
            wickLine.x2 = cx;
            wickLine.y2 = yLow;
            g2.draw(wickLine);

            bodyRect.x = cx - candleW / 2.0;
            bodyRect.y = yTop;
            bodyRect.width = candleW;
            bodyRect.height = Math.max(1.0, yBottom - yTop);

            if (close >= open) g2.draw(bodyRect);
            else g2.fill(bodyRect);
        }
    }

    private void ensureLodCapacity(int w) {
        if (lodCount.length >= w) return;
        lodCount = new int[w];
        lodOpen = new double[w];
        lodHigh = new double[w];
        lodLow = new double[w];
        lodClose = new double[w];
    }
}
