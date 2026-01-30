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
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
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

    private final Style style;

    public PredictivePathRenderer() {
        this(Style.LINEAR);
    }

    public PredictivePathRenderer(Style style) {
        super("predictivePath");
        this.style = (style != null) ? style : Style.LINEAR;
    }

    /**
     * @since 1.5.0
     */
    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        final int n0 = model.getPointCount();
        if (n0 < 2) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();
        final int n = Math.min(n0, Math.min(xData.length, yData.length));
        if (n < 2) return;

        double x1 = xData[n - 1];
        double y1 = yData[n - 1];
        double x0 = xData[n - 2];
        double y0 = yData[n - 2];

        double smoothing = ChartAssets.getFloat(KEY_SMOOTHING, 0.85f);
        smoothedDx = PredictiveMath.smoothDelta(smoothedDx, x1 - x0, smoothing);
        smoothedDy = PredictiveMath.smoothDelta(smoothedDy, y1 - y0, smoothing);

        int lookahead = ChartAssets.getInt(KEY_LOOKAHEAD, 32);
        double x2 = PredictiveMath.extrapolate(x1, smoothedDx, lookahead);
        double y2 = PredictiveMath.extrapolate(y1, smoothedDy, lookahead);

        double[] p0 = pBuffer();
        double[] p1 = pBuffer4();
        context.mapToPixel(x1, y1, p0);
        context.mapToPixel(x2, y2, p1);

        ArberColor base = getSeriesColor(model);
        float baseAlpha = ChartAssets.getFloat(KEY_ALPHA, 0.26f);
        canvas.setColor(ColorUtils.applyAlpha(base, baseAlpha));
        canvas.setStroke(ChartScale.scale(1.4f));

        if (style == Style.BAR) {
            drawBarGhost(canvas, context, p0[0], p0[1], p1[0], p1[1]);
            return;
        }
        if (style == Style.STEP) {
            drawStepGhost(canvas, p0[0], p0[1], p1[0], p1[1]);
            return;
        }
        drawLinearGhost(canvas, p0[0], p0[1], p1[0], p1[1]);
    }

    private void drawLinearGhost(ArberCanvas canvas, double x0, double y0, double x1, double y1) {
        float[] xs = { (float) x0, (float) x1 };
        float[] ys = { (float) y0, (float) y1 };
        canvas.drawPolyline(xs, ys, 2);
    }

    private void drawStepGhost(ArberCanvas canvas, double x0, double y0, double x1, double y1) {
        float[] xs = { (float) x0, (float) x1, (float) x1 };
        float[] ys = { (float) y0, (float) y0, (float) y1 };
        canvas.drawPolyline(xs, ys, 3);
    }

    private void drawBarGhost(ArberCanvas canvas, PlotContext context, double x0, double y0, double x1, double y1) {
        double spacing = Math.max(1e-9, x1 - x0);
        double width = Math.abs(spacing) * 0.7;
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
        if (rw <= 0 || rh <= 0) return;

        canvas.fillRect((float) rx, (float) ry, (float) rw, (float) rh);
    }
}
