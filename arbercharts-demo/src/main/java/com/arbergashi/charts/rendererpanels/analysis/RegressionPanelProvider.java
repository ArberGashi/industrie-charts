package com.arbergashi.charts.rendererpanels.analysis;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.analysis.RegressionLineRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

import java.util.Random;

public class RegressionPanelProvider {
    public static ArberChartPanel create() {
        // Sales Revenue Trend Analysis - Monthly revenue over 2 years.
        // Deterministic dataset for reproducible screenshots and QA.
        DefaultChartModel model = new DefaultChartModel("Monthly Revenue (k€)");

        double baseRevenue = 250.0;
        double growthRate = 8.0;

        Random rnd = new Random(2025);

        for (int month = 0; month < 24; month++) {
            double saturation = (month > 18) ? (month - 18) * 3.0 : 0.0;
            double trendRevenue = baseRevenue + growthRate * month - saturation;

            int quarter = (month % 12) / 3;
            double seasonalFactor = switch (quarter) {
                case 0 -> 0.92;
                case 1 -> 1.05;
                case 2 -> 0.98;
                case 3 -> 1.15;
                default -> 1.0;
            };

            double marketNoise = (rnd.nextDouble() - 0.5) * 35.0;

            double campaignBoost = 0.0;
            if (month == 6 || month == 18) {
                campaignBoost = 45.0;
            } else if (month == 12) {
                campaignBoost = 60.0;
            }

            double actualRevenue = trendRevenue * seasonalFactor + marketNoise + campaignBoost;
            actualRevenue = Math.max(actualRevenue, 150.0);

            model.addPoint(month, actualRevenue, 0, String.format("M%d", month + 1));
        }

        RegressionLineRenderer renderer = new RegressionLineRenderer();

        return ArberChartBuilder.create()
                .withTitle("Linear Regression - Revenue Trend")
                .addLayer(model, renderer)
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
