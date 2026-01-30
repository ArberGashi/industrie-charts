package com.arbergashi.charts.render.predictive;

import com.arbergashi.charts.api.DefaultPredictionBuffer;
import com.arbergashi.charts.api.HarmonicOscillatorPredictor;
import com.arbergashi.charts.api.LinearLeastSquaresPredictor;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.PredictionModel;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Renders a lightweight predictive "shadow" ahead of the latest data.
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class PredictiveShadowRenderer extends BaseRenderer {
    private static final String KEY_ENABLED = "Chart.predictive.enabled";
    private static final String KEY_COLOR = "Chart.predictive.color";
    private static final String KEY_LINE_ALPHA = "Chart.predictive.lineAlpha";
    private static final String KEY_STROKE = "Chart.predictive.strokeWidth";
    private static final String KEY_DASH_ON = "Chart.predictive.dashOn";
    private static final String KEY_DASH_OFF = "Chart.predictive.dashOff";
    private static final String KEY_CONF_HIGH_COLOR = "Chart.predictive.confidence.highColor";
    private static final String KEY_CONF_MID_COLOR = "Chart.predictive.confidence.midColor";
    private static final String KEY_CONF_LOW_COLOR = "Chart.predictive.confidence.lowColor";
    private static final String KEY_CONF_HIGH_THRESHOLD = "Chart.predictive.confidence.highThreshold";
    private static final String KEY_CONF_MID_THRESHOLD = "Chart.predictive.confidence.midThreshold";
    private static final String KEY_DELTA_ENABLED = "Chart.predictive.delta.enabled";
    private static final String KEY_DELTA_INTERVAL = "Chart.predictive.delta.interval";
    private static final String KEY_DELTA_TOLERANCE = "Chart.predictive.delta.tolerance";
    private static final String KEY_MODE = "Chart.predictive.mode";

    private static final ArberColor DEFAULT_HIGH = ColorRegistry.of(52, 211, 153, 255);
    private static final ArberColor DEFAULT_MID = ColorRegistry.of(251, 191, 36, 255);
    private static final ArberColor DEFAULT_LOW = ColorRegistry.of(248, 113, 113, 255);

    private static final Logger LOG = Logger.getLogger(PredictiveShadowRenderer.class.getName());

    private final PredictionModel predictor;
    private final DefaultPredictionBuffer buffer = new DefaultPredictionBuffer();
    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];
    private double[] auditXs = new double[0];
    private double[] auditYs = new double[0];
    private double[] auditConfidence = new double[0];
    private int auditCount;
    private double auditStartX;
    private double auditStep = 1.0;
    private int auditActualIndex = -1;
    private double deltaSum;
    private double deltaAbsSum;
    private double deltaSqSum;
    private int deltaSamples;
    private long lastPredictStamp = Long.MIN_VALUE;
    private double lastResidualStd;
    private double lastResidualScale = 1.0;

    public PredictiveShadowRenderer() {
        this(getDefaultPredictor());
    }

    public PredictiveShadowRenderer(PredictionModel predictor) {
        super("predictive_shadow");
        this.predictor = (predictor != null) ? predictor : new LinearLeastSquaresPredictor();
    }

    @Override
    public String getName() {
        return "Predictive Shadow";
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    /**
     * Ensures the prediction buffer and audit mapping are up to date for the model stamp.
     */
    public void ensurePredicted(ChartModel model, PlotContext context) {
        if (model == null || context == null) return;
        long stamp = model.getUpdateStamp();
        if (stamp == lastPredictStamp && buffer.count() > 0) return;
        if (!ChartAssets.getBoolean(KEY_ENABLED, true)) return;
        evaluateDeltaAudit(model);
        predictor.predict(model, context, buffer);
        lastResidualStd = buffer.residualStd();
        lastResidualScale = buffer.residualScale();
        int last = lastFiniteIndex(model);
        if (last >= 0) {
            double lastX = model.getX(last);
            setDeltaAudit(model, lastX, buffer.x(), buffer.y(), buffer.confidence(), buffer.count());
        }
        lastPredictStamp = stamp;
    }

    /**
     * Returns the predicted y-value for a given x if it is within the audit horizon.
     */
    public double predictedForX(double x) {
        int idx = auditIndexForX(x);
        return (idx >= 0) ? auditYs[idx] : Double.NaN;
    }

    /**
     * Returns the prediction confidence for a given x if available.
     */
    public double confidenceForX(double x) {
        int idx = auditIndexForX(x);
        return (idx >= 0) ? auditConfidence[idx] : Double.NaN;
    }

    public double residualStd() {
        return lastResidualStd;
    }

    public double residualScale() {
        return lastResidualScale;
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!ChartAssets.getBoolean(KEY_ENABLED, true)) return;
        ensurePredicted(model, context);
        int last = lastFiniteIndex(model);
        if (last < 1) return;
        int n = buffer.count();
        if (n < 1) return;

        double[] px = pBuffer();
        double lastX = model.getX(last);
        double lastY = model.getY(last);
        if (!Double.isFinite(lastX) || !Double.isFinite(lastY)) return;

        context.mapToPixel(lastX, lastY, px);
        double prevPx = context.snapPixel(px[0]);
        double prevPy = context.snapPixel(px[1]);

        double[] xs = buffer.x();
        double[] ys = buffer.y();
        double[] confidence = buffer.confidence();

        float strokeWidth = ChartScale.scale(ChartAssets.getFloat(KEY_STROKE, 1.4f));
        canvas.setStroke(strokeWidth);

        ArberColor base = getResolvedBaseColor(context, model);
        float alpha = ChartAssets.getFloat(KEY_LINE_ALPHA, 0.26f);
        ArberColor high = getResolvedColor(KEY_CONF_HIGH_COLOR, base, DEFAULT_HIGH);
        ArberColor mid = getResolvedColor(KEY_CONF_MID_COLOR, DEFAULT_MID, DEFAULT_MID);
        ArberColor low = getResolvedColor(KEY_CONF_LOW_COLOR, DEFAULT_LOW, DEFAULT_LOW);
        ArberColor highAlpha = ColorRegistry.applyAlpha(high, alpha);
        ArberColor midAlpha = ColorRegistry.applyAlpha(mid, alpha);
        ArberColor lowAlpha = ColorRegistry.applyAlpha(low, alpha);
        double highT = clamp01(ChartAssets.getFloat(KEY_CONF_HIGH_THRESHOLD, 0.66f));
        double midT = clamp01(ChartAssets.getFloat(KEY_CONF_MID_THRESHOLD, 0.33f));

        for (int i = 0; i < n; i++) {
            double x = xs[i];
            double y = ys[i];
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
            context.mapToPixel(x, y, px);
            double sx = context.snapPixel(px[0]);
            double sy = context.snapPixel(px[1]);

            double conf = (confidence != null && i < confidence.length) ? confidence[i] : 0.0;
            canvas.setColor(getResolvedConfidenceColor(conf, midT, highT, highAlpha, midAlpha, lowAlpha));
            lineX[0] = (float) prevPx;
            lineX[1] = (float) sx;
            lineY[0] = (float) prevPy;
            lineY[1] = (float) sy;
            canvas.drawPolyline(lineX, lineY, 2);
            prevPx = sx;
            prevPy = sy;
        }

    }

    private ArberColor getResolvedColor(String key, ArberColor preferred, ArberColor fallback) {
        ArberColor configured = ChartAssets.getColor(key, null);
        if (configured != null) return configured;
        return (preferred != null) ? preferred : fallback;
    }

    private ArberColor getResolvedConfidenceColor(double confidence, double midT, double highT,
                                         ArberColor high, ArberColor mid, ArberColor low) {
        if (confidence >= highT) return high;
        if (confidence >= midT) return mid;
        return low;
    }

    private ArberColor getResolvedBaseColor(PlotContext context, ChartModel model) {
        ArberColor configured = ChartAssets.getColor(KEY_COLOR, null);
        if (configured != null) return configured;
        if (context.getTheme() != null && context.getTheme().getAccentColor() != null) {
            return context.getTheme().getAccentColor();
        }
        return getSeriesColor(model);
    }

    private int lastFiniteIndex(ChartModel model) {
        for (int i = model.getPointCount() - 1; i >= 0; i--) {
            double x = model.getX(i);
            double y = model.getY(i);
            if (Double.isFinite(x) && Double.isFinite(y)) {
                return i;
            }
        }
        return -1;
    }

    private void setDeltaAudit(ChartModel model, double startX, double[] xs, double[] ys,
                                  double[] confidence, int count) {
        if (count <= 0 || xs == null || ys == null) return;
        ensureAuditCapacity(count);
        System.arraycopy(xs, 0, auditXs, 0, count);
        System.arraycopy(ys, 0, auditYs, 0, count);
        if (confidence != null) {
            System.arraycopy(confidence, 0, auditConfidence, 0, Math.min(count, confidence.length));
        }
        auditCount = count;
        auditStartX = startX;
        auditStep = (count > 0) ? (auditXs[0] - startX) : 1.0;
        if (!Double.isFinite(auditStep) || Math.abs(auditStep) < 1e-12) {
            auditStep = 1.0;
        }
        auditActualIndex = Math.max(auditActualIndex, model.getPointCount() - 1);
    }

    private void evaluateDeltaAudit(ChartModel model) {
        if (!ChartAssets.getBoolean(KEY_DELTA_ENABLED, false)) return;
        if (auditCount <= 0 || auditActualIndex >= model.getPointCount() - 1) return;

        double stepAbs = Math.abs(auditStep);
        double toleranceFactor = Math.max(0.05, ChartAssets.getFloat(KEY_DELTA_TOLERANCE, 0.35f));
        double tolerance = stepAbs * toleranceFactor;
        if (!Double.isFinite(tolerance) || tolerance <= 0.0) tolerance = stepAbs;

        int end = model.getPointCount() - 1;
        for (int i = Math.max(0, auditActualIndex + 1); i <= end; i++) {
            double x = model.getX(i);
            double y = model.getY(i);
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
            if ((auditStep > 0 && x <= auditStartX) || (auditStep < 0 && x >= auditStartX)) continue;

            int idx = (int) Math.round((x - auditStartX) / auditStep) - 1;
            if (idx < 0 || idx >= auditCount) continue;
            double predictedX = auditStartX + auditStep * (idx + 1);
            if (Math.abs(predictedX - x) > tolerance) continue;

            double delta = y - auditYs[idx];
            deltaSum += delta;
            deltaAbsSum += Math.abs(delta);
            deltaSqSum += delta * delta;
            deltaSamples++;
        }
        auditActualIndex = end;
        maybeLogDelta(model.getUpdateStamp());
    }

    private int auditIndexForX(double x) {
        if (auditCount <= 0 || !Double.isFinite(x)) return -1;
        if ((auditStep > 0 && x <= auditStartX) || (auditStep < 0 && x >= auditStartX)) return -1;
        double stepAbs = Math.abs(auditStep);
        if (stepAbs < 1e-12) return -1;
        double toleranceFactor = Math.max(0.05, ChartAssets.getFloat(KEY_DELTA_TOLERANCE, 0.35f));
        double tolerance = stepAbs * toleranceFactor;
        int idx = (int) Math.round((x - auditStartX) / auditStep) - 1;
        if (idx < 0 || idx >= auditCount) return -1;
        double predictedX = auditStartX + auditStep * (idx + 1);
        if (Math.abs(predictedX - x) > tolerance) return -1;
        return idx;
    }

    private void maybeLogDelta(long stamp) {
        int interval = Math.max(10, ChartAssets.getInt(KEY_DELTA_INTERVAL, 120));
        if (deltaSamples < interval) return;
        double mean = deltaSum / deltaSamples;
        double mae = deltaAbsSum / deltaSamples;
        double rmse = Math.sqrt(deltaSqSum / deltaSamples);
        LOG.log(Level.INFO, () -> String.format(
                "Predictive delta stamp=%d samples=%d mean=%.6f mae=%.6f rmse=%.6f",
                stamp, deltaSamples, mean, mae, rmse));
        deltaSum = 0.0;
        deltaAbsSum = 0.0;
        deltaSqSum = 0.0;
        deltaSamples = 0;
    }

    private void ensureAuditCapacity(int capacity) {
        if (auditXs.length >= capacity) return;
        int newCap = 1;
        while (newCap < capacity && newCap > 0) newCap <<= 1;
        if (newCap <= 0) newCap = capacity;
        double[] newXs = new double[newCap];
        double[] newYs = new double[newCap];
        double[] newConf = new double[newCap];
        if (auditCount > 0) {
            System.arraycopy(auditXs, 0, newXs, 0, auditCount);
            System.arraycopy(auditYs, 0, newYs, 0, auditCount);
            System.arraycopy(auditConfidence, 0, newConf, 0, Math.min(auditConfidence.length, auditCount));
        }
        auditXs = newXs;
        auditYs = newYs;
        auditConfidence = newConf;
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static PredictionModel getDefaultPredictor() {
        String mode = ChartAssets.getString(KEY_MODE, "harmonic");
        if (mode != null && mode.equalsIgnoreCase("linear")) {
            return new LinearLeastSquaresPredictor();
        }
        return new HarmonicOscillatorPredictor();
    }
}
