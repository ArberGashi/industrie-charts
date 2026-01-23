package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.HeatmapRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class HeatmapPanelProvider {
    public static ArberChartPanel create() {
        // User Activity Heatmap – Website click density over screen regions
        DefaultChartModel model = new DefaultChartModel("Click Intensity");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 115);

        // Navigation bar hotspot (top)
        for (int i = 0; i < 260; i++) {
            double x = 18 + rand.nextGaussian() * 14;
            double y = 90 + rand.nextGaussian() * 4;
            model.addPoint(x, y, 0, "Navigation");
        }

        // Main CTA button (center-left)
        for (int i = 0; i < 360; i++) {
            double x = 35 + rand.nextGaussian() * 8;
            double y = 55 + rand.nextGaussian() * 10;
            model.addPoint(x, y, 0, "Primary CTA");
        }

        // Secondary content (center-right)
        for (int i = 0; i < 220; i++) {
            double x = 70 + rand.nextGaussian() * 12;
            double y = 50 + rand.nextGaussian() * 15;
            model.addPoint(x, y, 0, "Content");
        }

        // Search bar
        for (int i = 0; i < 180; i++) {
            double x = 55 + rand.nextGaussian() * 10;
            double y = 82 + rand.nextGaussian() * 3;
            model.addPoint(x, y, 0, "Search");
        }

        // Footer links
        for (int i = 0; i < 120; i++) {
            double x = 50 + rand.nextGaussian() * 25;
            double y = 10 + rand.nextGaussian() * 5;
            model.addPoint(x, y, 0, "Footer");
        }

        return ArberChartBuilder.create()
                .withTitle("User Activity Heatmap – Click Density")
                .addLayer(model, new HeatmapRenderer())
                .withTooltips(true)
                .withAnimations(true)
                .build();
    }
}
