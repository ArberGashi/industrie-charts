package com.arbergashi.charts.rendererpanels.statistical;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.statistical.HistogramRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class HistogramChartPanelProvider {
    public static ArberChartPanel create() {
        // Response Time Distribution – Cached, warm, and cold paths
        DefaultChartModel model = new DefaultChartModel("Response Time (ms)");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 101);
        
        // Cached responses (fast peak)
        for (int i = 0; i < 500; i++) {
            double rt = 14 + rand.nextGaussian() * 4.5;
            model.addPoint(Math.max(2, rt), 1, 0, String.format("%.1f ms (cached)", rt));
        }

        // Warm cache misses
        for (int i = 0; i < 320; i++) {
            double rt = 38 + rand.nextGaussian() * 9;
            model.addPoint(Math.max(15, rt), 1, 0, String.format("%.1f ms (warm)", rt));
        }

        // Cold path (DB + auth)
        for (int i = 0; i < 260; i++) {
            double rt = 110 + rand.nextGaussian() * 22;
            model.addPoint(Math.max(60, rt), 1, 0, String.format("%.1f ms (cold)", rt));
        }
        
        return ArberChartBuilder.create()
                .withTitle("Response Time Distribution – Multi-Modal")
                .addLayer(model, new HistogramRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
