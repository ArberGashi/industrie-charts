package com.arbergashi.charts.rendererpanels.statistical;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.HexbinRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class HexbinPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Hexagonal Binning");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 111);
        for (int i = 0; i < 1400; i++) {
            double x = 35 + rand.nextGaussian() * 12;
            double y = 55 + rand.nextGaussian() * 10;
            model.addPoint(x, y, 0, "Cluster A");
        }
        for (int i = 0; i < 900; i++) {
            double x = 70 + rand.nextGaussian() * 14;
            double y = 35 + rand.nextGaussian() * 9;
            model.addPoint(x, y, 0, "Cluster B");
        }
        for (int i = 0; i < 300; i++) {
            double x = 15 + rand.nextGaussian() * 6;
            double y = 25 + rand.nextGaussian() * 6;
            model.addPoint(x, y, 0, "Noise");
        }
        return new ArberChartPanel(model, new HexbinRenderer());
    }
}
