package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.internal.HitTestUtils;
import com.arbergashi.charts.internal.RendererDescriptor;
import com.arbergashi.charts.render.RendererRegistry;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Optional;

/**
 * Hollow Candlestick renderer: draws hollow (bullish) and filled (bearish) candlesticks optimized for Swing.
 * Reuses shape objects and scaled strokes to avoid allocations in the paint loop. For very dense datasets
 * it aggregates points into pixel-bins to drastically reduce draw operations and GC pressure.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class CandlestickHollowRenderer extends BaseRenderer {

    static {
        RendererRegistry.register("candlestick_hollow", new RendererDescriptor("candlestick_hollow", "renderer.candlestick_hollow", "/icons/candlestick_hollow.svg"), CandlestickHollowRenderer::new);
    }

    private final double[] pBuffer = new double[2];
    // aggregation buffers when dataset is dense
    private transient boolean[] hasBucket;
    private transient double[] aggOpen, aggClose, aggHigh, aggLow, aggX;
    private transient Color bullishColor;
    private transient Color bearishColor;
    private transient Color outlineColor;
    private transient int uiKey;

    public CandlestickHollowRenderer() {
        super("candlestick_hollow");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;
        double[] xData = model.getXData();
        double[] closeData = model.getYData();
        double[] openData = model.getOpenData();
        double[] highData = model.getHighData();
        double[] lowData = model.getLowData();

        Rectangle bounds = context.plotBounds().getBounds();
        double barWidth = Math.max(1.0, bounds.getWidth() / Math.max(10.0, n) * 0.6);

        Stroke prevStroke = g2.getStroke();
        Color prevColor = g2.getColor();

        ensureUiColors(context);

        g2.setStroke(getSeriesStroke());

        // Decide whether to aggregate by pixel column
        boolean aggregate = n > Math.max(2000, bounds.width * 2);
        if (aggregate) {
            int buckets = Math.max(1, bounds.width);
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

                int bi = (int) Math.round(x - bounds.getX());
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
                g2.setColor(outlineColor);
                g2.draw(getLine(cx, highY, cx, lowY));

                double top = Math.min(aggOpen[bi], aggClose[bi]);
                double height = Math.max(Math.abs(aggOpen[bi] - aggClose[bi]), ChartScale.scale(1.0f));
                Shape body = getRect(left, top, Math.max(1.0, right - left), height);

                boolean bullishCandle = aggClose[bi] < aggOpen[bi];
                if (bullishCandle) {
                    g2.setColor(bullishColor);
                    g2.draw(body);
                } else {
                    g2.setColor(bearishColor);
                    g2.fill(body);
                    g2.setColor(outlineColor);
                    g2.draw(body);
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
                g2.setColor(outlineColor);
                g2.draw(getLine(highX, highY, lowX, lowY));

                double top = Math.min(openY, closeY);
                double height = Math.max(Math.abs(openY - closeY), ChartScale.scale(1.0f));

                Shape body = getRect(left, top, Math.max(1.0, right - left), height);

                boolean bullishCandle = closeY < openY; // lower y is higher price
                if (bullishCandle) {
                    // hollow: stroke only
                    g2.setColor(bullishColor);
                    g2.draw(body);
                } else {
                    // filled
                    g2.setColor(bearishColor);
                    g2.fill(body);
                    g2.setColor(outlineColor);
                    g2.draw(body);
                }
            }
        }

        g2.setColor(prevColor);
        g2.setStroke(prevStroke);
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
        int key = System.identityHashCode(UIManager.getDefaults());
        if (key == uiKey && bullishColor != null) return;
        uiKey = key;

        bullishColor = UIManager.getColor("Chart.candlestick.bullish");
        if (bullishColor == null) bullishColor = themeBullish(context);

        bearishColor = UIManager.getColor("Chart.candlestick.bearish");
        if (bearishColor == null) bearishColor = themeBearish(context);

        outlineColor = UIManager.getColor("Chart.candlestick.outline");
        if (outlineColor == null) outlineColor = UIManager.getColor("Label.foreground");
        if (outlineColor == null) outlineColor = themeForeground(context);
    }

    @Override
    public Optional<Integer> getPointAt(Point2D pixel, ChartModel model, PlotContext context) {
        // fallback: nearest point
        return java.util.Optional.ofNullable(HitTestUtils.nearestPointIndex(pixel, model, context).orElse(null));
    }
}
