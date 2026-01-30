package com.arbergashi.charts.render.predictive;

import com.arbergashi.charts.api.AnomalyLevel;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * Highlights divergence between the predictive shadow and live data.
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class AnomalyGapRenderer extends BaseRenderer {
    private static final String KEY_ENABLED = "Chart.predictive.anomaly.enabled";
    private static final String KEY_WINDOW = "Chart.predictive.anomaly.windowPoints";
    private static final String KEY_WARN_COLOR = "Chart.predictive.anomaly.warnColor";
    private static final String KEY_CRIT_COLOR = "Chart.predictive.anomaly.criticalColor";
    private static final String KEY_WARN_ALPHA = "Chart.predictive.anomaly.warnAlpha";
    private static final String KEY_CRIT_ALPHA = "Chart.predictive.anomaly.criticalAlpha";
    private static final String KEY_SIGMA = "Chart.predictive.anomaly.sigmaFactor";
    private static final String KEY_TOLERANCE = "Chart.predictive.delta.tolerance";

    private static final ArberColor DEFAULT_WARN = ColorRegistry.of(251, 191, 36, 255);
    private static final ArberColor DEFAULT_CRIT = ColorRegistry.of(248, 113, 113, 255);

    private final PredictiveShadowRenderer shadow;
    private final float[] polyX = new float[4];
    private final float[] polyY = new float[4];

    public AnomalyGapRenderer(PredictiveShadowRenderer shadow) {
        super("predictive_anomaly_gap");
        this.shadow = shadow;
    }

    @Override
    public String getName() {
        return "Predictive Anomaly Gap";
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (shadow == null) return;
        if (!ChartAssets.getBoolean(KEY_ENABLED, true)) return;

        shadow.ensurePredicted(model, context);

        int end = lastFiniteIndex(model);
        if (end < 2) return;
        int window = Math.max(16, ChartAssets.getInt(KEY_WINDOW, 96));
        int start = Math.max(1, end - window);

        double sigma = shadow.residualStd();
        double sigmaFactor = Math.max(0.5, ChartAssets.getFloat(KEY_SIGMA, 2.0f));
        double sigmaLimit = (sigma > 0.0 && Double.isFinite(sigma)) ? sigma * sigmaFactor : Double.POSITIVE_INFINITY;

        double tolFactor = Math.max(0.05, ChartAssets.getFloat(KEY_TOLERANCE, 0.35f));

        ArberColor warn = getResolvedColor(KEY_WARN_COLOR, DEFAULT_WARN, ChartAssets.getFloat(KEY_WARN_ALPHA, 0.18f));
        ArberColor crit = getResolvedColor(KEY_CRIT_COLOR, DEFAULT_CRIT, ChartAssets.getFloat(KEY_CRIT_ALPHA, 0.28f));
        double[] px4 = pBuffer4();

        AnomalyLevel strongest = AnomalyLevel.INFO;
        double strongestDelta = 0.0;
        double strongestX = Double.NaN;
        double strongestActual = Double.NaN;
        double strongestPred = Double.NaN;

        for (int i = start; i <= end; i++) {
            double x0 = model.getX(i - 1);
            double y0 = model.getY(i - 1);
            double x1 = model.getX(i);
            double y1 = model.getY(i);
            if (!finite4(x0, y0, x1, y1)) continue;

            double p0 = shadow.predictedForX(x0);
            double p1 = shadow.predictedForX(x1);
            if (!Double.isFinite(p0) || !Double.isFinite(p1)) continue;

            double dx = Math.abs(x1 - x0);
            if (!Double.isFinite(dx) || dx < 1e-9) dx = 1.0;
            double warnLimit = dx * tolFactor;
            double critLimit = Math.max(warnLimit, sigmaLimit);

            double delta = Math.abs(y1 - p1);
            AnomalyLevel level = (delta >= critLimit) ? AnomalyLevel.CRITICAL
                    : (delta >= warnLimit ? AnomalyLevel.WARN : AnomalyLevel.INFO);
            if (level == AnomalyLevel.INFO) continue;

            if (level.ordinal() > strongest.ordinal() || delta > strongestDelta) {
                strongest = level;
                strongestDelta = delta;
                strongestX = x1;
                strongestActual = y1;
                strongestPred = p1;
            }

            context.mapToPixel(x0, y0, px4);
            double ax0 = context.snapPixel(px4[0]);
            double ay0 = context.snapPixel(px4[1]);
            context.mapToPixel(x0, p0, px4);
            double px0 = context.snapPixel(px4[0]);
            double py0 = context.snapPixel(px4[1]);
            context.mapToPixel(x1, y1, px4);
            double ax1 = context.snapPixel(px4[0]);
            double ay1 = context.snapPixel(px4[1]);
            context.mapToPixel(x1, p1, px4);
            double px1 = context.snapPixel(px4[0]);
            double py1 = context.snapPixel(px4[1]);

            canvas.setColor(level == AnomalyLevel.CRITICAL ? crit : warn);
            polyX[0] = (float) ax0;
            polyY[0] = (float) ay0;
            polyX[1] = (float) ax1;
            polyY[1] = (float) ay1;
            polyX[2] = (float) px1;
            polyY[2] = (float) py1;
            polyX[3] = (float) px0;
            polyY[3] = (float) py0;
            canvas.fillPolygon(polyX, polyY, 4);
        }
    }

    private static boolean finite4(double a, double b, double c, double d) {
        return Double.isFinite(a) && Double.isFinite(b) && Double.isFinite(c) && Double.isFinite(d);
    }

    private int lastFiniteIndex(ChartModel model) {
        for (int i = model.getPointCount() - 1; i >= 0; i--) {
            if (Double.isFinite(model.getX(i)) && Double.isFinite(model.getY(i))) return i;
        }
        return -1;
    }

    private ArberColor getResolvedColor(String key, ArberColor fallback, float alpha) {
        ArberColor configured = ChartAssets.getColor(key, null);
        ArberColor base = (configured != null) ? configured : fallback;
        return ColorRegistry.applyAlpha(base, alpha);
    }
}
