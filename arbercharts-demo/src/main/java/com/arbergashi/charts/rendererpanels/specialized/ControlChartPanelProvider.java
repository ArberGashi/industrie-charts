package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.ControlChartRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class ControlChartPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Manufacturing Quality Control");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 124);
        double mean = 50.0;
        for (int i = 0; i < 80; i++) {
            double drift = (i > 40) ? 0.04 * (i - 40) : 0.0;
            double val = mean + drift + rand.nextGaussian() * 1.8;
            if (i == 26 || i == 58) val += 9.0;
            if (i == 71) val -= 7.5;
            model.addPoint(i, val, 0, String.format("Lot %d", i + 1));
        }
        return new ArberChartPanel(model, new ControlChartRenderer());
    }
}
