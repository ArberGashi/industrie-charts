package com.arbergashi.charts.rendererpanels.medical;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.LineRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.ui.grid.MedicalGridLayer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

/**
 * Provides a high-quality HRV (Heart Rate Variability) demonstration panel.
 * <p>
 * Generates realistic RR interval (tachogram) data showing respiratory sinus
 * arrhythmia and autonomic nervous system modulation. Uses MedicalGridLayer
 * for clinical-grade visualization.
 * </p>
 */
public class HeartRateVariabilityPanelProvider {

    private static final int BEAT_COUNT = 360;
    private static final double BASE_RR_INTERVAL = 830.0; // ms (~72 BPM)
    private static final double MIN_RR = 480.0;
    private static final double MAX_RR = 1250.0;

    public static ArberChartPanel create() {
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 38);

        // RR Interval Tachogram
        DefaultChartModel rrModel = new DefaultChartModel("RR Interval (ms)");

        double cumulativeTime = 0.0;
        double recovery = 0.0;

        for (int beat = 0; beat < BEAT_COUNT; beat++) {
            double t = cumulativeTime / 1000.0;

            // Very Low Frequency (VLF) ~0.02 Hz
            double vlf = 22.0 * Math.sin(2 * Math.PI * 0.02 * t);

            // Low Frequency (LF) ~0.1 Hz
            double lf = 30.0 * Math.sin(2 * Math.PI * 0.10 * t);

            // High Frequency (HF) ~0.25 Hz (respiratory)
            double hf = 45.0 * Math.sin(2 * Math.PI * 0.25 * t);

            // Random component (biological noise)
            double noise = rand.nextGaussian() * 7.5;

            // Occasional ectopic beat (PVC) with recovery
            if (rand.nextDouble() > 0.985 && recovery <= 0.0) {
                recovery = 120.0 + rand.nextDouble() * 60.0;
            }
            double ectopic = 0.0;
            if (recovery > 0.0) {
                ectopic = -80.0;
                recovery -= 40.0;
            }

            double rrInterval = BASE_RR_INTERVAL + vlf + lf + hf + noise + ectopic;
            rrInterval = Math.max(MIN_RR, Math.min(MAX_RR, rrInterval));

            rrModel.addPoint(t, rrInterval, 0,
                    String.format("Beat %d: %.0f ms", beat + 1, rrInterval));

            cumulativeTime += rrInterval;
        }

        LineRenderer renderer = new LineRenderer();

        return ArberChartBuilder.create()
                .withTitle("HRV Analysis - RR Interval Tachogram")
                .addLayer(rrModel, renderer)
                .withGridLayer(new MedicalGridLayer())
                .withTooltips(true)
                .withLegend(true)
                .xAxis(axis -> axis
                        .setUnitSuffix("s")
                        .setTicks(6))
                .yAxis(axis -> axis
                        .setUnitSuffix("ms")
                        .setTicks(8))
                .build();
    }
}
