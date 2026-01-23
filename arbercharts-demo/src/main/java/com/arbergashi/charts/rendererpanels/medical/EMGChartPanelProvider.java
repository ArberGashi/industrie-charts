package com.arbergashi.charts.rendererpanels.medical;

import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.medical.EMGRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.ui.grid.MedicalGridLayer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import javax.swing.Timer;
import java.util.Random;

/**
 * Provides a high-quality EMG (Electromyography) demonstration panel.
 * <p>
 * Generates realistic EMG waveforms showing muscle electrical activity with
 * motor unit action potentials. Uses the MedicalGridLayer for clinical visualization.
 * </p>
 */
public class EMGChartPanelProvider {

    private static final int CAPACITY = 2000;
    private static final int SAMPLE_RATE = 1000; // Hz - typical EMG sample rate

    public static ArberChartPanel create() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(CAPACITY, 1);
        model.setName("EMG sEMG");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 32);

        ArberChartPanel panel = ArberChartBuilder.create()
                .withTitle("EMG - Muscle Activation (Surface)")
                .addLayer(model, new EMGRenderer())
                .withGridLayer(new MedicalGridLayer())
                .withTooltips(true)
                .withLegend(true)
                .build();

        final double[] time = {0};
        final double[] muscleState = {0}; // 0 = relaxed, 1 = contracted

        Timer timer = new Timer(1000 / 60, e -> {
            int samplesPerFrame = SAMPLE_RATE / 60;
            for (int i = 0; i < samplesPerFrame; i++) {
                // Simulate muscle contraction cycles with smooth envelope
                double cyclePhase = (time[0] * 0.3) % 1.0;
                if (cyclePhase > 0.3 && cyclePhase < 0.7) {
                    double t = (cyclePhase - 0.3) / 0.4;
                    muscleState[0] = 0.5 - 0.5 * Math.cos(t * Math.PI);
                } else {
                    muscleState[0] = 0.0;
                }

                // Motor unit action potentials (MUAPs)
                double muapFrequency = muscleState[0] > 0.5 ? 25.0 : 5.0;
                double fatigue = 1.0 - 0.08 * Math.sin(time[0] * 0.04 * 2 * Math.PI);
                double muapAmplitude = (muscleState[0] > 0.5 ? 0.8 : 0.1) * fatigue;

                // Individual motor unit firing
                double muap1 = muapAmplitude * Math.sin(time[0] * muapFrequency * 2 * Math.PI);
                double muap2 = muapAmplitude * 0.7 * Math.sin(time[0] * muapFrequency * 1.3 * 2 * Math.PI);
                double muap3 = muapAmplitude * 0.5 * Math.sin(time[0] * muapFrequency * 1.7 * 2 * Math.PI);

                // Random motor unit recruitment spikes
                double spikes = 0;
                if (muscleState[0] > 0.5 && rand.nextDouble() > 0.92) {
                    spikes = (rand.nextDouble() - 0.5) * 2.0;
                }

                // Baseline noise (electrode noise)
                double noise = rand.nextGaussian() * (muscleState[0] > 0.5 ? 0.15 : 0.05);

                // Power line interference (50/60 Hz)
                double powerLine = 0.02 * Math.sin(time[0] * 50 * 2 * Math.PI);

                double emgValue = muap1 + muap2 + muap3 + spikes + noise + powerLine;

                model.add(time[0], new double[]{emgValue});
                time[0] += 1.0 / SAMPLE_RATE;
            }
        });
        DemoPanelUtils.attachManagedTimer(panel, timer);

        return panel;
    }
}
