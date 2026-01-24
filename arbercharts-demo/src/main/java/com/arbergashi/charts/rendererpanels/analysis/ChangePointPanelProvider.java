package com.arbergashi.charts.rendererpanels.analysis;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.analysis.ChangePointRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

import java.util.Random;

public class ChangePointPanelProvider {
    public static ArberChartPanel create() {
        // Manufacturing energy consumption - before/after efficiency upgrade.
        // Deterministic variation for reproducible presentation.
        DefaultChartModel model = new DefaultChartModel("Energy Consumption (kWh/day)");

        Random rnd = new Random(60);

        // Phase 1: Old equipment (Days 0-59)
        double oldBaselineMid = 850.0;
        for (int day = 0; day < 60; day++) {
            double weekdayFactor = (day % 7 < 5) ? 1.0 : 0.85;
            double consumption = oldBaselineMid;
            consumption += Math.sin(day * 0.3) * 25.0;
            consumption += (rnd.nextDouble() - 0.5) * 40.0;
            consumption *= weekdayFactor;
            model.addPoint(day, consumption, 0, "Day " + (day + 1));
        }

        // Change point: upgrade on Day 60

        // Phase 2: New equipment (Days 60-119)
        double newBaselineMid = 620.0;
        for (int day = 60; day < 120; day++) {
            double weekdayFactor = (day % 7 < 5) ? 1.0 : 0.85;
            double consumption = newBaselineMid;
            consumption += Math.sin(day * 0.3) * 20.0;
            consumption += (rnd.nextDouble() - 0.5) * 30.0;
            consumption *= weekdayFactor;
            String label = (day == 60) ? "Day 61 (upgrade)" : "Day " + (day + 1);
            model.addPoint(day, consumption, 0, label);
        }

        ChangePointRenderer renderer = new ChangePointRenderer();

        return ArberChartBuilder.create()
                .withTitle("Change Point Detection - Efficiency Upgrade")
                .addLayer(model, renderer)
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
