package com.arbergashi.charts.rendererpanels.medical;

import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.medical.EOGRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.ui.grid.MedicalGridLayer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import javax.swing.Timer;
import java.util.Random;

/**
 * Provides a high-quality EOG (Electrooculography) demonstration panel.
 * <p>
 * Generates realistic eye movement signals showing saccades, smooth pursuit,
 * and blinks. Uses the MedicalGridLayer for clinical visualization.
 * </p>
 */
public class EOGPanelProvider {

    private static final int CAPACITY = 1000;
    private static final int SAMPLE_RATE = 100; // Hz

    public static ArberChartPanel create() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(CAPACITY, 1);
        model.setName("EOG Signal");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 40);

        ArberChartPanel panel = ArberChartBuilder.create()
                .withTitle("EOG - Eye Movement Signal")
                .addLayer(model, new EOGRenderer())
                .withGridLayer(new MedicalGridLayer())
                .withTooltips(true)
                .withLegend(true)
                .build();

        final double[] time = {0};
        final double[] eyePosition = {0}; // Current eye position (-1 to 1)
        final double[] targetPosition = {0}; // Target for smooth pursuit
        final double[] lastSaccadeTime = {-1};

        Timer timer = new Timer(1000 / 60, e -> {
            int samplesPerFrame = SAMPLE_RATE / 60;
            for (int i = 0; i < samplesPerFrame; i++) {
                double signal = eyePosition[0];

                // Saccadic eye movements (rapid jumps)
                if (time[0] - lastSaccadeTime[0] > 0.8 + rand.nextDouble() * 1.5) {
                    // New saccade target
                    double saccadeAmplitude = (rand.nextDouble() - 0.5) * 1.6;
                    eyePosition[0] = Math.max(-1, Math.min(1, eyePosition[0] + saccadeAmplitude));
                    lastSaccadeTime[0] = time[0];
                }

                // Smooth pursuit component (slow tracking)
                targetPosition[0] = 0.4 * Math.sin(time[0] * 0.5 * 2 * Math.PI);
                double pursuit = (targetPosition[0] - eyePosition[0]) * 0.02;
                eyePosition[0] += pursuit;

                // Drift (microsaccades during fixation)
                double drift = rand.nextGaussian() * 0.005;
                eyePosition[0] += drift;

                // Blink artifact (occasional large deflection)
                double blink = 0;
                double blinkChance = (time[0] > 12 && time[0] < 18) ? 0.993 : 0.997;
                if (rand.nextDouble() > blinkChance) {
                    blink = -0.8 + rand.nextDouble() * 0.3;
                }

                // Electrode noise
                double noise = rand.nextGaussian() * 0.02;

                signal = eyePosition[0] + blink + noise;

                model.add(time[0], new double[]{signal});
                time[0] += 1.0 / SAMPLE_RATE;
            }
        });
        DemoPanelUtils.attachManagedTimer(panel, timer);

        return panel;
    }
}
