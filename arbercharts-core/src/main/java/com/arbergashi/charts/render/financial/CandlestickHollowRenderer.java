package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

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
 *   <li>No allocations in the hot drawing loop (no ArberPoint creation).</li>
 *   <li>For very dense datasets, switches to a level-of-detail (LOD) representation by aggregating
 *       into per-pixel OHLC buckets.</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 */
public final class CandlestickHollowRenderer extends BaseRenderer {

    static {
        RendererRegistry.register(
                "candlestick_hollow",
                new RendererDescriptor("candlestick_hollow", "renderer.candlestick_hollow", "/icons/candlestick_hollow.svg"),
                CandlestickHollowRenderer::new
        );
    }

    private final double[] px = new double[2];
    private final double[] px2 = new double[2];
    private final double[] px3 = new double[2];
    private final double[] px4 = new double[2];

    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];

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
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        final ChartTheme theme = getResolvedTheme(context);
        final ArberColor bullish = theme.getBullishColor();
        final ArberColor bearish = theme.getBearishColor();

        final ArberRect bounds = context.getPlotBounds();
        if (bounds == null || bounds.getWidth() <= 1 || bounds.getHeight() <= 1) return;

        final double candleW = Math.max(2.0, bounds.getWidth() / (double) Math.max(1, n) * 0.8);
        canvas.setStroke((float) ChartScale.scale(1.1f));

        if (n > Math.max(2000, bounds.getWidth())) {
            renderLod(canvas, model, context, bounds, candleW, bullish, bearish);
            return;
        }

        for (int i = 0; i < n; i++) {
            final double xVal = model.getX(i);
            final double close = model.getY(i);

            context.mapToPixel(xVal, close, px);
            final double x = px[0];

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

            canvas.setColor(close >= open ? bullish : bearish);

            drawLine(canvas, x, yHigh, x, yTop);
            drawLine(canvas, x, yBottom, x, yLow);

            float rx = (float) (x - candleW / 2.0);
            float ry = (float) yTop;
            float rw = (float) candleW;
            float rh = (float) Math.max(1.0, yBottom - yTop);

            if (close >= open) canvas.drawRect(rx, ry, rw, rh);
            else canvas.fillRect(rx, ry, rw, rh);
        }
    }

    private void renderLod(ArberCanvas canvas, ChartModel model, PlotContext context, ArberRect bounds,
                           double candleW, ArberColor bullish, ArberColor bearish) {
        final int w = Math.max(1, (int) Math.round(bounds.getWidth()));
        ensureLodCapacity(w);

        Arrays.fill(lodCount, 0);

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        final int n = model.getPointCount();
        for (int i = 0; i < n; i++) {
            final double x = model.getX(i);
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
        }
        final double span = Math.max(1e-12, maxX - minX);

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
                lodOpen[xi] = (lodOpen[xi] + o) * 0.5;
                lodClose[xi] = (lodClose[xi] + c) * 0.5;
                if (h > lodHigh[xi]) lodHigh[xi] = h;
                if (l < lodLow[xi]) lodLow[xi] = l;
            }
            lodCount[xi]++;
        }

        final double pixelStep = bounds.getWidth() / (double) w;
        final double x0 = bounds.x();

        for (int xi = 0; xi < w; xi++) {
            if (lodCount[xi] == 0) continue;

            final double cx = x0 + xi * pixelStep + pixelStep * 0.5;

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

            canvas.setColor(close >= open ? bullish : bearish);

            drawLine(canvas, cx, yHigh, cx, yTop);
            drawLine(canvas, cx, yBottom, cx, yLow);

            float rx = (float) (cx - candleW / 2.0);
            float ry = (float) yTop;
            float rw = (float) candleW;
            float rh = (float) Math.max(1.0, yBottom - yTop);

            if (close >= open) canvas.drawRect(rx, ry, rw, rh);
            else canvas.fillRect(rx, ry, rw, rh);
        }
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        lineX[0] = (float) x1;
        lineX[1] = (float) x2;
        lineY[0] = (float) y1;
        lineY[1] = (float) y2;
        canvas.drawPolyline(lineX, lineY, 2);
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
