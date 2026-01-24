package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.circular.RadarRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;

public class RadarChartPanelProvider {
    public static ArberChartPanel create() {
        // Team Performance Assessment - Multi-dimensional evaluation
        DefaultChartModel teamA = new DefaultChartModel("Team Alpha");
        DefaultChartModel teamB = new DefaultChartModel("Team Beta");
        DefaultChartModel teamC = new DefaultChartModel("Team Gamma");

        String[] dimensions = {"Innovation", "Quality", "Speed", "Collaboration", "Reliability", "Cost Efficiency"};
        double[] valuesA = {92, 85, 78, 88, 90, 72};
        double[] valuesB = {75, 90, 85, 70, 82, 88};

        for (int i = 0; i < dimensions.length; i++) {
            teamA.addPoint(i, valuesA[i], 0, dimensions[i]);
            teamB.addPoint(i, valuesB[i], 0, dimensions[i]);
            teamC.addPoint(i, 70 + (i * 4) % 18, 0, dimensions[i]);
        }

        return ArberChartBuilder.create()
                .withTitle("Team Performance Assessment")
                .addLayer(teamA, new RadarRenderer())
                .addLayer(teamB, new RadarRenderer())
                .addLayer(teamC, new RadarRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
