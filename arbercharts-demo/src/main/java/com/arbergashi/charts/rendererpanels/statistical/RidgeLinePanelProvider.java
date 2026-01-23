package com.arbergashi.charts.rendererpanels.statistical;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.statistical.RidgeLineRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class RidgeLinePanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Ridge Analysis");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 110);
        for (int series = 0; series < 6; series++) {
            double center = 12 + series * 9;
            double width = 4.5 + series * 0.4;
            double amplitude = 40 + series * 5;
            for (int i = 0; i < 60; i++) {
                double x = i;
                double dist = Math.exp(-Math.pow((i - center) / width, 2));
                double noise = rand.nextGaussian() * 0.6;
                model.addPoint(x, dist * amplitude + noise, 0, "Cohort " + (series + 1));
            }
        }
        return new ArberChartPanel(model, new RidgeLineRenderer());
    }
}
