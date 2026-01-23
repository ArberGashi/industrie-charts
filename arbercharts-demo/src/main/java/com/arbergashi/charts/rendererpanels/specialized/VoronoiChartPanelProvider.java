package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.VoronoiRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class VoronoiChartPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Voronoi Partitioning");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 133);
        for (int i = 0; i < 18; i++) {
            model.addPoint(20 + rand.nextGaussian() * 8, 70 + rand.nextGaussian() * 6, 0, "North Cluster");
        }
        for (int i = 0; i < 14; i++) {
            model.addPoint(70 + rand.nextGaussian() * 9, 35 + rand.nextGaussian() * 7, 0, "South Cluster");
        }
        for (int i = 0; i < 8; i++) {
            model.addPoint(45 + rand.nextGaussian() * 10, 50 + rand.nextGaussian() * 10, 0, "Bridge");
        }
        return new ArberChartPanel(model, new VoronoiRenderer());
    }
}
