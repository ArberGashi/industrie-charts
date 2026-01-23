package com.arbergashi.charts.rendererpanels.medical;

import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.medical.VentilatorWaveformRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.ui.grid.MedicalGridLayer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import javax.swing.Timer;
import java.util.Random;

/**
 * Provides a high-quality Ventilator Waveform demonstration panel.
 * <p>
 * Generates realistic mechanical ventilator waveforms showing pressure, flow,
 * and volume curves during controlled ventilation. Uses the MedicalGridLayer
 * for clinical-grade ICU monitor visualization.
 * </p>
 */
public class VentilatorWaveformPanelProvider {

    private static final int POINTS = 600;
    private static final int SAMPLE_RATE = 50; // Hz
    private static final double RESPIRATORY_RATE = 14.0; // breaths/min
    private static final double TIDAL_VOLUME = 500.0; // mL
    private static final double PEAK_PRESSURE = 25.0; // cmH2O
    private static final double PEEP = 5.0; // cmH2O

    public static ArberChartPanel create() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(POINTS, 3);
        model.setName("Ventilator Waveforms");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 36);

        double breathPeriod = 60.0 / RESPIRATORY_RATE; // seconds
        double inspiratoryTime = breathPeriod * 0.33; // I:E ratio ~1:2

        java.util.function.DoubleFunction<double[]> generateWaveform = (time) -> {
            double volumeFactor = 1.0 + 0.04 * Math.sin(time * 0.08 * 2 * Math.PI);
            double breathPhase = (time % breathPeriod) / breathPeriod;
            double inspiratoryFraction = inspiratoryTime / breathPeriod;

            double pressure;
            double flow;
            double volume;

            if (breathPhase < inspiratoryFraction) {
                // Inspiration phase
                double t = breathPhase / inspiratoryFraction;

                // Pressure: rapid rise to peak, then plateau
                if (t < 0.1) {
                    pressure = PEEP + (PEAK_PRESSURE - PEEP) * (t / 0.1);
                } else {
                    pressure = PEAK_PRESSURE - (t - 0.1) * 2.0; // Slight plateau decay
                }

                // Flow: decelerating (more clinical feel)
                flow = 75.0 - t * 25.0;

                // Volume: linear rise
                volume = TIDAL_VOLUME * volumeFactor * t;

            } else if (breathPhase < inspiratoryFraction + 0.05) {
                // End-inspiratory pause
                double t = (breathPhase - inspiratoryFraction) / 0.05;
                pressure = PEAK_PRESSURE - 3.0 - t * 2.0 + 0.4 * Math.sin(t * Math.PI);
                flow = 0;
                volume = TIDAL_VOLUME * volumeFactor;

            } else {
                // Expiration phase (passive)
                double t = (breathPhase - inspiratoryFraction - 0.05) / (1 - inspiratoryFraction - 0.05);

                // Pressure: exponential decay to PEEP
                pressure = PEEP + (PEAK_PRESSURE - 5.0 - PEEP) * Math.exp(-t * 4);

                // Flow: negative, exponential decay
                flow = -80.0 * Math.exp(-t * 3);

                // Volume: exponential decay
                volume = TIDAL_VOLUME * volumeFactor * Math.exp(-t * 3);
            }

            // Add small noise
            pressure += rand.nextGaussian() * 0.2;
            flow += rand.nextGaussian() * 1.0;
            volume += rand.nextGaussian() * 2.0;

            // Ensure PEEP baseline
            pressure = Math.max(PEEP, pressure);
            volume = Math.max(0, volume);

            return new double[]{pressure, flow, volume};
        };

        final double[] time = {0.0};
        for (int i = 0; i < POINTS; i++) {
            model.add(time[0], generateWaveform.apply(time[0]));
            time[0] += 1.0 / SAMPLE_RATE;
        }

        ArberChartPanel panel = ArberChartBuilder.create()
                .withTitle("Ventilator Monitor - Pressure/Flow/Volume")
                .addLayer(model, new VentilatorWaveformRenderer())
                .withGridLayer(new MedicalGridLayer())
                .withTooltips(true)
                .withLegend(true)
                .build();

        Timer timer = new Timer(1000 / SAMPLE_RATE, e -> {
            model.add(time[0], generateWaveform.apply(time[0]));
            time[0] += 1.0 / SAMPLE_RATE;
        });
        DemoPanelUtils.attachManagedTimer(panel, timer);

        return panel;
    }
}
