package com.arbergashi.charts.rendererpanels.statistical;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.statistical.QQPlotRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class QQPlotPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Normal Q-Q Plot");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 104);
        for (int i = 0; i < 180; i++) {
            double v = rand.nextGaussian();
            if (rand.nextDouble() < 0.08) {
                v *= 4.0 + rand.nextDouble() * 2.0; // heavy tails
            }
            model.addPoint(i, v, 0, String.format("Sample %d", i + 1));
        }
        return new ArberChartPanel(model, new QQPlotRenderer());
    }
}
