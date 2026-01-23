package com.arbergashi.charts.rendererpanels.medical;

import com.arbergashi.charts.model.FastMedicalModel;
import com.arbergashi.charts.render.medical.SpirometryRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.ui.grid.MedicalGridLayer;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

/**
 * Provides a high-quality Spirometry demonstration panel.
 * <p>
 * Generates a realistic Flow-Volume loop showing normal pulmonary function.
 * Uses the MedicalGridLayer for clinical-grade visualization.
 * </p>
 */
public class SpirometryChartPanelProvider {

    private static final int POINTS = 360;
    private static final double FVC = 4.5; // Forced Vital Capacity in liters
    private static final double PEF = 9.5; // Peak Expiratory Flow in L/s

    public static ArberChartPanel create() {
        FastMedicalModel model = new FastMedicalModel("Flow/Volume Loop", POINTS);
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 43);

        // Generate realistic Flow-Volume loop
        int expiratoryPoints = (int)(POINTS * 0.6);
        int inspiratoryPoints = POINTS - expiratoryPoints;

        // Expiratory phase (positive flow, volume decreasing from FVC to 0)
        for (int i = 0; i < expiratoryPoints; i++) {
            double volumeFraction = 1.0 - (double)i / expiratoryPoints;
            double volume = volumeFraction * FVC;

            double flow;
            if (i < expiratoryPoints * 0.15) {
                // Rapid rise to PEF
                double t = (double)i / (expiratoryPoints * 0.15);
                flow = PEF * Math.pow(t, 0.5);
            } else {
                // Effort-independent portion (linear decline)
                double t = ((double)i - expiratoryPoints * 0.15) / (expiratoryPoints * 0.85);
                flow = PEF * (1.0 - t * 0.85);
            }

            // Add small physiological noise
            double cough = (i == expiratoryPoints / 3) ? -1.2 : 0.0;
            flow += rand.nextGaussian() * 0.05 + cough;

            model.addPoint(volume, flow);
        }

        // Inspiratory phase (negative flow, volume increasing from 0 to FVC)
        for (int i = 0; i < inspiratoryPoints; i++) {
            double volumeFraction = (double)i / inspiratoryPoints;
            double volume = volumeFraction * FVC;

            // Inspiratory flow (effort-dependent, typically elliptical)
            double t = volumeFraction;
            double maxInspiratoryFlow = -6.0; // L/s (negative = inspiration)
            double flow = maxInspiratoryFlow * Math.sin(t * Math.PI);

            // Add small physiological noise
            flow += rand.nextGaussian() * 0.03;

            model.addPoint(volume, flow);
        }

        return ArberChartBuilder.create()
                .withTitle("Spirometry - Flow/Volume Loop")
                .addLayer(model, new SpirometryRenderer())
                .withGridLayer(new MedicalGridLayer())
                .withTooltips(true)
                .withLegend(true)
                .build();
    }
}
