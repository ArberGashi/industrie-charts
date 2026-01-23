package com.arbergashi.charts.rendererpanels.medical;

import com.arbergashi.charts.model.CircularFastMedicalModel;
import com.arbergashi.charts.render.medical.VCGRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.ui.grid.MedicalGridLayer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import javax.swing.Timer;
import java.util.Random;

/**
 * Provides a high-quality VCG (Vectorcardiogram) demonstration panel.
 * <p>
 * Generates a realistic 3D vector loop showing the spatial cardiac vector
 * movement during the cardiac cycle. Uses the MedicalGridLayer for clinical visualization.
 * </p>
 */
public class VCGPanelProvider {

    private static final int POINTS = 480;
    private static final int SAMPLE_RATE = 60; // Hz

    public static ArberChartPanel create() {
        CircularFastMedicalModel model = new CircularFastMedicalModel(POINTS, 1);
        model.setName("VCG Loop");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 39);

        java.util.function.DoubleFunction<double[]> generateLoop = (phase) -> {
            double x;
            double y;

            if (phase < 0.08) {
                // P-loop (atrial depolarization) - small anterior loop
                double t = phase / 0.08;
                double angle = t * Math.PI * 0.8 - Math.PI * 0.1;
                x = 0.15 * Math.cos(angle);
                y = 0.08 * Math.sin(angle);
            } else if (phase < 0.15) {
                // PR segment (isoelectric)
                x = 0;
                y = 0;
            } else if (phase < 0.35) {
                // QRS loop (ventricular depolarization) - large counterclockwise loop
                double t = (phase - 0.15) / 0.20;

                // Q deflection
                if (t < 0.1) {
                    double qt = t / 0.1;
                    x = -0.1 * qt;
                    y = -0.05 * qt;
                }
                // R deflection (main loop)
                else if (t < 0.6) {
                    double rt = (t - 0.1) / 0.5;
                    double angle = -Math.PI * 0.3 + rt * Math.PI * 1.2;
                    x = 1.0 * Math.cos(angle) * (1 - 0.3 * rt);
                    y = 0.8 * Math.sin(angle);
                }
                // S deflection
                else {
                    double st = (t - 0.6) / 0.4;
                    x = -0.15 * (1 - st);
                    y = -0.2 * (1 - st);
                }
            } else if (phase < 0.45) {
                // ST segment
                double t = (phase - 0.35) / 0.10;
                x = 0.02 * Math.sin(t * Math.PI);
                y = 0.01 * Math.sin(t * Math.PI);
            } else if (phase < 0.75) {
                // T-loop (ventricular repolarization) - smaller loop, same direction as QRS
                double t = (phase - 0.45) / 0.30;
                double angle = -Math.PI * 0.2 + t * Math.PI * 0.9;
                x = 0.35 * Math.cos(angle);
                y = 0.25 * Math.sin(angle);
            } else {
                // TP segment (isoelectric)
                x = 0;
                y = 0;
            }

            // Add small physiological noise + respiratory modulation
            double resp = 1.0 + 0.05 * Math.sin(phase * 2 * Math.PI);
            double rot = 0.06 * Math.sin(phase * 2 * Math.PI);
            double xr = x * Math.cos(rot) - y * Math.sin(rot);
            double yr = x * Math.sin(rot) + y * Math.cos(rot);
            x = xr * resp + rand.nextGaussian() * 0.008;
            y = yr * resp + rand.nextGaussian() * 0.008;

            return new double[]{x, y};
        };

        final double[] phase = {0.0};
        for (int i = 0; i < POINTS; i++) {
            double[] sample = generateLoop.apply(phase[0]);
            model.add(sample[0], new double[]{sample[1]});
            phase[0] += 1.0 / POINTS;
        }

        ArberChartPanel panel = ArberChartBuilder.create()
                .withTitle("VCG - Vectorcardiogram Loop")
                .addLayer(model, new VCGRenderer())
                .withGridLayer(new MedicalGridLayer())
                .withTooltips(true)
                .withLegend(true)
                .build();

        Timer timer = new Timer(1000 / SAMPLE_RATE, e -> {
            double[] sample = generateLoop.apply(phase[0]);
            model.add(sample[0], new double[]{sample[1]});
            phase[0] += 1.0 / POINTS;
        });
        DemoPanelUtils.attachManagedTimer(panel, timer);

        return panel;
    }
}
