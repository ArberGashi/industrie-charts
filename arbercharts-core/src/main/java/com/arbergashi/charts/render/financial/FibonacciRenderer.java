package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * <h1>FibonacciRenderer - Fibonacci Retracement Levels</h1>
 *
 * <p>Professional Fibonacci retracement renderer for technical analysis.
 * Displays key Fibonacci levels based on swing high and swing low.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>Retracement Levels:</b> 0%, 23.6%, 38.2%, 50%, 61.8%, 78.6%, 100%</li>
 *   <li><b>Extension Levels:</b> 127.2%, 161.8%, 261.8%</li>
 *   <li><b>Color Coding:</b> Key levels (38.2%, 50%, 61.8%) emphasized</li>
 *   <li><b>Labels:</b> Level percentages displayed</li>
 *   <li><b>Auto-Detection:</b> Finds swing high/low automatically</li>
 * </ul>
 *
 * <h2>Data Requirements:</h2>
 * <p>Requires price data. Automatically detects swing high and low points
 * within the visible range.</p>
 *
 * <h2>Fibonacci Ratios:</h2>
 * <ul>
 *   <li>0.236 (23.6%) - Shallow retracement</li>
 *   <li>0.382 (38.2%) - Key support/resistance</li>
 *   <li>0.500 (50.0%) - Psychological level</li>
 *   <li>0.618 (61.8%) - Golden ratio</li>
 *   <li>0.786 (78.6%) - Deep retracement</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class FibonacciRenderer extends BaseRenderer {

    private static final double[] RETRACEMENT_LEVELS = {
            0.0, 0.236, 0.382, 0.5, 0.618, 0.786, 1.0
    };

    private static final double[] EXTENSION_LEVELS = {
            1.272, 1.618, 2.618
    };

    private final double[] pxL = new double[2];
    private final double[] pxR = new double[2];
    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];
    private transient ChartModel cachedModel;
    private transient int cachedPointCount;
    private transient SwingPoints cachedSwing;

    public FibonacciRenderer() {
        super("fibonacci");
    }

    private static String formatPercent(double level) {
        // Keep it allocation-light and stable; integer rounding works well for chart labels.
        long tenth = Math.round(level * 1000.0); // e.g. 0.382 -> 382
        long whole = tenth / 10;
        long frac = Math.abs(tenth % 10);
        return whole + "." + frac + "%";
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < 2) return;

        ensureCache(model, n);
        final SwingPoints swing = cachedSwing;
        if (swing == null) return;

        final double range = swing.high - swing.low;
        if (!(range > 0.0)) return;

        final IndicatorRendererSupport.Viewport vp = IndicatorRendererSupport.viewport(context);

        final ChartTheme theme = getResolvedTheme(context);
        final ArberColor baseColor = theme.getAccentColor();
        final ArberColor keyLevelColor = ColorRegistry.adjustBrightness(baseColor, 1.2f);

        final double xLeft = model.getX(0);
        final double xRight = model.getX(n - 1);

        // Draw retracement levels
        for (double level : RETRACEMENT_LEVELS) {
            final double price = swing.low + (range * (1.0 - level));
            final boolean isKeyLevel = level == 0.382 || level == 0.5 || level == 0.618;

            drawFibLevel(canvas, context, xLeft, xRight, price, level,
                    isKeyLevel ? keyLevelColor : baseColor,
                    isKeyLevel, vp);
        }

        // Draw extension levels (only if within current plot Y-range)
        for (double level : EXTENSION_LEVELS) {
            final double price = swing.low + (range * (1.0 - level));
            context.mapToPixel(xLeft, price, pxL);
            final double y = pxL[1];
            if (y >= vp.getY() && y <= vp.getMaxY()) {
                drawFibLevel(canvas, context, xLeft, xRight, price, level,
                        ColorRegistry.applyAlpha(baseColor, 0.5f), false, vp);
            }
        }

        // Draw reference lines for swing high and low
        canvas.setColor(theme.getForeground());
        canvas.setStroke(ChartScale.scale(1.5f));

        context.mapToPixel(xLeft, swing.high, pxL);
        context.mapToPixel(xRight, swing.high, pxR);
        lineX[0] = (float) pxL[0];
        lineY[0] = (float) pxL[1];
        lineX[1] = (float) pxR[0];
        lineY[1] = (float) pxR[1];
        canvas.drawPolyline(lineX, lineY, 2);

        context.mapToPixel(xLeft, swing.low, pxL);
        context.mapToPixel(xRight, swing.low, pxR);
        lineX[0] = (float) pxL[0];
        lineY[0] = (float) pxL[1];
        lineX[1] = (float) pxR[0];
        lineY[1] = (float) pxR[1];
        canvas.drawPolyline(lineX, lineY, 2);
    }

    private void drawFibLevel(ArberCanvas canvas,
                              PlotContext context,
                              double xLeft,
                              double xRight,
                              double price,
                              double level,
                              ArberColor color,
                              boolean isKeyLevel,
                              IndicatorRendererSupport.Viewport vp) {
        context.mapToPixel(xLeft, price, pxL);
        context.mapToPixel(xRight, price, pxR);

        // quick y-clip rejection
        final double y = pxL[1];
        if (y < vp.getY() - 2.0 || y > vp.getMaxY() + 2.0) {
            return;
        }

        // Draw horizontal line
        canvas.setColor(color);
        float width = isKeyLevel ? 2.0f : 1.0f;
        canvas.setStroke(ChartScale.scale(width));
        lineX[0] = (float) pxL[0];
        lineY[0] = (float) pxL[1];
        lineX[1] = (float) pxR[0];
        lineY[1] = (float) pxR[1];
        canvas.drawPolyline(lineX, lineY, 2);
    }

    private void ensureCache(ChartModel model, int n) {
        if (n < 2) {
            cachedModel = model;
            cachedPointCount = 0;
            cachedSwing = null;
            return;
        }

        if (cachedModel == model && cachedPointCount == n && cachedSwing != null) {
            return;
        }

        cachedModel = model;
        cachedPointCount = n;
        cachedSwing = findSwingPoints(model, n);
    }

    /**
     * Finds swing high and swing low in the data.
     * Uses simple max/min for now, can be enhanced with pivot detection.
     */
    private SwingPoints findSwingPoints(ChartModel model, int n) {
        double high = Double.MIN_VALUE;
        double low = Double.MAX_VALUE;

        for (int i = 0; i < n; i++) {
            double val = model.getY(i);
            // Also check max/min fields if available (for OHLC data)
            double maxv = model.getMax(i);
            double minv = model.getMin(i);
            if (maxv != 0) {
                high = Math.max(high, maxv);
                low = Math.min(low, minv);
            } else {
                high = Math.max(high, val);
                low = Math.min(low, val);
            }
        }

        if (high == Double.MIN_VALUE || low == Double.MAX_VALUE) {
            return null;
        }

        return new SwingPoints(high, low);
    }

    private static class SwingPoints {
        final double high;
        final double low;

        SwingPoints(double high, double low) {
            this.high = high;
            this.low = low;
        }
    }
}
