package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.financial.GanttRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;

public class GanttPanelProvider {
    public static ArberChartPanel create() {
        // Software Development Project Timeline
        DefaultChartModel discovery = new DefaultChartModel("Discovery");
        DefaultChartModel build = new DefaultChartModel("Build");
        DefaultChartModel design = new DefaultChartModel("Design");
        DefaultChartModel quality = new DefaultChartModel("Quality");
        DefaultChartModel launch = new DefaultChartModel("Launch");
        // GanttRenderer: X = start, Y = task index, weight = duration
        discovery.addPoint(0, 0, 6, "Discovery & Requirements");
        design.addPoint(6, 1, 7, "Architecture Design");
        build.addPoint(12, 2, 16, "Core Platform Build");
        design.addPoint(14, 3, 14, "UX + Frontend");
        quality.addPoint(26, 4, 9, "Integration Testing");
        quality.addPoint(33, 5, 6, "Security Review");
        launch.addPoint(36, 6, 5, "Launch Readiness");
        launch.addPoint(41, 7, 7, "Post-Launch Support");

        return ArberChartBuilder.create()
                .withTitle("Software Project Timeline – H1 2026")
                .addLayer(discovery, new GanttRenderer())
                .addLayer(build, new GanttRenderer())
                .addLayer(design, new GanttRenderer())
                .addLayer(quality, new GanttRenderer())
                .addLayer(launch, new GanttRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
