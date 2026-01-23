package com.arbergashi.charts.rendererpanels.statistical;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.statistical.StatisticalErrorBarRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class StatisticalErrorBarPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Sensor Accuracy");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 107);
        for (int i = 0; i < 12; i++) {
            double x = i * 5.0;
            double y = 32 + i * 2.2 + rand.nextGaussian() * 1.8;
            double yErr = 2.5 + rand.nextDouble() * 4.0;
            double xErr = 1.0 + rand.nextDouble() * 1.8;
            model.addPoint(x, y, y - yErr, y + yErr, xErr, String.format("Sample %d", i + 1));
        }
        StatisticalErrorBarRenderer renderer = new StatisticalErrorBarRenderer();
        renderer.setShowHorizontal(true);
        renderer.setShowVertical(true);
        return new ArberChartPanel(model, renderer);
    }
}
