package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.BaselineAreaRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;

public class BaselineAreaPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel regionEast = new DefaultChartModel("East");
        DefaultChartModel regionWest = new DefaultChartModel("West");
        DefaultChartModel regionNorth = new DefaultChartModel("North");

        for (int i = 0; i < 52; i++) {
            double promo = (i == 12 || i == 36) ? 12.0 : 0.0;
            double east = 7 * Math.sin(i * 0.18) + 4 * Math.cos(i * 0.05) + promo;
            double west = -5 * Math.sin(i * 0.14) + 3 * Math.cos(i * 0.03);
            double north = 5 * Math.sin(i * 0.22 + 0.8) - 2 * Math.cos(i * 0.07);

            regionEast.addPoint(i, east, 0, String.format("Week %d", i + 1));
            regionWest.addPoint(i, west, 0, String.format("Week %d", i + 1));
            regionNorth.addPoint(i, north, 0, String.format("Week %d", i + 1));
        }

        return ArberChartBuilder.create()
                .withTitle("Baseline Area - Variance vs Target (%)")
                .addLayer(regionEast, new BaselineAreaRenderer())
                .addLayer(regionWest, new BaselineAreaRenderer())
                .addLayer(regionNorth, new BaselineAreaRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
