package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.circular.RadialBarRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;

public class RadialBarPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Weekly Activity Goals");
        model.addPoint(0, 78, 0, "Running (24 km)");
        model.addPoint(1, 62, 0, "Cycling (95 km)");
        model.addPoint(2, 88, 0, "Swimming (3.5 km)");
        model.addPoint(3, 54, 0, "Strength (4 sessions)");
        model.addPoint(4, 70, 0, "Mobility (5 sessions)");

        return ArberChartBuilder.create()
                .withTitle("Radial Bar - Weekly Goal Completion")
                .addLayer(model, new RadialBarRenderer())
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
