package com.arbergashi.charts.api;

import com.arbergashi.charts.model.DefaultChartModel;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.SplittableRandom;

class PredictiveShadowStressTest {

    private static final double DT = 0.04;

    @Test
    void sinusScenariosReportStability() {
        ScenarioResult clean = runScenario(1.0, 1.0, 0.0);
        ScenarioResult shock = runScenario(1.0, 2.5, 0.0);
        ScenarioResult noisy = runScenario(1.0, 1.0, 0.15);

        System.out.printf("PREDICTIVE clean rmse=%.6f mae=%.6f%n", clean.rmse, clean.mae);
        System.out.printf("PREDICTIVE shock rmse=%.6f mae=%.6f settleTicks=%d%n",
                shock.rmse, shock.mae, shock.settleTicks);
        System.out.printf("PREDICTIVE noisy rmse=%.6f mae=%.6f%n", noisy.rmse, noisy.mae);

        // Optional enforcement gate for local experimentation:
        // mvn -pl arbercharts-core test -Dtest=PredictiveShadowStressTest -Darbercharts.predictive.assert=true
        if (Boolean.getBoolean("arbercharts.predictive.assert")) {
            org.junit.jupiter.api.Assertions.assertTrue(clean.rmse < 1.25, "Clean sinus RMSE too high: " + clean.rmse);
            org.junit.jupiter.api.Assertions.assertTrue(noisy.rmse < 1.10, "Noisy sinus RMSE too high: " + noisy.rmse);
        }
    }

    private ScenarioResult runScenario(double freqA, double freqB, double noiseAmp) {
        DefaultChartModel model = new DefaultChartModel("sinus");
        HarmonicOscillatorPredictor predictor = new HarmonicOscillatorPredictor(120, 24, 1e-6, DT,
                6.0, 18, 0.2, 8.0, 0.80, 0.85, 7.0, 0.25, 10, 0.95, 12);
        DefaultPredictionBuffer buffer = new DefaultPredictionBuffer();

        SplittableRandom rnd = new SplittableRandom(42);
        Map<Long, Double> predictedByStep = new HashMap<>();

        int steps = 1500;
        int shockStep = steps / 2;
        int horizon = 24;
        int warmupSteps = 320;
        double sumSq = 0.0;
        double sumAbs = 0.0;
        int samples = 0;

        int settleTicks = -1;
        int settleWindow = 32;
        double settleThreshold = 0.10;
        double[] rollingSq = new double[settleWindow];
        int rollingIdx = 0;
        int rollingCount = 0;
        double rollingSumSq = 0.0;

        for (int step = 0; step < steps; step++) {
            double t = step * DT;
            double freq = (step < shockStep) ? freqA : freqB;
            double y = Math.sin(2.0 * Math.PI * freq * t);
            if (noiseAmp > 0.0) {
                y += noiseAmp * (rnd.nextDouble() * 2.0 - 1.0);
            }

            model.setXY(t, y);

            Double predicted = predictedByStep.remove((long) step);
            if (predicted != null) {
                double delta = y - predicted;
                boolean pastWarmup = step >= warmupSteps;
                if (pastWarmup) {
                    sumSq += delta * delta;
                    sumAbs += Math.abs(delta);
                    samples++;
                }

                if (step >= shockStep) {
                    double sq = delta * delta;
                    if (rollingCount < settleWindow) {
                        rollingSq[rollingIdx++] = sq;
                        rollingSumSq += sq;
                        rollingCount++;
                    } else {
                        rollingIdx %= settleWindow;
                        rollingSumSq -= rollingSq[rollingIdx];
                        rollingSq[rollingIdx++] = sq;
                        rollingSumSq += sq;
                    }
                    if (settleTicks < 0 && rollingCount == settleWindow) {
                        double windowRmse = Math.sqrt(rollingSumSq / settleWindow);
                        if (windowRmse < settleThreshold) settleTicks = step - shockStep;
                    }
                }
            }

            predictor.predict(model, null, buffer);
            int n = Math.min(buffer.count(), horizon);
            double[] xs = buffer.x();
            double[] ys = buffer.y();
            for (int i = 0; i < n; i++) {
                if (!Double.isFinite(xs[i])) continue;
                long targetStep = Math.round(xs[i] / DT);
                if (targetStep >= steps) break;
                if (targetStep > step) predictedByStep.putIfAbsent(targetStep, ys[i]);
            }
        }

        double rmse = (samples > 0) ? Math.sqrt(sumSq / samples) : Double.NaN;
        double mae = (samples > 0) ? (sumAbs / samples) : Double.NaN;
        return new ScenarioResult(rmse, mae, settleTicks);
    }

    private record ScenarioResult(double rmse, double mae, int settleTicks) {
    }
}
