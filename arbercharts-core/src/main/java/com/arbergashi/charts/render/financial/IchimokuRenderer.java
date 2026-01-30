package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FinancialChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
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
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
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
    private transient float[] pathX;
    private transient float[] pathY;
    private final float[] quadX = new float[4];
    private final float[] quadY = new float[4];

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

    private static double getCalculatedMidpoint(double[] highs, double[] lows, int start, int end) {
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

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < SENKOU_B_PERIOD + DISPLACEMENT) return;

        ensureCache(model);
        if (nTenkan < 2 || nKijun < 2 || nSpanA < 2 || nSpanB < 2 || nChikou < 2) return;

        // Theme-aligned colors (no new Color() allocations)
        final ChartTheme theme = getResolvedTheme(context);
        final ArberColor tenkanColor = ColorRegistry.adjustBrightness(theme.getBearishColor(), 1.15f);
        final ArberColor kijunColor = ColorRegistry.adjustBrightness(theme.getAccentColor(), 1.05f);
        final ArberColor chikouColor = ColorRegistry.applyAlpha(theme.getForeground(), 0.65f);
        final ArberColor spanAColor = ColorRegistry.adjustBrightness(theme.getBullishColor(), 1.10f);
        final ArberColor spanBColor = ColorRegistry.adjustBrightness(theme.getBearishColor(), 1.10f);
        final ArberColor bullishCloudColor = ColorRegistry.applyAlpha(theme.getBullishColor(), 0.15f);
        final ArberColor bearishCloudColor = ColorRegistry.applyAlpha(theme.getBearishColor(), 0.15f);

        final IndicatorRendererSupport.Viewport vp = IndicatorRendererSupport.viewport(context);

        // Draw cloud (Kumo) first (background layer)
        drawCloud(canvas, context, bullishCloudColor, bearishCloudColor, vp);

        // slice all series
        final IndicatorRendererSupport.IndexRange spanRange = IndicatorRendererSupport.visibleRange(context, Math.min(nSpanA, nSpanB), 2);
        final IndicatorRendererSupport.IndexRange tenkanRange = IndicatorRendererSupport.visibleRange(context, nTenkan, 2);
        final IndicatorRendererSupport.IndexRange kijunRange = IndicatorRendererSupport.visibleRange(context, nKijun, 2);
        final IndicatorRendererSupport.IndexRange chikouRange = IndicatorRendererSupport.visibleRange(context, nChikou, 2);

        // Draw leading spans
        drawLine(canvas, xSpanA, ySpanA, spanRange.getStart(), spanRange.getEndExclusive(), context, spanAColor, 1.5f, vp);
        drawLine(canvas, xSpanB, ySpanB, spanRange.getStart(), spanRange.getEndExclusive(), context, spanBColor, 1.5f, vp);

        // Draw base lines
        drawLine(canvas, xKijun, yKijun, kijunRange.getStart(), kijunRange.getEndExclusive(), context, kijunColor, 2.0f, vp);
        drawLine(canvas, xTenkan, yTenkan, tenkanRange.getStart(), tenkanRange.getEndExclusive(), context, tenkanColor, 2.0f, vp);

        // Draw lagging span
        drawLine(canvas, xChikou, yChikou, chikouRange.getStart(), chikouRange.getEndExclusive(), context, chikouColor, 1.5f, vp);
    }

    private void drawCloud(ArberCanvas canvas, PlotContext context, ArberColor bullishColor, ArberColor bearishColor, IndicatorRendererSupport.Viewport vp) {
        if (nSpanA < 2 || nSpanB < 2) return;
        final int n = Math.min(nSpanA, nSpanB);

        final IndicatorRendererSupport.IndexRange range = IndicatorRendererSupport.visibleRange(context, n, 2);
        final int start = range.getStart();
        final int endExclusive = range.getEndExclusive();
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
            if (maxX < vp.getX() || minX > vp.getMaxX() || maxY < vp.getY() || minY > vp.getMaxY()) {
                continue;
            }

            quadX[0] = (float) pxA[0];
            quadY[0] = (float) pxA[1];
            quadX[1] = (float) pxB[0];
            quadY[1] = (float) pxB[1];
            quadX[2] = (float) pxD[0];
            quadY[2] = (float) pxD[1];
            quadX[3] = (float) pxC[0];
            quadY[3] = (float) pxC[1];

            // Color based on which span is on top
            boolean bullish = ySpanA[i] > ySpanB[i];
            canvas.setColor(bullish ? bullishColor : bearishColor);
            canvas.fillPolygon(quadX, quadY, 4);
        }
    }

    private void drawLine(ArberCanvas canvas,
                          double[] xs,
                          double[] ys,
                          int start,
                          int endExclusive,
                          PlotContext context,
                          ArberColor color,
                          float width,
                          IndicatorRendererSupport.Viewport vp) {
        if (endExclusive - start < 2) return;
        int count = endExclusive - start;
        ensurePathCapacity(count);

        final double minX = vp.getX() - 2.0;
        final double maxX = vp.getMaxX() + 2.0;
        int outCount = 0;
        for (int i = start; i < endExclusive; i++) {
            context.mapToPixel(xs[i], ys[i], pxA);
            if (pxA[0] < minX || pxA[0] > maxX) {
                continue;
            }
            pathX[outCount] = (float) pxA[0];
            pathY[outCount] = (float) pxA[1];
            outCount++;
        }
        if (outCount < 2) return;
        canvas.setColor(color);
        canvas.setStroke(ChartScale.scale(width));
        canvas.drawPolyline(pathX, pathY, outCount);
    }

    private void ensurePathCapacity(int count) {
        if (pathX == null || pathX.length < count) {
            pathX = new float[count];
            pathY = new float[count];
        }
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
        final FinancialChartModel fin = (model instanceof FinancialChartModel) ? (FinancialChartModel) model : null;
        if (fin != null) {
            for (int i = 0; i < n; i++) {
                hs[i] = fin.getHigh(i);
                ls[i] = fin.getLow(i);
            }
        } else {
            for (int i = 0; i < n; i++) {
                hs[i] = model.getMax(i);
                ls[i] = model.getMin(i);
            }
        }

        // --- Tenkan (9) ---
        nTenkan = 0;
        for (int i = TENKAN_PERIOD - 1; i < n; i++) {
            xTenkan[nTenkan] = model.getX(i);
            yTenkan[nTenkan] = getCalculatedMidpoint(hs, ls, i - TENKAN_PERIOD + 1, i);
            nTenkan++;
        }

        // --- Kijun (26) ---
        nKijun = 0;
        for (int i = KIJUN_PERIOD - 1; i < n; i++) {
            xKijun[nKijun] = model.getX(i);
            yKijun[nKijun] = getCalculatedMidpoint(hs, ls, i - KIJUN_PERIOD + 1, i);
            nKijun++;
        }

        // --- Span A (forward displaced) ---
        nSpanA = 0;
        for (int i = KIJUN_PERIOD - 1; i < n; i++) {
            double tenkan = getCalculatedMidpoint(hs, ls, i - TENKAN_PERIOD + 1, i);
            double kijun = getCalculatedMidpoint(hs, ls, i - KIJUN_PERIOD + 1, i);
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
            double spanB = getCalculatedMidpoint(hs, ls, i - SENKOU_B_PERIOD + 1, i);
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
            yChikou[nChikou] = (fin != null) ? fin.getClose(i) : model.getY(i);
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
