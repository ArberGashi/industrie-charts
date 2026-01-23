package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.HorizonRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class HorizonPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Horizon Time Series");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 125);
        for (int i = 0; i < 500; i++) {
            double trend = i * 0.05;
            double seasonal = Math.sin(i * 0.06) * 40;
            double noise = rand.nextGaussian() * 8;
            double v = seasonal + trend + noise;
            model.addPoint(i, v, 0, String.format("t=%d", i));
        }
        return new ArberChartPanel(model, new HorizonRenderer());
    }
}
