package com.arbergashi.charts.rendererpanels.medical;

import com.arbergashi.charts.model.FastMedicalModel;
import com.arbergashi.charts.render.medical.EEGRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.ui.grid.MedicalGridLayer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import javax.swing.Timer;
import java.util.Random;

/**
 * Provides a high-quality EEG (Electroencephalogram) demonstration panel.
 * <p>
 * Generates realistic multi-channel EEG waveforms showing brain electrical activity
 * with alpha, beta, theta, and delta rhythm components. Uses the MedicalGridLayer
 * for clinical-grade visualization.
 * </p>
 */
public class EEGChartPanelProvider {

    private static final int CHANNELS = 8;
    private static final int POINTS_PER_CHANNEL = 500;

    public static ArberChartPanel create() {
        FastMedicalModel model = new FastMedicalModel("EEG Cortical Rhythm", CHANNELS * POINTS_PER_CHANNEL);
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 31);

        // Generate realistic EEG data for each channel
        for (int ch = 0; ch < CHANNELS; ch++) {
            // Each channel has different dominant frequencies
            double alphaWeight = 0.3 + (ch % 3) * 0.15;   // 8-13 Hz (relaxed, eyes closed)
            double betaWeight = 0.2 + (ch % 2) * 0.1;     // 13-30 Hz (active thinking)
            double thetaWeight = 0.15;                      // 4-8 Hz (drowsiness)
            double deltaWeight = 0.1;                       // 0.5-4 Hz (deep sleep)

            for (int i = 0; i < POINTS_PER_CHANNEL; i++) {
                double t = i * 0.004; // 250 Hz sample rate equivalent

                // Alpha rhythm (8-12 Hz) - posterior dominant rhythm
                double alphaMod = 1.0 + 0.15 * Math.sin(t * 0.7 * 2 * Math.PI);
                double alpha = alphaWeight * alphaMod * Math.sin(t * 10 * 2 * Math.PI)
                             + alphaWeight * 0.3 * Math.sin(t * 11 * 2 * Math.PI);

                // Beta rhythm (13-30 Hz) - frontal activity
                double beta = betaWeight * Math.sin(t * 18 * 2 * Math.PI)
                            + betaWeight * 0.4 * Math.sin(t * 25 * 2 * Math.PI);

                // Theta rhythm (4-8 Hz) - temporal activity
                double theta = thetaWeight * Math.sin(t * 6 * 2 * Math.PI);

                // Delta rhythm (0.5-4 Hz) - slow wave
                double delta = deltaWeight * Math.sin(t * 2 * 2 * Math.PI);

                // Eye movement artifact (occasional)
                double eyeArtifact = 0;
                if (ch < 2 && rand.nextDouble() > 0.995) {
                    eyeArtifact = rand.nextGaussian() * 0.5;
                }

                // Electrode noise
                double noise = rand.nextGaussian() * 0.08;

                double gain = 0.7 + (ch % 4) * 0.08;
                double eegValue = (alpha + beta + theta + delta) * gain + eyeArtifact + noise;

                model.addPoint(i, eegValue);
            }
        }

        ArberChartPanel panel = ArberChartBuilder.create()
                .withTitle("EEG - 8-Channel Cortical Rhythm")
                .addLayer(model, new EEGRenderer(CHANNELS))
                .withGridLayer(new MedicalGridLayer())
                .withTooltips(true)
                .withLegend(true)
                .build();

        // Optional: add live update simulation
        Timer timer = new Timer(100, e -> {
            // EEG demo uses static data for clarity
        });
        DemoPanelUtils.attachManagedTimer(panel, timer);

        return panel;
    }
}
