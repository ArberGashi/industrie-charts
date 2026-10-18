package com.arbergashi.charts.render.standard;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.PredictiveMath;
import com.arbergashi.charts.util.ColorUtils;

/**
 * Draws a predictive extension beyond the latest point.
 * Reuses segment geometry between frames.
 *
 * @since 1.5.0
 * @author Arber Gashi
 */
public final class PredictivePathRenderer extends BaseRenderer {
    public enum Style {
        LINEAR,
        STEP,
        BAR
    }

    private static final String KEY_LOOKAHEAD = "Chart.predictive.global.lookahead";
    private static final String KEY_SMOOTHING = "Chart.predictive.global.smoothing";
    private static final String KEY_ALPHA = "Chart.predictive.lineAlpha";

    private double smoothedDx = 0.0;
    private double smoothedDy = 0.0;
    private final float[] lineX = new float[3];
    private final float[] lineY = new float[3];

    private final Style style;

    public PredictivePathRenderer() {
        this(Style.LINEAR);
    }

    public PredictivePathRenderer(Style style) {
        super("predictivePath");
        this.style = (style != null) ? style : Style.LINEAR;
    }

    /** Includes the complete extension, including its smoothed endpoint, when fitting the X axis. */
    @Override
    public double[] getPreferredXRange(ChartModel model) {
        return predictionRange(model, true);
    }

    /** Includes the complete extension, including its smoothed endpoint, when fitting the Y axis. */
    @Override
    public double[] getPreferredYRange(ChartModel model) {
        return predictionRange(model, false);
    }

    private double[] predictionRange(ChartModel model, boolean xAxis) {
        if (model == null || model.isEmpty()) return null;
        double[] data = model.getDataRange();
        double min = data[xAxis ? 0 : 2];
        double max = data[xAxis ? 1 : 3];
        int last = model.getPointCount() - 1;
        if (last >= 1) {
            double value = xAxis ? model.getX(last) : model.getY(last);
            double previous = xAxis ? model.getX(last - 1) : model.getY(last - 1);
            int lookahead = ChartAssets.getInt(KEY_LOOKAHEAD, 32);
            double currentEndpoint = PredictiveMath.extrapolate(value, xAxis ? smoothedDx : smoothedDy, lookahead);
            double finalEndpoint = PredictiveMath.extrapolate(value, value - previous, lookahead);
            for (double endpoint : new double[]{currentEndpoint, finalEndpoint}) {
                if (!Double.isFinite(endpoint)) continue;
                double halfWidth = xAxis && style == Style.BAR ? Math.abs(endpoint - value) * 0.35 : 0.0;
                double low = endpoint - halfWidth;
                double high = endpoint + halfWidth;
                if (Double.isFinite(low) && Double.isFinite(high)) {
                    min = Math.min(min, low);
                    max = Math.max(max, high);
                }
            }
        }
        if (!xAxis && style == Style.BAR) {
            min = Math.min(min, 0.0);
            max = Math.max(max, 0.0);
        }
        return new double[]{min, max};
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < 2) return;

        double x1 = model.getX(n - 1);
        double y1 = model.getY(n - 1);
        double x0 = model.getX(n - 2);
        double y0 = model.getY(n - 2);
        if (!Double.isFinite(x0) || !Double.isFinite(y0)
                || !Double.isFinite(x1) || !Double.isFinite(y1)) return;

        double smoothing = ChartAssets.getFloat(KEY_SMOOTHING, 0.85f);
        smoothedDx = PredictiveMath.smoothDelta(smoothedDx, x1 - x0, smoothing);
        smoothedDy = PredictiveMath.smoothDelta(smoothedDy, y1 - y0, smoothing);

        int lookahead = ChartAssets.getInt(KEY_LOOKAHEAD, 32);
        double x2 = PredictiveMath.extrapolate(x1, smoothedDx, lookahead);
        double y2 = PredictiveMath.extrapolate(y1, smoothedDy, lookahead);
        if (!Double.isFinite(x2) || !Double.isFinite(y2)) return;

        double[] p0 = pBuffer();
        double[] p1 = pBuffer4();
        context.mapToPixel(x1, y1, p0);
        context.mapToPixel(x2, y2, p1);

        ArberColor base = getSeriesColor(model);
        float baseAlpha = ChartAssets.getFloat(KEY_ALPHA, 0.26f);
        canvas.setColor(ColorUtils.applyAlpha(base, baseAlpha));
        canvas.setStroke(ChartScale.scale(1.4f));

        if (style == Style.BAR) {
            drawBarGhost(canvas, context, x1, y1, x2, y2);
            return;
        }
        if (style == Style.STEP) {
            drawStepGhost(canvas, p0[0], p0[1], p1[0], p1[1]);
            return;
        }
        drawLinearGhost(canvas, p0[0], p0[1], p1[0], p1[1]);
    }

    private void drawLinearGhost(ArberCanvas canvas, double x0, double y0, double x1, double y1) {
        lineX[0] = (float) x0;
        lineX[1] = (float) x1;
        lineY[0] = (float) y0;
        lineY[1] = (float) y1;
        canvas.drawPolyline(lineX, lineY, 2);
    }

    private void drawStepGhost(ArberCanvas canvas, double x0, double y0, double x1, double y1) {
        lineX[0] = (float) x0;
        lineX[1] = (float) x1;
        lineX[2] = (float) x1;
        lineY[0] = (float) y0;
        lineY[1] = (float) y0;
        lineY[2] = (float) y1;
        canvas.drawPolyline(lineX, lineY, 3);
    }

    private void drawBarGhost(ArberCanvas canvas, PlotContext context, double x0, double y0, double x1, double y1) {
        double width = Math.max(1e-9, Math.abs(x1 - x0)) * 0.7;
        double baseValue = 0.0;
        if (baseValue < context.getMinY() || baseValue > context.getMaxY()) {
            baseValue = context.getMinY();
        }

        double[] pb0 = pBuffer();
        double[] pb1 = pBuffer4();
        context.mapToPixel(x1 - width * 0.5, baseValue, pb0);
        context.mapToPixel(x1 + width * 0.5, y1, pb1);

        double rx = Math.min(pb0[0], pb1[0]);
        double ry = Math.min(pb0[1], pb1[1]);
        double rw = Math.abs(pb1[0] - pb0[0]);
        double rh = Math.abs(pb1[1] - pb0[1]);
        if (!Double.isFinite(rx) || !Double.isFinite(ry)
                || !Double.isFinite(rw) || !Double.isFinite(rh) || rw <= 0 || rh <= 0) return;

        canvas.fillRect((float) rx, (float) ry, (float) rw, (float) rh);
    }
}
