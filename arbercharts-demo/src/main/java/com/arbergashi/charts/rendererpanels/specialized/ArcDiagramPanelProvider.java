package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.ArcDiagramRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class ArcDiagramPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Interaction Network");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 119);
        String[] nodes = {
            "Gateway", "Auth", "Profile", "Search", "Catalog", "Cart",
            "Checkout", "Payments", "Fraud", "Shipping", "Notifications", "Analytics"
        };
        for (int i = 0; i < nodes.length; i++) {
            double jitter = rand.nextGaussian() * 1.5;
            model.addPoint(i * 10 + jitter, 0, 0, nodes[i]);
        }
        return new ArberChartPanel(model, new ArcDiagramRenderer());
    }
}
