package com.arbergashi.charts.rendererpanels.statistical;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.statistical.KDERenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class KDEPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Kernel Density Estimation");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 103);
        // Mixture of normal + lognormal for realistic skew
        for (int i = 0; i < 650; i++) {
            double v;
            if (rand.nextDouble() < 0.7) {
                v = 32 + rand.nextGaussian() * 6;
            } else {
                v = 40 * Math.exp(rand.nextGaussian() * 0.35);
            }
            model.addPoint(v, v, 0, String.format("Sample %d", i + 1));
        }
        return new ArberChartPanel(model, new KDERenderer());
    }
}
