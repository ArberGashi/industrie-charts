package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * <h1>IchimokuRenderer - Ichimoku Kinko Hyo (Ichimoku Cloud)</h1>
 *
 * <p>Professional Ichimoku Cloud renderer for comprehensive trend analysis.
 * Displays all five Ichimoku lines with cloud (Kumo) visualization.</p>
 *
 * <h2>Interpretation:</h2>
 * <ul>
 *   <li>Price above cloud = Bullish trend</li>
 *   <li>Price below cloud = Bearish trend</li>
 *   <li>Price in cloud = Consolidation/Uncertainty</li>
 *   <li>Green cloud = Bullish (Span A &gt; Span B)</li>
 *   <li>Red cloud = Bearish (Span A &lt; Span B)</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class IchimokuRenderer extends BaseRenderer {

    private static final int TENKAN_PERIOD = 9;
    private static final int KIJUN_PERIOD = 26;
    private static final int SENKOU_B_PERIOD = 52;
    private static final int DISPLACEMENT = 26;

    private final double[] pxA = new double[2];
    private final double[] pxB = new double[2];
    private final double[] pxC = new double[2];
    private final double[] pxD = new double[2];

    /**
     * Reused path for any polyline Ichimoku series.
     */
    private final Path2D linePath = new Path2D.Double();

    /**
     * Reused path for the cloud quad segment.
     */
    private final Path2D cloudSegmentPath = new Path2D.Double();

    // Cached computed series (x + y) to avoid allocating ChartPoint lists
    private transient ChartModel cachedModel;
    private transient int cachedPointCount;

    private transient double[] xTenkan;
    private transient double[] yTenkan;
    private transient int nTenkan;

    private transient double[] xKijun;
    private transient double[] yKijun;
    private transient int nKijun;

    private transient double[] xSpanA;
    private transient double[] ySpanA;
    private transient int nSpanA;

    private transient double[] xSpanB;
    private transient double[] ySpanB;
    private transient int nSpanB;

    private transient double[] xChikou;
    private transient double[] yChikou;
    private transient int nChikou;

    // Cached OHLC helpers (avoid repeated list access in midpoint calc)
    private transient double[] highs;
    private transient double[] lows;

    public IchimokuRenderer() {
        super("ichimoku");
    }

    private static double calculateMidpoint(double[] highs, double[] lows, int start, int end) {
        double high = Double.NEGATIVE_INFINITY;
        double low = Double.POSITIVE_INFINITY;

        for (int i = start; i <= end; i++) {
            double h = highs[i];
            double l = lows[i];
            if (h > high) high = h;
            if (l < low) low = l;
        }

        return (high + low) * 0.5;
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < SENKOU_B_PERIOD + DISPLACEMENT) return;

        ensureCache(model);
        if (nTenkan < 2 || nKijun < 2 || nSpanA < 2 || nSpanB < 2 || nChikou < 2) return;

        // Theme-aligned colors (no new Color() allocations)
        final ChartTheme theme = resolveTheme(context);
        final Color tenkanColor = ColorUtils.adjustBrightness(theme.getBearishColor(), 1.15f);
        final Color kijunColor = ColorUtils.adjustBrightness(theme.getAccentColor(), 1.05f);
        final Color chikouColor = ColorUtils.withAlpha(theme.getForeground(), 0.65f);
        final Color spanAColor = ColorUtils.adjustBrightness(theme.getBullishColor(), 1.10f);
        final Color spanBColor = ColorUtils.adjustBrightness(theme.getBearishColor(), 1.10f);
        final Color bullishCloudColor = ColorUtils.withAlpha(theme.getBullishColor(), 0.15f);
        final Color bearishCloudColor = ColorUtils.withAlpha(theme.getBearishColor(), 0.15f);

        final IndicatorRendererSupport.Viewport vp = IndicatorRendererSupport.viewport(g, context);

        // Draw cloud (Kumo) first (background layer)
        drawCloud(g, context, bullishCloudColor, bearishCloudColor, vp);

        // slice all series
        final IndicatorRendererSupport.IndexRange spanRange = IndicatorRendererSupport.visibleRange(g, context, Math.min(nSpanA, nSpanB), 2);
        final IndicatorRendererSupport.IndexRange tenkanRange = IndicatorRendererSupport.visibleRange(g, context, nTenkan, 2);
        final IndicatorRendererSupport.IndexRange kijunRange = IndicatorRendererSupport.visibleRange(g, context, nKijun, 2);
        final IndicatorRendererSupport.IndexRange chikouRange = IndicatorRendererSupport.visibleRange(g, context, nChikou, 2);

        // Draw leading spans
        drawLine(g, xSpanA, ySpanA, spanRange.start(), spanRange.endExclusive(), context, spanAColor, 1.5f, vp);
        drawLine(g, xSpanB, ySpanB, spanRange.start(), spanRange.endExclusive(), context, spanBColor, 1.5f, vp);

        // Draw base lines
        drawLine(g, xKijun, yKijun, kijunRange.start(), kijunRange.endExclusive(), context, kijunColor, 2.0f, vp);
        drawLine(g, xTenkan, yTenkan, tenkanRange.start(), tenkanRange.endExclusive(), context, tenkanColor, 2.0f, vp);

        // Draw lagging span
        drawLine(g, xChikou, yChikou, chikouRange.start(), chikouRange.endExclusive(), context, chikouColor, 1.5f, vp);
    }

    private void drawCloud(Graphics2D g, PlotContext context, Color bullishColor, Color bearishColor, IndicatorRendererSupport.Viewport vp) {
        if (nSpanA < 2 || nSpanB < 2) return;
        final int n = Math.min(nSpanA, nSpanB);

        final IndicatorRendererSupport.IndexRange range = IndicatorRendererSupport.visibleRange(g, context, n, 2);
        final int start = range.start();
        final int endExclusive = range.endExclusive();
        if (endExclusive - start < 2) return;

        for (int i = start; i < endExclusive - 1; i++) {
            // Zero-allocation coordinate transform
            context.mapToPixel(xSpanA[i], ySpanA[i], pxA);
            context.mapToPixel(xSpanA[i + 1], ySpanA[i + 1], pxB);
            context.mapToPixel(xSpanB[i], ySpanB[i], pxC);
            context.mapToPixel(xSpanB[i + 1], ySpanB[i + 1], pxD);

            // Cheap clip rejection (bounding box of quad)
            double minX = Math.min(Math.min(pxA[0], pxB[0]), Math.min(pxC[0], pxD[0]));
            double maxX = Math.max(Math.max(pxA[0], pxB[0]), Math.max(pxC[0], pxD[0]));
            double minY = Math.min(Math.min(pxA[1], pxB[1]), Math.min(pxC[1], pxD[1]));
            double maxY = Math.max(Math.max(pxA[1], pxB[1]), Math.max(pxC[1], pxD[1]));
            if (maxX < vp.x() || minX > vp.maxX() || maxY < vp.y() || minY > vp.maxY()) {
                continue;
            }

            cloudSegmentPath.reset();
            cloudSegmentPath.moveTo(pxA[0], pxA[1]);
            cloudSegmentPath.lineTo(pxB[0], pxB[1]);
            cloudSegmentPath.lineTo(pxD[0], pxD[1]);
            cloudSegmentPath.lineTo(pxC[0], pxC[1]);
            cloudSegmentPath.closePath();

            // Color based on which span is on top
            boolean bullish = ySpanA[i] > ySpanB[i];
            g.setColor(bullish ? bullishColor : bearishColor);
            g.fill(cloudSegmentPath);
        }
    }

    private void drawLine(Graphics2D g,
                          double[] xs,
                          double[] ys,
                          int start,
                          int endExclusive,
                          PlotContext context,
                          Color color,
                          float width,
                          IndicatorRendererSupport.Viewport vp) {
        if (endExclusive - start < 2) return;

        linePath.reset();

        context.mapToPixel(xs[start], ys[start], pxA);
        linePath.moveTo(pxA[0], pxA[1]);

        final double minX = vp.x() - 2.0;
        final double maxX = vp.maxX() + 2.0;

        for (int i = start + 1; i < endExclusive; i++) {
            context.mapToPixel(xs[i], ys[i], pxA);
            if (pxA[0] < minX || pxA[0] > maxX) {
                linePath.moveTo(pxA[0], pxA[1]);
                continue;
            }
            linePath.lineTo(pxA[0], pxA[1]);
        }

        g.setColor(color);
        g.setStroke(getCachedStroke(ChartScale.scale(width)));
        g.draw(linePath);
    }

    private void ensureCache(ChartModel model) {
        final int n = model.getPointCount();
        if (cachedModel == model && cachedPointCount == n && xTenkan != null) {
            return;
        }

        cachedModel = model;
        cachedPointCount = n;

        ensureCapacity(n);

        // cache highs/lows once
        final double[] hs = highs;
        final double[] ls = lows;
        for (int i = 0; i < n; i++) {
            hs[i] = model.getMax(i);
            ls[i] = model.getMin(i);
        }

        // --- Tenkan (9) ---
        nTenkan = 0;
        for (int i = TENKAN_PERIOD - 1; i < n; i++) {
            xTenkan[nTenkan] = model.getX(i);
            yTenkan[nTenkan] = calculateMidpoint(hs, ls, i - TENKAN_PERIOD + 1, i);
            nTenkan++;
        }

        // --- Kijun (26) ---
        nKijun = 0;
        for (int i = KIJUN_PERIOD - 1; i < n; i++) {
            xKijun[nKijun] = model.getX(i);
            yKijun[nKijun] = calculateMidpoint(hs, ls, i - KIJUN_PERIOD + 1, i);
            nKijun++;
        }

        // --- Span A (forward displaced) ---
        nSpanA = 0;
        for (int i = KIJUN_PERIOD - 1; i < n; i++) {
            double tenkan = calculateMidpoint(hs, ls, i - TENKAN_PERIOD + 1, i);
            double kijun = calculateMidpoint(hs, ls, i - KIJUN_PERIOD + 1, i);
            double spanA = (tenkan + kijun) * 0.5;

            int displacedIndex = i + DISPLACEMENT;
            if (displacedIndex < n) {
                xSpanA[nSpanA] = model.getX(displacedIndex);
                ySpanA[nSpanA] = spanA;
                nSpanA++;
            }
        }

        // --- Span B (52 forward displaced) ---
        nSpanB = 0;
        for (int i = SENKOU_B_PERIOD - 1; i < n; i++) {
            double spanB = calculateMidpoint(hs, ls, i - SENKOU_B_PERIOD + 1, i);
            int displacedIndex = i + DISPLACEMENT;
            if (displacedIndex < n) {
                xSpanB[nSpanB] = model.getX(displacedIndex);
                ySpanB[nSpanB] = spanB;
                nSpanB++;
            }
        }

        // --- Chikou (lagging) ---
        nChikou = 0;
        for (int i = DISPLACEMENT; i < n; i++) {
            int laggedIndex = i - DISPLACEMENT;
            xChikou[nChikou] = model.getX(laggedIndex);
            yChikou[nChikou] = model.getY(i);
            nChikou++;
        }
    }

    private void ensureCapacity(int n) {
        if (xTenkan == null || xTenkan.length < n) {
            xTenkan = new double[n];
            yTenkan = new double[n];
            xKijun = new double[n];
            yKijun = new double[n];
            xSpanA = new double[n];
            ySpanA = new double[n];
            xSpanB = new double[n];
            ySpanB = new double[n];
            xChikou = new double[n];
            yChikou = new double[n];
        }
        if (highs == null || highs.length < n) {
            highs = new double[n];
            lows = new double[n];
        }
    }
}
