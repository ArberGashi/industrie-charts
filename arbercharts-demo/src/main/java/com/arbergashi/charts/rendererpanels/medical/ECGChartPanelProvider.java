package com.arbergashi.charts.rendererpanels.medical;

import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.medical.ECGRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.ui.grid.MedicalGridLayer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import javax.swing.Timer;
import java.util.Random;

/**
 * Provides a high-quality ECG (Electrocardiogram) demonstration panel.
 * <p>
 * Generates realistic ECG waveforms with proper P-wave, QRS complex, and T-wave
 * morphology. Uses the MedicalGridLayer for clinical-grade 1mm/5mm grid rendering.
 * </p>
 */
public class ECGChartPanelProvider {

    private static final int CAPACITY = 3000;
    private static final int SAMPLE_RATE = 250; // Hz - standard ECG sample rate
    private static final double BASE_HEART_RATE = 72.0; // BPM

    public static ArberChartPanel create() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(CAPACITY, 1);
        model.setName("ECG Lead II");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 30);

        // Realistic ECG waveform generator (PQRST complex)
        java.util.function.DoubleFunction<double[]> generateECG = (t) -> {
            double phase = t % 1.0; // One heartbeat cycle

            // P-wave: atrial depolarization (0.08-0.10s duration, 0.1-0.2mV amplitude)
            double pWave = 0.12 * Math.exp(-Math.pow((phase - 0.16) / 0.04, 2));

            // PR segment (isoelectric)

            // QRS complex: ventricular depolarization
            double qWave = -0.08 * Math.exp(-Math.pow((phase - 0.38) / 0.012, 2));
            double rWave = 1.2 * Math.exp(-Math.pow((phase - 0.40) / 0.015, 2));
            double sWave = -0.15 * Math.exp(-Math.pow((phase - 0.42) / 0.012, 2));

            // ST segment (slight elevation for realism)
            double stSegment = 0.02 * Math.exp(-Math.pow((phase - 0.50) / 0.06, 2));

            // T-wave: ventricular repolarization (0.10-0.25s duration)
            double tWave = 0.25 * Math.exp(-Math.pow((phase - 0.65) / 0.08, 2));

            // U-wave (subtle, sometimes visible)
            double uWave = 0.02 * Math.exp(-Math.pow((phase - 0.82) / 0.03, 2));

            // Baseline wander (respiratory artifact ~0.15-0.3 Hz)
            double baselineWander = 0.03 * Math.sin(t * 0.25 * 2 * Math.PI);

            // High-frequency noise (EMG artifact)
            double noise = (rand.nextDouble() - 0.5) * 0.02;

            // Occasional ectopic spike (rare, subtle)
            double ectopic = (rand.nextDouble() > 0.9997)
                    ? 0.35 * Math.exp(-Math.pow((phase - 0.40) / 0.010, 2))
                    : 0.0;

            double ecgValue = pWave + qWave + rWave + sWave + stSegment + tWave + uWave
                            + baselineWander + noise + ectopic;

            return new double[]{ecgValue};
        };

        ArberChartPanel panel = ArberChartBuilder.of(model, new ECGRenderer())
                .withTitle("ECG Monitor - Lead II Rhythm")
                .withGridLayer(new MedicalGridLayer())
                .withTooltips(true)
                .withLegend(true)
                .xAxis(axis -> axis
                        .medicalScale(25)
                        .setUnitSuffix("s")
                        .setTicks(6))
                .yAxis(axis -> axis
                        .medicalScale(10)
                        .setUnitSuffix("mV")
                        .setTicks(6))
                .hints(h -> h
                        .antialiasing(true)
                        .strokeWidth(1.6f))
                .build();

        final double[] time = {0};
        Timer timer = new Timer(1000 / 60, e -> {
            int samplesPerFrame = SAMPLE_RATE / 60;
            for (int i = 0; i < samplesPerFrame; i++) {
                double hrFactor = (BASE_HEART_RATE / 60.0) * (1.0 + 0.03 * Math.sin(time[0] * 0.12 * 2 * Math.PI));
                double tNormalized = time[0] * hrFactor;
                model.add(time[0], generateECG.apply(tNormalized));
                time[0] += 1.0 / SAMPLE_RATE;
            }
        });
        DemoPanelUtils.attachManagedTimer(panel, timer);

        return panel;
    }
}
