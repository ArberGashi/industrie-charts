package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.GanttResourceViewRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

public class GanttResourcePanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel alice = new DefaultChartModel("Alice");
        DefaultChartModel bob = new DefaultChartModel("Bob");
        DefaultChartModel charlie = new DefaultChartModel("Charlie");
        // GanttResourceViewRenderer: Y = resource index, min = start, max = end
        alice.addPoint(0, 0, 0, 8, 0, "API Design");
        alice.addPoint(0, 0, 10, 22, 0, "SDK Integration");
        bob.addPoint(0, 1, 4, 14, 0, "Data Pipeline");
        bob.addPoint(0, 1, 16, 26, 0, "Migration");
        charlie.addPoint(0, 2, 0, 12, 0, "UI Refresh");
        charlie.addPoint(0, 2, 14, 30, 0, "QA Sweep");

        return com.arbergashi.charts.api.ArberChartBuilder.create()
                .withTitle("Resource Management – Sprint Plan")
                .addLayer(alice, new GanttResourceViewRenderer())
                .addLayer(bob, new GanttResourceViewRenderer())
                .addLayer(charlie, new GanttResourceViewRenderer())
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
