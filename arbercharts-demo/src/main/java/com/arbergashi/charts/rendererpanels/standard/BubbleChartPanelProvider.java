package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.BubbleRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class BubbleChartPanelProvider {
    public static ArberChartPanel create() {
        // Market Opportunity Analysis - Revenue potential by market segment
        // X = Market Size (B$), Y = Growth Rate (%), Bubble = Investment Required (M$)
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 6);

        String[] markets = {
            "Enterprise SaaS", "SMB Tools", "Consumer Apps", "Healthcare Tech",
            "FinTech", "AI Infrastructure", "Cybersecurity"
        };
        double[] baseSizes = {45, 28, 72, 35, 55, 60, 38};
        double[] baseGrowth = {18, 25, 12, 32, 22, 28, 20};
        double[] baseInvestment = {120, 45, 85, 150, 180, 220, 95};

        DefaultChartModel[] models = new DefaultChartModel[markets.length];
        for (int i = 0; i < markets.length; i++) {
            models[i] = new DefaultChartModel(markets[i]);
            double size = baseSizes[i] + rand.nextGaussian() * 3;
            double growth = baseGrowth[i] + rand.nextGaussian() * 2;
            double investment = baseInvestment[i] + rand.nextGaussian() * 15;
            models[i].addPoint(size, growth, investment,
                String.format("%s: $%.1fB size, %.1f%% growth, $%.0fM capex", markets[i], size, growth, investment));
        }

        ArberChartBuilder builder = ArberChartBuilder.create()
                .withTitle("Bubble Chart - Market Opportunity");

        for (DefaultChartModel model : models) {
            builder.addLayer(model, new BubbleRenderer());
        }

        return builder
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
