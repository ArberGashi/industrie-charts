package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import javax.swing.*;
import java.awt.*;

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
    private final Rectangle labelBounds = new Rectangle();
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

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < 2) return;

        ensureCache(model, n);
        final SwingPoints swing = cachedSwing;
        if (swing == null) return;

        final double range = swing.high - swing.low;
        if (!(range > 0.0)) return;

        final IndicatorRendererSupport.Viewport vp = IndicatorRendererSupport.viewport(g, context);

        final ChartTheme theme = resolveTheme(context);
        final Color baseColor = theme.getAccentColor();
        final Color keyLevelColor = ColorUtils.adjustBrightness(baseColor, 1.2f);

        final Font baseFont = (g.getFont() != null) ? g.getFont() : UIManager.getFont("Label.font");
        final Font labelFont = baseFont.deriveFont(Font.BOLD, ChartScale.uiFontSize(baseFont, 10.0f));

        final double xLeft = model.getX(0);
        final double xRight = model.getX(n - 1);

        // Draw retracement levels
        for (double level : RETRACEMENT_LEVELS) {
            final double price = swing.low + (range * (1.0 - level));
            final boolean isKeyLevel = level == 0.382 || level == 0.5 || level == 0.618;

            drawFibLevel(g, context, xLeft, xRight, price, level,
                    isKeyLevel ? keyLevelColor : baseColor,
                    isKeyLevel, labelFont, vp);
        }

        // Draw extension levels (only if within current plot Y-range)
        for (double level : EXTENSION_LEVELS) {
            final double price = swing.low + (range * (1.0 - level));
            context.mapToPixel(xLeft, price, pxL);
            final double y = pxL[1];
            if (y >= vp.y() && y <= vp.maxY()) {
                drawFibLevel(g, context, xLeft, xRight, price, level,
                        ColorUtils.withAlpha(baseColor, 0.5f), false, labelFont, vp);
            }
        }

        // Draw reference lines for swing high and low
        g.setColor(theme.getForeground());
        g.setStroke(getCachedStroke(ChartScale.scale(1.5f)));

        context.mapToPixel(xLeft, swing.high, pxL);
        context.mapToPixel(xRight, swing.high, pxR);
        g.draw(getLine(pxL[0], pxL[1], pxR[0], pxR[1]));

        context.mapToPixel(xLeft, swing.low, pxL);
        context.mapToPixel(xRight, swing.low, pxR);
        g.draw(getLine(pxL[0], pxL[1], pxR[0], pxR[1]));
    }

    private void drawFibLevel(Graphics2D g,
                              PlotContext context,
                              double xLeft,
                              double xRight,
                              double price,
                              double level,
                              Color color,
                              boolean isKeyLevel,
                              Font labelFont,
                              IndicatorRendererSupport.Viewport vp) {
        context.mapToPixel(xLeft, price, pxL);
        context.mapToPixel(xRight, price, pxR);

        // quick y-clip rejection
        final double y = pxL[1];
        if (y < vp.y() - 2.0 || y > vp.maxY() + 2.0) {
            return;
        }

        // Draw horizontal line
        g.setColor(color);
        float width = isKeyLevel ? 2.0f : 1.0f;
        g.setStroke(getCachedStroke(ChartScale.scale(width)));
        g.draw(getLine(pxL[0], pxL[1], pxR[0], pxR[1]));

        // Draw label
        final String label = formatPercent(level);
        final float labelX = (float) (pxR[0] - ChartScale.scale(8));
        final float labelY = (float) (pxL[1] - ChartScale.scale(4));

        // fixed-size chip (no FontMetrics; stable for short % labels)
        final int chipW = Math.round(ChartScale.scale(44));
        final int chipH = Math.round(ChartScale.scale(14));
        labelBounds.setBounds((int) (labelX - chipW), (int) (labelY - chipH), chipW, chipH);
        g.setColor(ColorUtils.withAlpha(getTheme().getBackground(), 0.75f));
        g.fill(labelBounds);

        drawLabel(g, label, labelFont, color, labelX - ChartScale.scale(40), labelY);
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
