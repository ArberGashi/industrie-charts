package com.arbergashi.charts.rendererpanels.statistical;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.statistical.ConfidenceIntervalRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class ConfidenceIntervalPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Forecast Demand");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 108);
        for (int i = 0; i < 48; i++) {
            double mean = 120 + i * 1.6 + Math.sin(i * 0.22) * 8 + rand.nextGaussian() * 1.8;
            double ci = 6 + i * 0.06 + Math.sin(i * 0.11) * 2.0;
            model.addPoint(i, mean, mean - ci, mean + ci, 0, String.format("Week %d", i + 1));
        }
        return new ArberChartPanel(model, new ConfidenceIntervalRenderer());
    }
}
