package com.arbergashi.charts.api;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.util.ChartAssets;
/**
 * Phase-aware predictor tuned for oscillating signals (e.g., ECG-like waves).
 *
 * <p>It estimates a dominant period via positive-slope zero crossings and
 * extrapolates with {@code A * sin(w * (x - x0)) + C}. When insufficient
 * crossings are available, it falls back to the linear predictor.</p>
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class HarmonicOscillatorPredictor implements PredictionModel {
    private final int windowSize;
    private final int horizonPoints;
    private final double minStep;
    private final double snapStep;
    private final double residualScale;
    private final int decayPoints;
    private final double minPeriod;
    private final double maxPeriod;
    private final double periodAlpha;
    private final double amplitudeAlpha;
    private final double stabilityScale;
    private final double resetThreshold;
    private final int fastWindowSize;
    private final double fastAlpha;
    private final int resetCooldownTicks;
    private final LinearLeastSquaresPredictor fallback;
    private final MicroWindowPeriodFinder fastFinder;

    public HarmonicOscillatorPredictor() {
        this(
                ChartAssets.getInt("Chart.predictive.windowSize", 96),
                ChartAssets.getInt("Chart.predictive.horizonPoints", 24),
                ChartAssets.getFloat("Chart.predictive.minStep", 1e-6f),
                ChartAssets.getFloat("Chart.predictive.stepSnap", 0.0f),
                ChartAssets.getFloat("Chart.predictive.residualScale", 6.0f),
                ChartAssets.getInt("Chart.predictive.decayPoints", 18),
                ChartAssets.getFloat("Chart.predictive.harmonic.minPeriod", 0.2f),
                ChartAssets.getFloat("Chart.predictive.harmonic.maxPeriod", 20.0f),
                ChartAssets.getFloat("Chart.predictive.harmonic.periodAlpha", 0.80f),
                ChartAssets.getFloat("Chart.predictive.harmonic.amplitudeAlpha", 0.85f),
                ChartAssets.getFloat("Chart.predictive.harmonic.stabilityScale", 7.0f),
                ChartAssets.getFloat("Chart.predictive.harmonic.resetThreshold", 0.25f),
                ChartAssets.getInt("Chart.predictive.harmonic.fastWindowSize", 12),
                ChartAssets.getFloat("Chart.predictive.harmonic.fastAlpha", 0.95f),
                ChartAssets.getInt("Chart.predictive.harmonic.resetCooldownTicks", 12)
        );
    }

    public HarmonicOscillatorPredictor(int windowSize, int horizonPoints, double minStep, double snapStep,
                                       double residualScale, int decayPoints, double minPeriod, double maxPeriod,
                                       double periodAlpha, double amplitudeAlpha, double stabilityScale,
                                       double resetThreshold, int fastWindowSize, double fastAlpha,
                                       int resetCooldownTicks) {
        this.windowSize = Math.max(8, windowSize);
        this.horizonPoints = Math.max(2, horizonPoints);
        this.minStep = (minStep > 0.0 && Double.isFinite(minStep)) ? minStep : 1e-6;
        this.snapStep = (snapStep > 0.0 && Double.isFinite(snapStep)) ? snapStep : 0.0;
        this.residualScale = (residualScale > 0.0 && Double.isFinite(residualScale)) ? residualScale : 6.0;
        this.decayPoints = Math.max(2, decayPoints);
        double minP = (minPeriod > 0.0 && Double.isFinite(minPeriod)) ? minPeriod : 0.2;
        double maxP = (maxPeriod > minP && Double.isFinite(maxPeriod)) ? maxPeriod : 20.0;
        this.minPeriod = minP;
        this.maxPeriod = maxP;
        double pAlpha = (periodAlpha > 0.05 && periodAlpha < 1.0 && Double.isFinite(periodAlpha)) ? periodAlpha : 0.80;
        double aAlpha = (amplitudeAlpha > 0.05 && amplitudeAlpha < 1.0 && Double.isFinite(amplitudeAlpha))
                ? amplitudeAlpha : 0.85;
        this.periodAlpha = pAlpha;
        this.amplitudeAlpha = aAlpha;
        this.stabilityScale = (stabilityScale > 0.0 && Double.isFinite(stabilityScale)) ? stabilityScale : 6.0;
        double rThresh = (resetThreshold > 0.05 && Double.isFinite(resetThreshold)) ? resetThreshold : 0.25;
        this.resetThreshold = rThresh;
        this.fastWindowSize = Math.max(6, fastWindowSize);
        double fAlpha = (fastAlpha > 0.5 && fastAlpha < 1.0 && Double.isFinite(fastAlpha)) ? fastAlpha : 0.95;
        this.fastAlpha = fAlpha;
        this.resetCooldownTicks = Math.max(4, resetCooldownTicks);
        this.fallback = new LinearLeastSquaresPredictor(windowSize, horizonPoints, this.minStep, this.snapStep,
                this.residualScale, this.decayPoints);
        this.fastFinder = new MicroWindowPeriodFinder(this.fastWindowSize, this.minPeriod, this.maxPeriod);
    }

    @Override
    public void predict(ChartModel model, PlotContext context, PredictionBuffer out) {
        if (model == null || out == null) return;
        int count = model.getPointCount();
        if (count < 4) {
            out.setCount(0);
            return;
        }

        // Scan a bounded trailing window to avoid O(n) on very large models.
        int start = Math.max(0, count - windowSize);
        int collected = 0;
        double lastX = Double.NaN;
        double lastY = Double.NaN;
        double prevX = Double.NaN;
        double prevY = Double.NaN;
        double stepSum = 0.0;
        int stepCount = 0;

        double mean = 0.0;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double sumSqCentered = 0.0;

        double prevCross = Double.NaN;
        double lastCross = Double.NaN;
        boolean lastCrossPositiveSlope = true;
        double periodEma = Double.NaN;
        double periodVarEma = 0.0;
        int cooldownRemaining = 0;
        int crossingCount = 0;

        for (int i = start; i < count; i++) {
            double x = model.getX(i);
            double y = model.getY(i);
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;

            if (Double.isFinite(prevX)) {
                double dx = x - prevX;
                if (Math.abs(dx) > minStep) {
                    stepSum += Math.abs(dx);
                    stepCount++;
                }
                // Positive-slope zero crossing with linear interpolation.
                if (prevY <= 0.0 && y > 0.0) {
                    double denom = y - prevY;
                    if (Math.abs(denom) > 1e-12) {
                        double t = -prevY / denom;
                        double crossX = prevX + dx * t;
                        prevCross = lastCross;
                        lastCross = crossX;
                        lastCrossPositiveSlope = denom > 0.0;
                        crossingCount++;
                        if (Double.isFinite(prevCross)) {
                            double period = Math.abs(lastCross - prevCross);
                            if (Double.isFinite(period) && period >= minPeriod && period <= maxPeriod) {
                                if (!Double.isFinite(periodEma)) {
                                    periodEma = period;
                                } else {
                                    double ratio = Math.abs(period - periodEma) / Math.max(minPeriod, periodEma);
                                    if (ratio > resetThreshold) {
                                        periodEma = period;
                                        periodVarEma = 0.0;
                                        cooldownRemaining = resetCooldownTicks;
                                    } else {
                                        double alpha = (cooldownRemaining > 0) ? fastAlpha : periodAlpha;
                                        double delta = period - periodEma;
                                        periodEma += alpha * delta;
                                        periodVarEma = (1.0 - alpha) * periodVarEma + alpha * Math.abs(delta);
                                        if (cooldownRemaining > 0) cooldownRemaining--;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            prevX = x;
            prevY = y;
            lastX = x;
            lastY = y;
            mean += y;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
            // Center later by subtracting the running mean approximation.
            sumSqCentered += y * y;
            collected++;
        }

        if (collected < 8 || !Double.isFinite(lastX) || !Double.isFinite(lastY)) {
            out.setCount(0);
            return;
        }

        mean /= collected;
        double slopeSign = 0.0;
        if (Double.isFinite(prevX) && Math.abs(lastX - prevX) > minStep) {
            slopeSign = Math.signum(lastY - prevY);
        }
        double amplitudeMinMax = (maxY - minY) * 0.5;
        double variance = Math.max(0.0, (sumSqCentered / collected) - mean * mean);
        double amplitudeRms = Math.sqrt(variance) * Math.sqrt(2.0);
        double amplitude = amplitudeAlpha * amplitudeRms + (1.0 - amplitudeAlpha) * amplitudeMinMax;
        if (!Double.isFinite(amplitude) || amplitude < 1e-9) {
            fallback.predict(model, context, out);
            return;
        }

        MicroWindowPeriodFinder.Result fastResult = fastFinder.find(model, count);
        boolean resetTriggered = false;
        if (fastResult.isPeriodAvailable()) {
            double fastPeriod = fastResult.getPeriod();
            if (!Double.isFinite(periodEma)) {
                periodEma = fastPeriod;
            } else {
                double ratio = Math.abs(fastPeriod - periodEma) / Math.max(minPeriod, periodEma);
                if (ratio > resetThreshold) {
                    periodEma = fastPeriod;
                    periodVarEma = 0.0;
                    cooldownRemaining = resetCooldownTicks;
                    resetTriggered = true;
                }
            }
        }

        double period = Double.NaN;
        if (Double.isFinite(periodEma)) period = periodEma;
        else if (Double.isFinite(prevCross) && Double.isFinite(lastCross)) period = Math.abs(lastCross - prevCross);
        if (!Double.isFinite(period) || period < minPeriod || period > maxPeriod) {
            fallback.predict(model, context, out);
            return;
        }

        double step = (stepCount > 0) ? (stepSum / stepCount) : minStep;
        if (!Double.isFinite(step) || step < minStep) step = minStep;
        if (snapStep > 0.0) {
            step = Math.max(minStep, Math.round(step / snapStep) * snapStep);
        }

        double omega = (Math.PI * 2.0) / period;
        double anchorX = lastCross;
        if (resetTriggered && fastResult.isPeriodAvailable() && Double.isFinite(fastResult.getAnchorX())) {
            // Only trust the micro-window anchor during a confirmed regime reset.
            anchorX = fastResult.getAnchorX();
        }
        if (!Double.isFinite(anchorX)) anchorX = lastX - period;
        double phaseOffset = lastCrossPositiveSlope ? 0.0 : Math.PI;
        if (resetTriggered) {
            phaseOffset = getComputedPhaseLock(mean, amplitude, lastX, lastY, slopeSign, omega, anchorX,
                    fastResult.isPositiveSlopeCrossing());
        }

        out.ensureCapacity(horizonPoints);
        double[] xs = out.x();
        double[] ys = out.y();
        double[] confidence = out.confidence();

        double residualStd = getComputedResidualStd(model, start, count, omega, anchorX, phaseOffset, amplitude, mean);
        double baseConfidence = 1.0 / (1.0 + residualStd * residualScale);
        // Penalize confidence while the period estimate is still jittery.
        if (Double.isFinite(periodVarEma) && period > minPeriod) {
            double jitterRatio = periodVarEma / period;
            baseConfidence *= Math.exp(-jitterRatio * stabilityScale);
        }
        if (crossingCount < 3) baseConfidence *= 0.7;
        if (cooldownRemaining > 0) baseConfidence *= 0.75;
        out.setResidualStd(residualStd);
        out.setResidualScale(residualScale);

        for (int i = 0; i < horizonPoints; i++) {
            double x = lastX + step * (i + 1);
            xs[i] = x;
            ys[i] = mean + amplitude * Math.sin(omega * (x - anchorX) + phaseOffset);
            double decay = Math.exp(-(double) i / decayPoints);
            confidence[i] = clamp01(baseConfidence * decay);
        }
        out.setCount(horizonPoints);
    }

    private double getComputedPhaseLock(double mean, double amplitude, double lastX, double lastY, double slopeSign,
                                    double omega, double anchorX, boolean crossingPositiveSlope) {
        if (!Double.isFinite(amplitude) || amplitude < 1e-9) {
            return crossingPositiveSlope ? 0.0 : Math.PI;
        }
        double normalized = clamp((lastY - mean) / amplitude, -1.0, 1.0);
        double base = Math.asin(normalized);
        double alt = Math.PI - base;
        double theta = omega * (lastX - anchorX);
        double phi1 = base - theta;
        double phi2 = alt - theta;
        if (slopeSign > 0.0) return phi1;
        if (slopeSign < 0.0) return phi2;
        return crossingPositiveSlope ? phi1 : phi2;
    }

    private double getComputedResidualStd(ChartModel model, int start, int end, double omega, double anchorX,
                                      double phaseOffset, double amplitude, double mean) {
        int collected = 0;
        double sse = 0.0;
        for (int i = start; i < end; i++) {
            double x = model.getX(i);
            double y = model.getY(i);
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
            double fit = mean + amplitude * Math.sin(omega * (x - anchorX) + phaseOffset);
            double err = y - fit;
            sse += err * err;
            collected++;
        }
        if (collected < 2) return 0.0;
        return Math.sqrt(sse / collected);
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static double clamp(double v, double lo, double hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }
}
