package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.api.types.ArberPoint;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.HitTestUtils;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.platform.render.RendererRegistry;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import java.util.Optional;
/**
 * Hollow Candlestick renderer: draws hollow (bullish) and filled (bearish) candlesticks optimized for Swing.
 * Reuses shape objects and scaled strokes to avoid allocations in the paint loop. For very dense datasets
 * it aggregates points into pixel-bins to drastically reduce draw operations and GC pressure.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class CandlestickHollowRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("candlestick_hollow", new RendererDescriptor("candlestick_hollow", "renderer.candlestick_hollow", "/icons/candlestick_hollow.svg"), CandlestickHollowRenderer::new);
    }

    private final double[] pBuffer = new double[2];
    // aggregation buffers when dataset is dense
    private transient boolean[] hasBucket;
    private transient double[] aggOpen, aggClose, aggHigh, aggLow, aggX;
    private transient ArberColor bullishColor;
    private transient ArberColor bearishColor;
    private transient ArberColor outlineColor;
    private transient int themeKey;

    public CandlestickHollowRenderer() {
        super("candlestick_hollow");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;
        double[] xData = model.getXData();
        double[] closeData = model.getYData();
        double[] openData = model.getOpenData();
        double[] highData = model.getHighData();
        double[] lowData = model.getLowData();

        ArberRect bounds = context.getPlotBounds();
        double barWidth = Math.max(1.0, bounds.width() / Math.max(10.0, n) * 0.6);

        ensureUiColors(context);

        canvas.setStroke(getSeriesStrokeWidth());

        // Decide whether to aggregate by pixel column
        boolean aggregate = n > Math.max(2000, bounds.width() * 2);
        if (aggregate) {
            int buckets = Math.max(1, (int) bounds.width());
            ensureAggBuffers(buckets);
            // reset
            for (int i = 0; i < buckets; i++) hasBucket[i] = false;

            // accumulate OHLC per bucket
            for (int i = 0; i < n; i++) {
                context.mapToPixel(xData[i], closeData[i], pBuffer);
                double c = pBuffer[1];
                double x = pBuffer[0];

                context.mapToPixel(xData[i], openData[i], pBuffer);
                double o = pBuffer[1];

                context.mapToPixel(xData[i], highData[i], pBuffer);
                double h = pBuffer[1];

                context.mapToPixel(xData[i], lowData[i], pBuffer);
                double l = pBuffer[1];

                int bi = (int) Math.round(x - bounds.x());
                if (bi < 0) continue;
                if (bi >= buckets) {
                    bi = buckets - 1;
                }


                if (!hasBucket[bi]) {
                    hasBucket[bi] = true;
                    aggOpen[bi] = o;
                    aggClose[bi] = c;
                    aggHigh[bi] = h;
                    aggLow[bi] = l;
                    aggX[bi] = x;
                } else {
                    // open: keep first (leftmost) -> approximate by minimal x
                    if (x < aggX[bi]) aggOpen[bi] = o;
                    // close: keep last (rightmost)
                    if (x >= aggX[bi]) aggClose[bi] = c;
                    if (h < aggHigh[bi]) aggHigh[bi] = h; // y smaller means higher pixel? keep extreme conservatively
                    if (l > aggLow[bi]) aggLow[bi] = l;
                    aggX[bi] = x; // update last x
                }
            }

            // draw aggregated buckets
            for (int bi = 0; bi < aggX.length; bi++) {
                if (!hasBucket[bi]) continue;
                double cx = aggX[bi];
                double left = cx - barWidth / 2.0;
                double right = cx + barWidth / 2.0;

                double highY = aggHigh[bi];
                double lowY = aggLow[bi];
                canvas.setColor(outlineColor);
                drawLine(canvas, cx, highY, cx, lowY);

                double top = Math.min(aggOpen[bi], aggClose[bi]);
                double height = Math.max(Math.abs(aggOpen[bi] - aggClose[bi]), ChartScale.scale(1.0f));
                float rx = (float) left;
                float ry = (float) top;
                float rw = (float) Math.max(1.0, right - left);
                float rh = (float) height;

                boolean bullishCandle = aggClose[bi] < aggOpen[bi];
                if (bullishCandle) {
                    canvas.setColor(bullishColor);
                    canvas.drawRect(rx, ry, rw, rh);
                } else {
                    canvas.setColor(bearishColor);
                    canvas.fillRect(rx, ry, rw, rh);
                    canvas.setColor(outlineColor);
                    canvas.drawRect(rx, ry, rw, rh);
                }
            }

        } else {
            // original per-point rendering
            for (int i = 0; i < n; i++) {
                // Interpret ChartPoint as OHLC: x=time, y=close, weight=open, min=low, max=high
                context.mapToPixel(xData[i], highData[i], pBuffer);
                double highY = pBuffer[1];
                double highX = pBuffer[0];

                context.mapToPixel(xData[i], lowData[i], pBuffer);
                double lowY = pBuffer[1];
                double lowX = pBuffer[0];

                context.mapToPixel(xData[i], openData[i], pBuffer);
                double openY = pBuffer[1];

                context.mapToPixel(xData[i], closeData[i], pBuffer);
                double closeY = pBuffer[1];
                double cx = pBuffer[0];

                double left = cx - barWidth / 2.0;
                double right = cx + barWidth / 2.0;

                // wick
                canvas.setColor(outlineColor);
                drawLine(canvas, highX, highY, lowX, lowY);

                double top = Math.min(openY, closeY);
                double height = Math.max(Math.abs(openY - closeY), ChartScale.scale(1.0f));

                float rx = (float) left;
                float ry = (float) top;
                float rw = (float) Math.max(1.0, right - left);
                float rh = (float) height;

                boolean bullishCandle = closeY < openY; // lower y is higher price
                if (bullishCandle) {
                    // hollow: stroke only
                    canvas.setColor(bullishColor);
                    canvas.drawRect(rx, ry, rw, rh);
                } else {
                    // filled
                    canvas.setColor(bearishColor);
                    canvas.fillRect(rx, ry, rw, rh);
                    canvas.setColor(outlineColor);
                    canvas.drawRect(rx, ry, rw, rh);
                }
            }
        }
    }

    private void ensureAggBuffers(int buckets) {
        if (hasBucket == null || hasBucket.length < buckets) {
            hasBucket = new boolean[buckets];
            aggOpen = new double[buckets];
            aggClose = new double[buckets];
            aggHigh = new double[buckets];
            aggLow = new double[buckets];
            aggX = new double[buckets];
        }
    }

    private void ensureUiColors(PlotContext context) {
        int key = System.identityHashCode(getResolvedTheme(context));
        if (key == themeKey && bullishColor != null) return;
        themeKey = key;

        bullishColor = themeBullish(context);
        bearishColor = themeBearish(context);
        outlineColor = themeForeground(context);
    }

    private void drawLine(ArberCanvas canvas, double x1, double y1, double x2, double y2) {
        float[] xs = com.arbergashi.charts.tools.RendererAllocationCache.getFloatArray(this, "candleh.line.x", 2);
        float[] ys = com.arbergashi.charts.tools.RendererAllocationCache.getFloatArray(this, "candleh.line.y", 2);
        xs[0] = (float) x1;
        ys[0] = (float) y1;
        xs[1] = (float) x2;
        ys[1] = (float) y2;
        canvas.drawPolyline(xs, ys, 2);
    }

    @Override
    public Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context) {
        // fallback: nearest point
        return java.util.Optional.ofNullable(HitTestUtils.nearestPointIndex(pixel, model, context).orElse(null));
    }
}
