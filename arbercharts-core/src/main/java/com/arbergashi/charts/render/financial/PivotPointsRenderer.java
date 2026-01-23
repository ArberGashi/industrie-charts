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
 * <h1>PivotPointsRenderer - Support/Resistance Pivot Points</h1>
 *
 * <p>Professional pivot points renderer for intraday trading levels.
 * Displays pivot point (PP) with support (S1, S2, S3) and resistance (R1, R2, R3) levels.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><b>Pivot Point (PP):</b> Central reference level (blue)</li>
 *   <li><b>Resistance Levels:</b> R1, R2, R3 (red, progressively lighter)</li>
 *   <li><b>Support Levels:</b> S1, S2, S3 (green, progressively lighter)</li>
 *   <li><b>Labels:</b> Level names displayed at right edge</li>
 *   <li><b>Multiple Methods:</b> Standard, Fibonacci, Woodie, Camarilla</li>
 * </ul>
 *
 * <h2>Data Requirements:</h2>
 * <p>Requires previous period's OHLC data (High, Low, Close).</p>
 *
 * <h2>Calculation (Standard Method):</h2>
 * <pre>
 * PP = (High + Low + Close) / 3
 * R1 = 2*PP - Low
 * R2 = PP + (High - Low)
 * R3 = High + 2*(PP - Low)
 * S1 = 2*PP - High
 * S2 = PP - (High - Low)
 * S3 = Low - 2*(High - PP)
 * </pre>
 *
 * <h2>Interpretation:</h2>
 * <ul>
 *   <li>Price above PP = Bullish bias</li>
 *   <li>Price below PP = Bearish bias</li>
 *   <li>R1, R2, R3 = Potential resistance targets</li>
 *   <li>S1, S2, S3 = Potential support targets</li>
 *   <li>Breaks above/below levels = Strong momentum</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class PivotPointsRenderer extends BaseRenderer {

    private final double[] pxL = new double[2];
    private final double[] pxR = new double[2];
    private final Rectangle labelBounds = new Rectangle();
    private PivotMethod method = PivotMethod.STANDARD;
    // Cached pivot levels
    private transient ChartModel cachedModel;
    private transient int cachedPointCount;
    private transient PivotLevels cachedPivots;
    public PivotPointsRenderer() {
        super("pivot_points");
    }
    @SuppressWarnings("unused")
    public PivotPointsRenderer(PivotMethod method) {
        super("pivot_points");
        this.method = method;
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < 2) return;

        ensureCache(model);
        final PivotLevels pivots = cachedPivots;
        if (pivots == null) return;

        final IndicatorRendererSupport.Viewport vp = IndicatorRendererSupport.viewport(g, context);

        final ChartTheme theme = resolveTheme(context);
        final Color ppColor = theme.getAccentColor();
        final Color r1Color = ColorUtils.adjustBrightness(theme.getBearishColor(), 1.0f);
        final Color r2Color = ColorUtils.adjustBrightness(theme.getBearishColor(), 1.2f);
        final Color r3Color = ColorUtils.adjustBrightness(theme.getBearishColor(), 1.4f);
        final Color s1Color = ColorUtils.adjustBrightness(theme.getBullishColor(), 1.0f);
        final Color s2Color = ColorUtils.adjustBrightness(theme.getBullishColor(), 1.2f);
        final Color s3Color = ColorUtils.adjustBrightness(theme.getBullishColor(), 1.4f);

        final Font baseFont = g.getFont();
        final Font resolvedBase = (baseFont != null ? baseFont : UIManager.getFont("Label.font"));
        final Font labelFont = resolvedBase
                .deriveFont(Font.BOLD, ChartScale.uiFontSize(resolvedBase, 10.0f));

        final double xLeft = model.getX(0);
        final double xRight = model.getX(n - 1);

        // Draw resistance levels
        drawPivotLevel(g, context, xLeft, xRight, pivots.r3, "R3", r3Color, 1.0f, labelFont, vp);
        drawPivotLevel(g, context, xLeft, xRight, pivots.r2, "R2", r2Color, 1.5f, labelFont, vp);
        drawPivotLevel(g, context, xLeft, xRight, pivots.r1, "R1", r1Color, 2.0f, labelFont, vp);

        // Draw pivot point
        drawPivotLevel(g, context, xLeft, xRight, pivots.pp, "PP", ppColor, 2.5f, labelFont, vp);

        // Draw support levels
        drawPivotLevel(g, context, xLeft, xRight, pivots.s1, "S1", s1Color, 2.0f, labelFont, vp);
        drawPivotLevel(g, context, xLeft, xRight, pivots.s2, "S2", s2Color, 1.5f, labelFont, vp);
        drawPivotLevel(g, context, xLeft, xRight, pivots.s3, "S3", s3Color, 1.0f, labelFont, vp);
    }

    private void drawPivotLevel(Graphics2D g,
                                PlotContext context,
                                double xLeft,
                                double xRight,
                                double level,
                                String label,
                                Color color,
                                float width,
                                Font labelFont,
                                IndicatorRendererSupport.Viewport vp) {
        context.mapToPixel(xLeft, level, pxL);
        context.mapToPixel(xRight, level, pxR);

        // quick y-clip rejection
        final double y = pxR[1];
        if (y < vp.y() - 2.0 || y > vp.maxY() + 2.0) {
            return;
        }

        // Draw line
        g.setColor(color);
        g.setStroke(getCachedStroke(ChartScale.scale(width)));
        g.draw(getLine(pxL[0], pxL[1], pxR[0], pxR[1]));

        // Label at right edge (use cached glyph rendering via BaseRenderer)
        final float labelX = (float) (pxR[0] + ChartScale.scale(5));
        final float labelY = (float) pxR[1];

        // optional background chip (no FontMetrics) - fixed paddings; works well for short labels.
        final int chipW = Math.round(ChartScale.scale(26));
        final int chipH = Math.round(ChartScale.scale(14));
        labelBounds.setBounds((int) labelX - 2, (int) (labelY - chipH * 0.6f), chipW, chipH);
        g.setColor(ColorUtils.withAlpha(resolveTheme(context).getBackground(), 0.80f));
        g.fill(labelBounds);

        drawLabel(g, label, labelFont, color, labelX, labelY + ChartScale.scale(4));
    }

    private void ensureCache(ChartModel model) {
        final int n = model.getPointCount();
        if (n < 2) {
            cachedModel = model;
            cachedPointCount = 0;
            cachedPivots = null;
            return;
        }

        if (cachedModel == model && cachedPointCount == n && cachedPivots != null) {
            return;
        }

        cachedModel = model;
        cachedPointCount = n;
        cachedPivots = calculatePivotPoints(model, n);
    }

    private PivotLevels calculatePivotPoints(ChartModel model, int n) {
        // Find high, low, close from the data
        double high = Double.NEGATIVE_INFINITY;
        double low = Double.POSITIVE_INFINITY;
        double close = model.getY(n - 1);

        for (int i = 0; i < n; i++) {
            double max = model.getMax(i);
            double min = model.getMin(i);
            double y = model.getY(i);
            double v = (max != 0.0) ? max : y;
            high = Math.max(high, v);
            low = Math.min(low, (min != 0.0) ? min : y);
        }

        return switch (method) {
            case STANDARD -> calculateStandardPivots(high, low, close);
            case FIBONACCI -> calculateFibonacciPivots(high, low, close);
            case WOODIE -> calculateWoodiePivots(high, low, close, model.getY(0));
            case CAMARILLA -> calculateCamarillaPivots(high, low, close);
        };
    }

    private PivotLevels calculateStandardPivots(double high, double low, double close) {
        double pp = (high + low + close) / 3.0;
        double range = high - low;

        return new PivotLevels(
                pp,
                2 * pp - low,           // R1
                pp + range,             // R2
                high + 2 * (pp - low),  // R3
                2 * pp - high,          // S1
                pp - range,             // S2
                low - 2 * (high - pp)   // S3
        );
    }

    private PivotLevels calculateFibonacciPivots(double high, double low, double close) {
        double pp = (high + low + close) / 3.0;
        double range = high - low;

        return new PivotLevels(
                pp,
                pp + 0.382 * range,     // R1 (38.2%)
                pp + 0.618 * range,     // R2 (61.8%)
                pp + range,             // R3 (100%)
                pp - 0.382 * range,     // S1
                pp - 0.618 * range,     // S2
                pp - range              // S3
        );
    }

    private PivotLevels calculateWoodiePivots(double high, double low, double close, double ignoredOpen) {
        double pp = (high + low + 2 * close) / 4.0;
        double range = high - low;

        return new PivotLevels(
                pp,
                2 * pp - low,
                pp + range,
                high + 2 * (pp - low),
                2 * pp - high,
                pp - range,
                low - 2 * (high - pp)
        );
    }

    private PivotLevels calculateCamarillaPivots(double high, double low, double close) {
        double range = high - low;

        return new PivotLevels(
                close,                              // PP = close
                close + range * 1.1 / 12,           // R1
                close + range * 1.1 / 6,            // R2
                close + range * 1.1 / 4,            // R3
                close - range * 1.1 / 12,           // S1
                close - range * 1.1 / 6,            // S2
                close - range * 1.1 / 4             // S3
        );
    }

    @SuppressWarnings("unused")
    public PivotMethod getPivotMethod() {
        return method;
    }

    @SuppressWarnings("unused")
    public void setPivotMethod(PivotMethod method) {
        this.method = method;
    }

    /**
     * Supported pivot point calculation methods.
     */
    public enum PivotMethod {
        /** Classic pivot points. */
        STANDARD,
        /** Fibonacci-based pivots. */
        FIBONACCI,
        /** Woodie pivots. */
        WOODIE,
        /** Camarilla pivots. */
        CAMARILLA
    }

    private record PivotLevels(double pp, double r1, double r2, double r3,
                               double s1, double s2, double s3) {
    }
}
