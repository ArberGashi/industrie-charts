package com.arbergashi.charts.rendererpanels.analysis;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.analysis.OutlierDetectionRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;

import java.util.Random;

public class OutlierDetectionPanelProvider {
    public static ArberChartPanel create() {
        // Outlier Detection (MAD z-score) demo:
        // Goal: show a stable latency baseline + a few clear outliers that the renderer highlights.
        // We also include two "incident" windows (gradual degradation) so engineers see realistic behavior.
        // Deterministic dataset for QA/screenshot reproducibility.

        DefaultChartModel model = new DefaultChartModel("API p95 latency (ms)");

        int minutes = 240; // 4 hours, 1-min resolution
        Random rnd = new Random(DemoPanelUtils.DEMO_SEED + 9001);

        // Baseline resembling a healthy service
        double base = 185.0;

        // Explicit spikes: these should be flagged as outliers by MAD
        int[] spikeMinutes = {37, 58, 112, 147, 176, 209, 228};
        double[] spikeValues = {980, 780, 1250, 900, 1450, 1050, 1180};

        for (int t = 0; t < minutes; t++) {
            // Diurnal-ish (slow) wave + a faster component
            double slowWave = 14.0 * Math.sin(t / 28.0);
            double fastWave = 6.0 * Math.sin(t / 6.5);

            // Bursty noise: mostly small, sometimes slightly larger
            double noise = rnd.nextGaussian() * (6.0 + (rnd.nextDouble() < 0.08 ? 18.0 : 0.0));

            double latency = base + slowWave + fastWave + noise;

            // Incident window A: cache-miss storm (gradual degradation, not necessarily outliers)
            if (t >= 70 && t <= 92) {
                latency += 160.0 + (t - 70) * 3.5; // ramps up
            }

            // Incident window B: GC pressure (elevated baseline with oscillation)
            if (t >= 160 && t <= 185) {
                latency += 120.0 + 45.0 * Math.sin((t - 160) / 2.5);
            }

            // Hard spikes (true outliers)
            for (int i = 0; i < spikeMinutes.length; i++) {
                if (t == spikeMinutes[i]) {
                    latency = spikeValues[i];
                    break;
                }
            }

            // Keep within realistic bounds for the baseline (but allow spikes)
            if (latency < 80) latency = 80;

            String label = defaultLabelFor(t);
            model.addPoint(t, latency, 0, label);
        }

        OutlierDetectionRenderer renderer = new OutlierDetectionRenderer();

        return ArberChartBuilder.create()
                .withTitle("Outlier Detection (MAD) – API Latency Incidents")
                .addLayer(model, renderer)
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }

    private static String defaultLabelFor(int minute) {
        // Make tooltips instantly meaningful.
        if (minute >= 70 && minute <= 92) {
            return String.format("t=%d min (incident: cache misses)", minute);
        }
        if (minute >= 160 && minute <= 185) {
            return String.format("t=%d min (incident: GC pressure)", minute);
        }
        return String.format("t=%d min", minute);
    }
}
