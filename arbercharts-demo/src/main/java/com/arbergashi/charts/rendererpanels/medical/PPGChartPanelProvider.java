package com.arbergashi.charts.rendererpanels.medical;

import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.medical.PPGRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.ui.grid.MedicalGridLayer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import javax.swing.Timer;
import java.util.Random;

/**
 * Provides a high-quality PPG (Photoplethysmography) demonstration panel.
 * <p>
 * Generates realistic PPG waveforms showing arterial blood volume changes.
 * Uses the MedicalGridLayer for clinical-grade visualization.
 * </p>
 */
public class PPGChartPanelProvider {

    private static final int CAPACITY = 1800;
    private static final int SAMPLE_RATE = 125; // Hz - typical pulse oximeter rate
    private static final double BASE_HEART_RATE = 68.0; // BPM

    public static ArberChartPanel create() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(CAPACITY, 1);
        model.setName("PPG Pulse");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 33);

        ArberChartPanel panel = ArberChartBuilder.create()
                .withTitle("PPG - Pulse Oximetry Waveform")
                .addLayer(model, new PPGRenderer())
                .withGridLayer(new MedicalGridLayer())
                .withTooltips(true)
                .withLegend(true)
                .build();

        final double[] time = {0};
        Timer timer = new Timer(1000 / 60, e -> {
            int samplesPerFrame = SAMPLE_RATE / 60;
            for (int i = 0; i < samplesPerFrame; i++) {
                double hrFactor = (BASE_HEART_RATE / 60.0) * (1.0 + 0.030 * Math.sin(time[0] * 0.10 * 2 * Math.PI));
                double phase = (time[0] * hrFactor) % 1.0;

                // Systolic peak (sharp upstroke)
                double systolicPeak = Math.exp(-Math.pow((phase - 0.25) / 0.08, 2));

                // Dicrotic notch (aortic valve closure)
                double dicroticNotch = -0.15 * Math.exp(-Math.pow((phase - 0.45) / 0.03, 2));

                // Diastolic wave (reflected wave)
                double diastolicWave = 0.35 * Math.exp(-Math.pow((phase - 0.55) / 0.10, 2));

                // Diastolic decay
                double decay = (phase > 0.6) ? 0.3 * Math.exp(-(phase - 0.6) * 3) : 0;

                // Respiratory modulation (slow baseline)
                double resp = 1.0 + 0.06 * Math.sin(time[0] * 0.22 * 2 * Math.PI);

                // Motion artifact (occasional)
                double motionArtifact = (rand.nextDouble() > 0.996)
                    ? (rand.nextDouble() - 0.5) * 0.25 : 0;

                // Baseline noise
                double noise = (rand.nextDouble() - 0.5) * 0.02;

                double perfusion = 1.0 + 0.08 * Math.sin(time[0] * 0.18 * 2 * Math.PI);
                double ppgValue = (systolicPeak + dicroticNotch + diastolicWave + decay) * perfusion * resp
                                + motionArtifact + noise;

                model.add(time[0], new double[]{ppgValue});
                time[0] += 1.0 / SAMPLE_RATE;
            }
        });
        DemoPanelUtils.attachManagedTimer(panel, timer);

        return panel;
    }
}
