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
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class PivotPointsRenderer extends BaseRenderer {

    private final double[] pxL = new double[2];
    private final double[] pxR = new double[2];
    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];
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

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < 2) return;

        ensureCache(model);
        final PivotLevels pivots = cachedPivots;
        if (pivots == null) return;

        final IndicatorRendererSupport.Viewport vp = IndicatorRendererSupport.viewport(context);

        final ChartTheme theme = getResolvedTheme(context);
        final ArberColor ppColor = theme.getAccentColor();
        final ArberColor r1Color = ColorRegistry.adjustBrightness(theme.getBearishColor(), 1.0f);
        final ArberColor r2Color = ColorRegistry.adjustBrightness(theme.getBearishColor(), 1.2f);
        final ArberColor r3Color = ColorRegistry.adjustBrightness(theme.getBearishColor(), 1.4f);
        final ArberColor s1Color = ColorRegistry.adjustBrightness(theme.getBullishColor(), 1.0f);
        final ArberColor s2Color = ColorRegistry.adjustBrightness(theme.getBullishColor(), 1.2f);
        final ArberColor s3Color = ColorRegistry.adjustBrightness(theme.getBullishColor(), 1.4f);

        final double xLeft = model.getX(0);
        final double xRight = model.getX(n - 1);

        // Draw resistance levels
        drawPivotLevel(canvas, context, xLeft, xRight, pivots.r3, r3Color, 1.0f, vp);
        drawPivotLevel(canvas, context, xLeft, xRight, pivots.r2, r2Color, 1.5f, vp);
        drawPivotLevel(canvas, context, xLeft, xRight, pivots.r1, r1Color, 2.0f, vp);

        // Draw pivot point
        drawPivotLevel(canvas, context, xLeft, xRight, pivots.pp, ppColor, 2.5f, vp);

        // Draw support levels
        drawPivotLevel(canvas, context, xLeft, xRight, pivots.s1, s1Color, 2.0f, vp);
        drawPivotLevel(canvas, context, xLeft, xRight, pivots.s2, s2Color, 1.5f, vp);
        drawPivotLevel(canvas, context, xLeft, xRight, pivots.s3, s3Color, 1.0f, vp);
    }

    private void drawPivotLevel(ArberCanvas canvas,
                                PlotContext context,
                                double xLeft,
                                double xRight,
                                double level,
                                ArberColor color,
                                float width,
                                IndicatorRendererSupport.Viewport vp) {
        context.mapToPixel(xLeft, level, pxL);
        context.mapToPixel(xRight, level, pxR);

        // quick y-clip rejection
        final double y = pxR[1];
        if (y < vp.getY() - 2.0 || y > vp.getMaxY() + 2.0) {
            return;
        }

        // Draw line
        canvas.setColor(color);
        canvas.setStroke(ChartScale.scale(width));
        lineX[0] = (float) pxL[0];
        lineY[0] = (float) pxL[1];
        lineX[1] = (float) pxR[0];
        lineY[1] = (float) pxR[1];
        canvas.drawPolyline(lineX, lineY, 2);
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
        cachedPivots = getCalculatedPivotPoints(model, n);
    }

    private PivotLevels getCalculatedPivotPoints(ChartModel model, int n) {
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
            case STANDARD -> getCalculatedStandardPivots(high, low, close);
            case FIBONACCI -> getCalculatedFibonacciPivots(high, low, close);
            case WOODIE -> getCalculatedWoodiePivots(high, low, close, model.getY(0));
            case CAMARILLA -> getCalculatedCamarillaPivots(high, low, close);
        };
    }

    private PivotLevels getCalculatedStandardPivots(double high, double low, double close) {
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

    private PivotLevels getCalculatedFibonacciPivots(double high, double low, double close) {
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

    private PivotLevels getCalculatedWoodiePivots(double high, double low, double close, double ignoredOpen) {
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

    private PivotLevels getCalculatedCamarillaPivots(double high, double low, double close) {
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
    public PivotPointsRenderer setPivotMethod(PivotMethod method) {
        this.method = method;
        return this;
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

    private static final class PivotLevels {
        private double pp;
        private double r1;
        private double r2;
        private double r3;
        private double s1;
        private double s2;
        private double s3;

        private PivotLevels(double pp, double r1, double r2, double r3,
                            double s1, double s2, double s3) {
            this.pp = pp;
            this.r1 = r1;
            this.r2 = r2;
            this.r3 = r3;
            this.s1 = s1;
            this.s2 = s2;
            this.s3 = s3;
        }

        public double getPp() {
            return pp;
        }

        public double getR1() {
            return r1;
        }

        public double getR2() {
            return r2;
        }

        public double getR3() {
            return r3;
        }

        public double getS1() {
            return s1;
        }

        public double getS2() {
            return s2;
        }

        public double getS3() {
            return s3;
        }
    }
}
