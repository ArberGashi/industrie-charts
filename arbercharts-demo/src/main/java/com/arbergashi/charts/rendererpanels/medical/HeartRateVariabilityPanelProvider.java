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

    private static final int BEAT_COUNT = 300;
    private static final double BASE_RR_INTERVAL = 833.0; // ms (72 BPM)

    public static ArberChartPanel create() {
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 38);

        // RR Interval Tachogram
        DefaultChartModel rrModel = new DefaultChartModel("RR Interval (ms)");

        double cumulativeTime = 0;

        for (int beat = 0; beat < BEAT_COUNT; beat++) {
            // Very Low Frequency (VLF) component - thermoregulation, RAAS
            double vlf = 30.0 * Math.sin(beat * 0.01 * 2 * Math.PI);

            // Low Frequency (LF) component - sympathetic + parasympathetic
            // ~0.04-0.15 Hz => ~7-25 beats cycle
            double lf = 25.0 * Math.sin(beat * 0.07 * 2 * Math.PI);

            // High Frequency (HF) component - respiratory sinus arrhythmia
            // ~0.15-0.4 Hz => ~2.5-7 beats cycle (15 breaths/min = 4 beats/breath)
            double breathRate = 0.22 + 0.03 * Math.sin(beat * 0.02 * 2 * Math.PI);
            double hf = 40.0 * Math.sin(beat * breathRate * 2 * Math.PI);

            // Random component (biological noise)
            double noise = rand.nextGaussian() * 8.0;

            // Occasional ectopic beat (premature ventricular contraction)
            double ectopic = 0;
            if (rand.nextDouble() > 0.98) {
                ectopic = -100 + rand.nextDouble() * 50; // Short RR
            }

            double rrInterval = BASE_RR_INTERVAL + vlf + lf + hf + noise + ectopic;
            rrInterval = Math.max(500, Math.min(1200, rrInterval)); // Physiological limits

            rrModel.addPoint(cumulativeTime / 1000.0, rrInterval, 0,
                String.format("Beat %d: %.0f ms", beat, rrInterval));

            cumulativeTime += rrInterval;
        }

        LineRenderer renderer = new LineRenderer();

        return ArberChartBuilder.create()
            .withTitle("HRV Analysis - RR Interval Tachogram")
            .addLayer(rrModel, renderer)
            .withGridLayer(new MedicalGridLayer())
            .withTooltips(true)
            .withLegend(true)
            .build();
    }
}
