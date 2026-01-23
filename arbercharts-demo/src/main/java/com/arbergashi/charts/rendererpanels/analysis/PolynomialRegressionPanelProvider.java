package com.arbergashi.charts.rendererpanels.analysis;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.analysis.PolynomialRegressionRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

import java.util.Random;

public class PolynomialRegressionPanelProvider {
    public static ArberChartPanel create() {
        // Software project costs - quadratic complexity/technical debt growth.
        // Deterministic variation for stable demo output.
        DefaultChartModel model = new DefaultChartModel("Development Cost (k€/month)");

        Random rnd = new Random(42);

        for (int month = 0; month < 24; month++) {
            double baseCost = 25.0;
            double linearGrowth = 2.0 * month;
            double quadraticGrowth = 0.15 * month * month;

            double theoreticalCost = baseCost + linearGrowth + quadraticGrowth;

            double seasonalEffect = Math.sin(month * Math.PI / 6.0) * 3.0;
            double randomVariation = (rnd.nextDouble() - 0.5) * 8.0;

            double inflation = 1.0 + month * 0.004;
            double actualCost = Math.max((theoreticalCost + seasonalEffect + randomVariation) * inflation, 15.0);

            model.addPoint(month, actualCost, 0, String.format("Month %d", month + 1));
        }

        PolynomialRegressionRenderer renderer = new PolynomialRegressionRenderer();

        return ArberChartBuilder.create()
                .withTitle("Polynomial Regression - Cost Trajectory")
                .addLayer(model, renderer)
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
