package com.arbergashi.charts.rendererpanels.medical;

import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.medical.NIRSRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.ui.grid.MedicalGridLayer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import javax.swing.Timer;
import java.util.Random;
import java.util.function.DoubleSupplier;

/**
 * Provides a high-quality NIRS (Near-Infrared Spectroscopy) demonstration panel.
 * <p>
 * Generates realistic regional cerebral oxygen saturation (rSO2) data with
 * physiological variations. Uses the MedicalGridLayer for clinical visualization.
 * </p>
 */
public class NIRSPanelProvider {

    private static final int CAPACITY = 300;
    private static final double RSO2_BASELINE = 68.0; // % - normal cerebral oxygenation
    private static final double RSO2_LOWER_LIMIT = 50.0; // % - critical threshold

    public static ArberChartPanel create() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(CAPACITY, 1);
        model.setName("rSO2");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 35);

        final double[] time = {0};
        final double[] trend = {0}; // Slow trend component

        DoubleSupplier nextSample = () -> {
            // Slow physiological trend (blood pressure, cardiac output changes)
            trend[0] += (rand.nextDouble() - 0.5) * 0.1;
            trend[0] = Math.max(-8, Math.min(8, trend[0])); // Clamp to +/- 8%

            // Respiratory variation (~0.2 Hz)
            double respiratoryVariation = 1.5 * Math.sin(time[0] * 0.2 * 2 * Math.PI);

            // Cardiac pulsation artifact (subtle, ~1.2 Hz)
            double cardiacPulsation = 0.3 * Math.sin(time[0] * 1.2 * 2 * Math.PI);

            // Random measurement noise
            double noise = rand.nextGaussian() * 0.4;

            // Motion artifact (occasional)
            double motionArtifact = 0;
            if (rand.nextDouble() > 0.995) {
                motionArtifact = rand.nextGaussian() * 3.0;
            }

            double desaturation = 0.0;
            if (time[0] > 40 && time[0] < 60) {
                desaturation = -6.0;
            } else if (time[0] >= 60 && time[0] < 75) {
                desaturation = -3.0;
            } else if (time[0] >= 75 && time[0] < 85) {
                desaturation = 1.5;
            }
            double rSO2 = RSO2_BASELINE + trend[0] + respiratoryVariation
                        + cardiacPulsation + noise + motionArtifact;
            rSO2 += desaturation;

            // Clamp to physiological range
            rSO2 = Math.max(40, Math.min(85, rSO2));
            return rSO2;
        };

        for (int i = 0; i < CAPACITY; i++) {
            model.add(time[0], new double[]{nextSample.getAsDouble()});
            time[0] += 0.1; // 100ms intervals
        }

        ArberChartPanel panel = ArberChartBuilder.create()
                .withTitle("NIRS - Cerebral Oximetry (rSO2)")
                .addLayer(model, new NIRSRenderer())
                .withGridLayer(new MedicalGridLayer())
                .withTooltips(true)
                .withLegend(true)
                .build();

        Timer timer = new Timer(100, e -> { // 10 Hz update (typical NIRS)
            model.add(time[0], new double[]{nextSample.getAsDouble()});
            time[0] += 0.1; // 100ms intervals
        });
        DemoPanelUtils.attachManagedTimer(panel, timer);

        return panel;
    }
}
