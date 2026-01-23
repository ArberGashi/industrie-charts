package com.arbergashi.charts.rendererpanels.medical;

import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.medical.MedicalSweepRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.ui.grid.MedicalGridLayer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import javax.swing.Timer;
import java.util.Random;

/**
 * Provides a generic Medical Sweep demonstration panel.
 * <p>
 * Demonstrates the sweep-erase rendering mode used in patient monitors.
 * Uses the MedicalGridLayer for clinical-grade visualization.
 * </p>
 */
public class MedicalSweepPanelProvider {

    private static final int CAPACITY = 800;
    private static final int SAMPLE_RATE = 125; // Hz

    public static ArberChartPanel create() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(CAPACITY, 2);
        model.setName("Monitor Sweep");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 41);

        ArberChartPanel panel = ArberChartBuilder.create()
                .withTitle("Medical Sweep - Multi-signal")
                .addLayer(model, new MedicalSweepRenderer())
                .withGridLayer(new MedicalGridLayer())
                .withTooltips(false) // Clean monitor display
                .withLegend(false)
                .build();

        final double[] time = {0};

        Timer timer = new Timer(1000 / 60, e -> {
            int samplesPerFrame = SAMPLE_RATE / 60;
            for (int i = 0; i < samplesPerFrame; i++) {
                // Multi-frequency physiological signal
                double primary = 0.55 * Math.sin(time[0] * 1.1 * 2 * Math.PI);
                double secondary = 0.24 * Math.sin(time[0] * 3.6 * 2 * Math.PI);
                double respiratory = 0.16 * Math.sin(time[0] * 0.25 * 2 * Math.PI);
                double burst = (time[0] % 12.0 > 10.0) ? 0.18 * Math.sin(time[0] * 7.5 * 2 * Math.PI) : 0.0;
                double noise = rand.nextGaussian() * 0.045;

                double signal = primary + secondary + respiratory + burst + noise;
                double signal2 = 0.36 * Math.sin(time[0] * 0.9 * 2 * Math.PI + 0.6)
                        + rand.nextGaussian() * 0.03;

                // Normalized X for sweep display
                double xVal = (double)(model.getHeadIndex()) / CAPACITY;
                model.add(xVal, new double[]{signal2, signal});
                time[0] += 1.0 / SAMPLE_RATE;
            }
        });
        DemoPanelUtils.attachManagedTimer(panel, timer);

        return panel;
    }
}
