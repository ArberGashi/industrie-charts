package com.arbergashi.charts.rendererpanels.statistical;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.statistical.BandRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class BandPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Service Latency Band");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 109);
        for (int i = 0; i < 52; i++) {
            double base = 180 + Math.sin(i * 0.22) * 12 + rand.nextGaussian() * 3;
            double low = base - 18 - rand.nextDouble() * 6;
            double high = base + 18 + rand.nextDouble() * 6;
            model.addPoint(i, base, low, high, 0, String.format("Week %d", i + 1));
        }
        return new ArberChartPanel(model, new BandRenderer());
    }
}
