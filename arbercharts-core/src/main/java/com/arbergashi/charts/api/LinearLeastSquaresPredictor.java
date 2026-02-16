package com.arbergashi.charts.api;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.util.ChartAssets;
/**
 * Lightweight linear predictor using least squares over a trailing window.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 2.0.0
 */
public final class LinearLeastSquaresPredictor implements PredictionModel {
    private final int windowSize;
    private final int horizonPoints;
    private final double minStep;
    private final double snapStep;
    private final double residualScale;
    private final int decayPoints;

    /**
     * Creates a predictor with sensible defaults.
     */
    public LinearLeastSquaresPredictor() {
        this(
                ChartAssets.getInt("Chart.predictive.windowSize", 48),
                ChartAssets.getInt("Chart.predictive.horizonPoints", 24),
                ChartAssets.getFloat("Chart.predictive.minStep", 1e-6f),
                ChartAssets.getFloat("Chart.predictive.stepSnap", 0.0f),
                ChartAssets.getFloat("Chart.predictive.residualScale", 6.0f),
                ChartAssets.getInt("Chart.predictive.decayPoints", 18)
        );
    }

    public LinearLeastSquaresPredictor(int windowSize, int horizonPoints, double minStep, double snapStep) {
        this(windowSize, horizonPoints, minStep, snapStep, 6.0, 18);
    }

    public LinearLeastSquaresPredictor(int windowSize, int horizonPoints, double minStep, double snapStep,
                                       double residualScale, int decayPoints) {
        this.windowSize = Math.max(4, windowSize);
        this.horizonPoints = Math.max(2, horizonPoints);
        this.minStep = (minStep > 0.0 && Double.isFinite(minStep)) ? minStep : 1e-6;
        this.snapStep = (snapStep > 0.0 && Double.isFinite(snapStep)) ? snapStep : 0.0;
        this.residualScale = (residualScale > 0.0 && Double.isFinite(residualScale)) ? residualScale : 6.0;
        this.decayPoints = Math.max(2, decayPoints);
    }

    @Override
    public void predict(ChartModel model, PlotContext context, PredictionBuffer out) {
        if (model == null || out == null) return;
        int count = model.getPointCount();
        if (count < 2) {
            out.setCount(0);
            return;
        }

        int collected = 0;
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXX = 0.0;
        double sumXY = 0.0;
        double lastX = Double.NaN;
        double lastY = Double.NaN;
        double prevX = Double.NaN;
        double stepSum = 0.0;
        int stepCount = 0;

        for (int i = count - 1; i >= 0 && collected < windowSize; i--) {
            double x = model.getX(i);
            double y = model.getY(i);
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;

            if (!Double.isFinite(lastX)) {
                lastX = x;
                lastY = y;
            }
            if (Double.isFinite(prevX)) {
                double dx = Math.abs(prevX - x);
                if (dx > minStep) {
                    stepSum += dx;
                    stepCount++;
                }
            }
            prevX = x;

            sumX += x;
            sumY += y;
            sumXX += x * x;
            sumXY += x * y;
            collected++;
        }

        if (collected < 2 || !Double.isFinite(lastX)) {
            out.setCount(0);
            return;
        }

        double denom = collected * sumXX - sumX * sumX;
        double slope;
        double intercept;
        if (Math.abs(denom) < 1e-12) {
            slope = 0.0;
            intercept = lastY;
        } else {
            slope = (collected * sumXY - sumX * sumY) / denom;
            intercept = (sumY - slope * sumX) / collected;
        }

        double step = (stepCount > 0) ? (stepSum / stepCount) : minStep;
        if (!Double.isFinite(step) || step < minStep) step = minStep;
        if (snapStep > 0.0) {
            step = Math.max(minStep, Math.round(step / snapStep) * snapStep);
        }

        out.ensureCapacity(horizonPoints);
        double[] xs = out.x();
        double[] ys = out.y();
        double[] confidence = out.confidence();
        double residualStd = getComputedResidualStd(model, count, slope, intercept);
        double baseConfidence = 1.0 / (1.0 + residualStd * residualScale);
        out.setResidualStd(residualStd);
        out.setResidualScale(residualScale);
        for (int i = 0; i < horizonPoints; i++) {
            double x = lastX + step * (i + 1);
            xs[i] = x;
            ys[i] = slope * x + intercept;
            double decay = Math.exp(-(double) i / decayPoints);
            confidence[i] = clamp01(baseConfidence * decay);
        }
        out.setCount(horizonPoints);
    }

    private double getComputedResidualStd(ChartModel model, int count, double slope, double intercept) {
        int collected = 0;
        double sse = 0.0;
        for (int i = count - 1; i >= 0 && collected < windowSize; i--) {
            double x = model.getX(i);
            double y = model.getY(i);
            if (!Double.isFinite(x) || !Double.isFinite(y)) continue;
            double fit = slope * x + intercept;
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
}
