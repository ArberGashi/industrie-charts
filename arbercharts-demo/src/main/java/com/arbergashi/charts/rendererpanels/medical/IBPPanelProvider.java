package com.arbergashi.charts.rendererpanels.medical;

import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.medical.IBPRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.ui.grid.MedicalGridLayer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import javax.swing.Timer;
import java.util.Random;

/**
 * Provides a high-quality IBP (Invasive Blood Pressure) demonstration panel.
 * <p>
 * Generates realistic arterial pressure waveforms showing systolic upstroke,
 * dicrotic notch, and diastolic runoff. Uses the MedicalGridLayer for clinical visualization.
 * </p>
 */
public class IBPPanelProvider {

    private static final int CAPACITY = 1500;
    private static final int SAMPLE_RATE = 125; // Hz
    private static final double BASE_SYSTOLIC = 120.0; // mmHg
    private static final double BASE_DIASTOLIC = 80.0; // mmHg
    private static final double BASE_HEART_RATE = 72.0; // BPM

    public static ArberChartPanel create() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(CAPACITY, 1);
        model.setName("Arterial Pressure");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 37);

        final double[] time = {0};
        java.util.function.DoubleFunction<Double> generatePressure = t -> {
            double hrFactor = (BASE_HEART_RATE / 60.0) * (1.0 + 0.04 * Math.sin(t * 0.08 * 2 * Math.PI));
            double phase = (t * hrFactor) % 1.0;

            // Pressure variation with respiration (~0.2 Hz)
            double respiratoryVariation = 5.0 * Math.sin(t * 0.2 * 2 * Math.PI);
            double systolic = BASE_SYSTOLIC + respiratoryVariation;
            double diastolic = BASE_DIASTOLIC + respiratoryVariation * 0.5;
            double pulsePress = systolic - diastolic;
            double notchDepth = 0.08 + 0.02 * Math.sin(t * 0.14 * 2 * Math.PI);

            double pressure;
            if (phase < 0.15) {
                // Systolic upstroke (rapid rise)
                double u = phase / 0.15;
                pressure = diastolic + pulsePress * Math.pow(u, 0.8);
            } else if (phase < 0.35) {
                // Systolic peak and initial decline
                double u = (phase - 0.15) / 0.20;
                pressure = systolic - pulsePress * 0.15 * u;
            } else if (phase < 0.40) {
                // Dicrotic notch (aortic valve closure)
                double u = (phase - 0.35) / 0.05;
                pressure = systolic - pulsePress * 0.15 - pulsePress * notchDepth * Math.sin(u * Math.PI);
            } else if (phase < 0.50) {
                // Dicrotic wave
                double u = (phase - 0.40) / 0.10;
                pressure = diastolic + pulsePress * 0.25 * Math.exp(-u * 2);
            } else {
                // Diastolic runoff (exponential decay)
                double u = (phase - 0.50) / 0.50;
                pressure = diastolic + pulsePress * 0.15 * Math.exp(-u * 3);
            }

            // Add small noise and a subtle dither to avoid a perfectly repeating loop
            pressure += rand.nextGaussian() * 0.5 + Math.sin(t * 0.03) * 0.2;
            return pressure;
        };

        for (int i = 0; i < CAPACITY; i++) {
            model.add(time[0], new double[]{generatePressure.apply(time[0])});
            time[0] += 1.0 / SAMPLE_RATE;
        }

        ArberChartPanel panel = ArberChartBuilder.create()
                .withTitle("IBP - Arterial Line Waveform")
                .addLayer(model, new IBPRenderer())
                .withGridLayer(new MedicalGridLayer())
                .withTooltips(true)
                .withLegend(true)
                .build();

        Timer timer = new Timer(1000 / 60, e -> {
            int samplesPerFrame = SAMPLE_RATE / 60;
            for (int i = 0; i < samplesPerFrame; i++) {
                model.add(time[0], new double[]{generatePressure.apply(time[0])});
                time[0] += 1.0 / SAMPLE_RATE;
            }
        });
        DemoPanelUtils.attachManagedTimer(panel, timer);

        return panel;
    }
}
