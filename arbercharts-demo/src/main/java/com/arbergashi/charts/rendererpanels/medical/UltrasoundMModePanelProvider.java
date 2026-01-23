package com.arbergashi.charts.rendererpanels.medical;

import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.medical.UltrasoundMModeRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.ui.grid.MedicalGridLayer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import javax.swing.Timer;
import java.util.Random;

/**
 * Provides a high-quality Ultrasound M-Mode demonstration panel.
 * <p>
 * Generates realistic M-mode echocardiography data showing cardiac wall motion
 * over time. Uses the MedicalGridLayer for clinical visualization.
 * </p>
 */
public class UltrasoundMModePanelProvider {

    private static final int POINTS = 480;
    private static final int SAMPLE_RATE = 50; // Hz
    private static final double HEART_RATE = 72.0; // BPM

    public static ArberChartPanel create() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(POINTS, 2);
        model.setName("LV Wall Motion");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 38);

        java.util.function.DoubleFunction<double[]> generateFrame = (time) -> {
            double heartbeatPhase = (time * HEART_RATE / 60.0) % 1.0;

            // Posterior wall motion (deeper structure)
            double posteriorWallBase = 8.0; // cm from transducer
            double posteriorMotion;
            if (heartbeatPhase < 0.4) {
                // Systole - wall thickening and inward motion
                double t = heartbeatPhase / 0.4;
                posteriorMotion = -1.2 * Math.sin(t * Math.PI);
            } else {
                // Diastole - wall relaxation and outward motion
                double t = (heartbeatPhase - 0.4) / 0.6;
                posteriorMotion = 0.3 * Math.sin(t * Math.PI * 0.5);
            }
            double drift = 0.08 * Math.sin(time * 0.3);
            double posteriorDepth = posteriorWallBase + posteriorMotion + drift + rand.nextGaussian() * 0.05;

            // Echo intensity varies with tissue characteristics
            double baseIntensity = 190.0;
            double wallThickness = 1.0 + 0.4 * Math.sin(heartbeatPhase * 2 * Math.PI);
            double intensity = baseIntensity + wallThickness * 35 + rand.nextDouble() * 18;
            intensity = Math.min(255, Math.max(100, intensity));

            return new double[]{posteriorDepth, intensity};
        };

        final double[] time = {0.0};
        for (int i = 0; i < POINTS; i++) {
            model.add(time[0], generateFrame.apply(time[0]));
            time[0] += 1.0 / SAMPLE_RATE;
        }

        ArberChartPanel panel = ArberChartBuilder.create()
                .withTitle("Ultrasound M-Mode - LV Wall Motion")
                .addLayer(model, new UltrasoundMModeRenderer())
                .withGridLayer(new MedicalGridLayer())
                .withTooltips(true)
                .withLegend(true)
                .build();

        Timer timer = new Timer(1000 / SAMPLE_RATE, e -> {
            model.add(time[0], generateFrame.apply(time[0]));
            time[0] += 1.0 / SAMPLE_RATE;
        });
        DemoPanelUtils.attachManagedTimer(panel, timer);

        return panel;
    }
}
