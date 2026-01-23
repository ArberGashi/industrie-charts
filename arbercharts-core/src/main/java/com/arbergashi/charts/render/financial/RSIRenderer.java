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
 * <h1>RSIRenderer - Relative Strength Index</h1>
 *
 * <p>Professional RSI indicator renderer for momentum analysis.
 * Displays RSI line with overbought/oversold zones.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>RSI Line:</b> Momentum indicator (0-100 scale)</li>
 *   <li><b>Overbought Zone:</b> Above 70 (red shading)</li>
 *   <li><b>Oversold Zone:</b> Below 30 (green shading)</li>
 *   <li><b>Neutral Zone:</b> 30-70 range</li>
 *   <li><b>Reference Lines:</b> At 30, 50, 70 levels</li>
 * </ul>
 *
 * <h2>Data Mapping:</h2>
 * <pre>
 * ChartPoint fields:
 *   x       → Time/Index
 *   y       → RSI value (0-100)
 * </pre>
 *
 * <h2>Calculation:</h2>
 * <pre>
 * RS = Average Gain / Average Loss (14-period default)
 * RSI = 100 - (100 / (1 + RS))
 * </pre>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class RSIRenderer extends BaseRenderer {

    private static final int DEFAULT_PERIOD = 14;
    private static final double OVERBOUGHT_LEVEL = 70.0;
    private static final double OVERSOLD_LEVEL = 30.0;
    private static final double MIDDLE_LEVEL = 50.0;

    private final double[] pxA = new double[2];
    private final double[] pxB = new double[2];
    private final double[] pxC = new double[2];
    private final double[] pxD = new double[2];

    private final Path2D overboughtZone = new Path2D.Double();
    private final Path2D oversoldZone = new Path2D.Double();
    private final Path2D rsiPath = new Path2D.Double();

    // Cached indicator values (kept reusable to avoid allocations on every repaint)
    private transient ChartModel cachedModel;
    private transient int cachedPointCount;
    @SuppressWarnings("FieldMayBeFinal")
    private double[] rsiValues = new double[0];
    @SuppressWarnings("FieldMayBeFinal")
    private double[] xValues = new double[0];
    private transient int cachedPeriod = DEFAULT_PERIOD;

    public RSIRenderer() {
        super("rsi");
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        final boolean ready = looksLikeRSI(model);
        if (!ready) {
            ensureRSICache(model, cachedPeriod);
            if (cachedPointCount <= 0) return;
        }

        drawRSILayer(g, model, context, !ready);
    }

    private void drawRSILayer(Graphics2D g,
                              ChartModel model,
                              PlotContext context,
                              boolean useCache) {
        final int n = model.getPointCount();
        if (n == 0) return;

        final IndicatorRendererSupport.IndexRange range = IndicatorRendererSupport.visibleRange(g, context, n, 2);
        final int start = range.start();
        final int endExclusive = range.endExclusive();
        if (endExclusive <= start) return;

        final ChartTheme theme = resolveTheme(context);
        final Color rsiColor = theme.getAccentColor();
        final Color overboughtColor = ColorUtils.withAlpha(theme.getBearishColor(), 0.15f);
        final Color oversoldColor = ColorUtils.withAlpha(theme.getBullishColor(), 0.15f);

        final double firstX = model.getX(0);
        final double lastX = model.getX(n - 1);

        // Overbought zone (70-100)
        context.mapToPixel(firstX, 100, pxA);              // left/top
        context.mapToPixel(lastX, 100, pxB);               // right/top
        context.mapToPixel(firstX, OVERBOUGHT_LEVEL, pxC); // left/70
        context.mapToPixel(lastX, OVERBOUGHT_LEVEL, pxD);  // right/70

        overboughtZone.reset();
        overboughtZone.moveTo(pxA[0], pxA[1]);
        overboughtZone.lineTo(pxB[0], pxB[1]);
        overboughtZone.lineTo(pxD[0], pxD[1]);
        overboughtZone.lineTo(pxC[0], pxC[1]);
        overboughtZone.closePath();
        g.setColor(overboughtColor);
        g.fill(overboughtZone);

        // Oversold zone (0-30)
        context.mapToPixel(firstX, 0, pxA);                // left/0
        context.mapToPixel(lastX, 0, pxB);                 // right/0
        context.mapToPixel(firstX, OVERSOLD_LEVEL, pxC);   // left/30
        context.mapToPixel(lastX, OVERSOLD_LEVEL, pxD);    // right/30

        oversoldZone.reset();
        oversoldZone.moveTo(pxA[0], pxA[1]);
        oversoldZone.lineTo(pxB[0], pxB[1]);
        oversoldZone.lineTo(pxD[0], pxD[1]);
        oversoldZone.lineTo(pxC[0], pxC[1]);
        oversoldZone.closePath();
        g.setColor(oversoldColor);
        g.fill(oversoldZone);

        // Reference lines
        g.setColor(theme.getGridColor());
        g.setStroke(getCachedStroke(ChartScale.scale(1.0f)));

        // 70
        context.mapToPixel(firstX, OVERBOUGHT_LEVEL, pxA);
        context.mapToPixel(lastX, OVERBOUGHT_LEVEL, pxB);
        g.draw(getLine(pxA[0], pxA[1], pxB[0], pxB[1]));

        // 50
        context.mapToPixel(firstX, MIDDLE_LEVEL, pxA);
        context.mapToPixel(lastX, MIDDLE_LEVEL, pxB);
        g.draw(getLine(pxA[0], pxA[1], pxB[0], pxB[1]));

        // 30
        context.mapToPixel(firstX, OVERSOLD_LEVEL, pxA);
        context.mapToPixel(lastX, OVERSOLD_LEVEL, pxB);
        g.draw(getLine(pxA[0], pxA[1], pxB[0], pxB[1]));

        // RSI line (draw only visible slice)
        rsiPath.reset();

        if (useCache) {
            int i0 = start;
            // skip warmup NaNs
            while (i0 < endExclusive && !Double.isFinite(rsiValues[i0])) {
                i0++;
            }
            if (i0 >= endExclusive) return;

            context.mapToPixel(xValues[i0], rsiValues[i0], pxA);
            rsiPath.moveTo(pxA[0], pxA[1]);

            for (int i = i0 + 1; i < endExclusive; i++) {
                double y = rsiValues[i];
                if (!Double.isFinite(y)) continue;
                context.mapToPixel(xValues[i], y, pxA);
                rsiPath.lineTo(pxA[0], pxA[1]);
            }
        } else {
            double x0 = model.getX(start);
            context.mapToPixel(x0, model.getY(start), pxA);
            rsiPath.moveTo(pxA[0], pxA[1]);
            for (int i = start + 1; i < endExclusive; i++) {
                double x = model.getX(i);
                double y = model.getY(i);
                context.mapToPixel(x, y, pxA);
                rsiPath.lineTo(pxA[0], pxA[1]);
            }
        }

        g.setColor(rsiColor);
        g.setStroke(getCachedStroke(ChartScale.scale(2.5f)));
        g.draw(rsiPath);
    }

    private boolean looksLikeRSI(ChartModel model) {
        final int n = model.getPointCount();
        // Heuristic: RSI is always in 0..100.
        for (int i = 0; i < n; i++) {
            double y = model.getY(i);
            if (!(y >= 0.0 && y <= 100.0)) return false;
        }
        return true;
    }

    private void ensureRSICache(ChartModel model, int period) {
        final int n = model.getPointCount();
        if (n <= period) {
            cachedModel = model;
            cachedPointCount = 0;
            return;
        }

        if (cachedModel == model && cachedPointCount == n && cachedPeriod == period) {
            return;
        }

        cachedModel = model;
        cachedPointCount = n;
        cachedPeriod = period;

        if (rsiValues.length < n) {
            rsiValues = new double[n];
            xValues = new double[n];
        }

        // Copy x values once.
        for (int i = 0; i < n; i++) {
            xValues[i] = model.getX(i);
        }

        // RSI calculation (Wilder smoothing).
        double avgGain = 0.0;
        double avgLoss = 0.0;

        for (int i = 1; i <= period; i++) {
            final double change = model.getY(i) - model.getY(i - 1);
            if (change > 0) avgGain += change;
            else avgLoss -= change; // change is negative
        }

        avgGain /= period;
        avgLoss /= period;

        // For indices < period, we write NaN so renderers can skip or just show 50.
        for (int i = 0; i < period; i++) {
            rsiValues[i] = Double.NaN;
        }

        for (int i = period; i < n; i++) {
            final double change = model.getY(i) - model.getY(i - 1);
            final double gain = (change > 0) ? change : 0.0;
            final double loss = (change < 0) ? -change : 0.0;

            avgGain = ((avgGain * (period - 1)) + gain) / period;
            avgLoss = ((avgLoss * (period - 1)) + loss) / period;

            // If avgLoss is 0, RSI is 100.
            final double rs = (avgLoss == 0.0) ? Double.POSITIVE_INFINITY : (avgGain / avgLoss);
            rsiValues[i] = 100.0 - (100.0 / (1.0 + rs));
        }

        // Fill initial NaNs with the first computed value for a stable line.
        double firstValid = rsiValues[period];
        if (!Double.isFinite(firstValid)) firstValid = 50.0;
        for (int i = 0; i < period; i++) {
            rsiValues[i] = firstValid;
        }
    }

}
