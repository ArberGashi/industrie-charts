package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.SpectrogramRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;

import java.util.Random;

public class SpectrogramPanelProvider {
    public static ArberChartPanel create() {
        // Demo data represents a pseudo "spectrogram intensity" stream.
        // We create a clear time-localized event and a second harmonic band,
        // so the renderer has a meaningful structure to show.
        DefaultChartModel model = new DefaultChartModel("Spectrogram Intensity");

        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 126);

        int frames = 2400;
        for (int t = 0; t < frames; t++) {
            double base = 0.05 + 0.03 * rand.nextDouble();

            // Localized burst event
            double burst = gauss(t, 820, 1.0, 85) + gauss(t, 1360, 0.7, 120) + gauss(t, 1780, 0.5, 90);

            // Slow drift (environment)
            double drift = 0.08 * (0.5 + 0.5 * Math.sin(t / 260.0));

            // Fine grain noise
            double noise = Math.abs(rand.nextGaussian()) * 0.04;

            double v = base + drift + burst + noise;
            model.addPoint(t, v, 0, String.format("frame=%d", t));
        }

        return ArberChartBuilder.create()
                .withTitle("Spectrogram – Acoustic Event Detection")
                .addLayer(model, new SpectrogramRenderer())
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }

    private static double gauss(double x, double center, double amp, double sigma) {
        double d = x - center;
        return amp * Math.exp(-(d * d) / (2.0 * sigma * sigma));
    }
}
