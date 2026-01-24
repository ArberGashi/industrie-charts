package com.arbergashi.charts.rendererpanels.medical;

import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.medical.SweepEraseEKGRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.ui.grid.MedicalGridLayer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import javax.swing.Timer;
import java.util.Random;

/**
 * Provides a high-quality Sweep-Erase EKG demonstration panel.
 * <p>
 * Generates a classic patient monitor style ECG display with sweep-erase effect.
 * Uses the MedicalGridLayer for clinical-grade visualization similar to bedside monitors.
 * </p>
 */
public class SweepEKGPanelProvider {

    private static final int CAPACITY = 1000;
    private static final int SAMPLE_RATE = 250; // Hz
    private static final double BASE_HEART_RATE = 75.0; // BPM

    public static ArberChartPanel create() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(CAPACITY, 1);
        model.setName("ECG Sweep");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 42);

        ArberChartPanel panel = ArberChartBuilder.of(model, new SweepEraseEKGRenderer())
                .withTitle("ECG Monitor - Sweep Display")
                .withGridLayer(new MedicalGridLayer())
                .withTooltips(false) // Disable for real-time monitor feel
                .withLegend(false)
                .hints(h -> h
                        .antialiasing(true)
                        .strokeWidth(1.6f))
                .build();

        final double[] time = {0};

        Timer timer = new Timer(1000 / 60, e -> {
            int samplesPerFrame = SAMPLE_RATE / 60;
            for (int i = 0; i < samplesPerFrame; i++) {
                double hrFactor = (BASE_HEART_RATE / 60.0) * (1.0 + 0.035 * Math.sin(time[0] * 0.1 * 2 * Math.PI));
                double phase = (time[0] * hrFactor) % 1.0;

                // Realistic PQRST morphology
                double pWave = 0.10 * Math.exp(-Math.pow((phase - 0.16) / 0.035, 2));
                double qWave = -0.05 * Math.exp(-Math.pow((phase - 0.40) / 0.010, 2));
                double rWave = 0.80 * Math.exp(-Math.pow((phase - 0.42) / 0.012, 2));
                double sWave = -0.12 * Math.exp(-Math.pow((phase - 0.44) / 0.010, 2));
                double tWave = 0.20 * Math.exp(-Math.pow((phase - 0.65) / 0.070, 2));

                // Baseline noise (power line + movement)
                double noise = rand.nextGaussian() * 0.015;
                double baseline = 0.02 * Math.sin(time[0] * 0.3 * 2 * Math.PI);

                double ekg = pWave + qWave + rWave + sWave + tWave + noise + baseline;

                // Normalized X for sweep display (0 to 1 across capacity)
                double xVal = (double)(model.getHeadIndex()) / CAPACITY;
                model.add(xVal, new double[]{ekg});
                time[0] += 1.0 / SAMPLE_RATE;
            }
        });
        DemoPanelUtils.attachManagedTimer(panel, timer);

        return panel;
    }
}
