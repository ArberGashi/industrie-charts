package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.VoronoiRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class VoronoiPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Spatial Partitioning");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 117);
        for (int i = 0; i < 20; i++) {
            model.addPoint(25 + rand.nextGaussian() * 9, 30 + rand.nextGaussian() * 8, 0, "Cluster A");
        }
        for (int i = 0; i < 20; i++) {
            model.addPoint(70 + rand.nextGaussian() * 9, 70 + rand.nextGaussian() * 8, 0, "Cluster B");
        }
        for (int i = 0; i < 10; i++) {
            model.addPoint(50 + rand.nextGaussian() * 12, 50 + rand.nextGaussian() * 12, 0, "Bridge");
        }
        return new ArberChartPanel(model, new VoronoiRenderer());
    }
}
