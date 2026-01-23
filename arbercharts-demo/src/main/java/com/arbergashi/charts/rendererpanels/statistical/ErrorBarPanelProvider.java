package com.arbergashi.charts.rendererpanels.statistical;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.statistical.ErrorBarRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class ErrorBarPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Calibration Runs");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 106);
        for (int i = 0; i < 12; i++) {
            double mean = 48 + i * 1.6 + rand.nextGaussian() * 2.0;
            double error = Math.max(1.5, 6.5 - i * 0.3 + rand.nextGaussian() * 0.4);
            model.addPoint(i, mean, mean - error, mean + error, 0, String.format("Run %d", i + 1));
        }
        return new ArberChartPanel(model, new ErrorBarRenderer());
    }
}
