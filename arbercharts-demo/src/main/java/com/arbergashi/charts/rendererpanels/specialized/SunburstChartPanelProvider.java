package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.specialized.SunburstRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;

public class SunburstChartPanelProvider {
    public static ArberChartPanel create() {
        // Company Budget Allocation – Hierarchical breakdown
        DefaultChartModel model = new DefaultChartModel("Budget Allocation (M$)");

        // Hierarchical labels: root/branch/leaf
        model.addPoint(0, 28.0, 0, "Engineering/Platform/Core Services");
        model.addPoint(1, 16.0, 0, "Engineering/Platform/Data Infra");
        model.addPoint(2, 21.0, 0, "Engineering/Product/Web");
        model.addPoint(3, 14.0, 0, "Engineering/Product/Mobile");
        model.addPoint(4, 12.0, 0, "Engineering/DevOps/Cloud Ops");
        model.addPoint(5, 7.0, 0, "Engineering/DevOps/Security");
        model.addPoint(6, 34.0, 0, "Sales/North America");
        model.addPoint(7, 26.0, 0, "Sales/Europe");
        model.addPoint(8, 14.0, 0, "Sales/APAC");
        model.addPoint(9, 18.0, 0, "Marketing/Digital");
        model.addPoint(10, 9.0, 0, "Marketing/Events");
        model.addPoint(11, 6.0, 0, "Marketing/Content");
        model.addPoint(12, 10.0, 0, "Operations/Support");
        model.addPoint(13, 6.0, 0, "Operations/Legal");

        return ArberChartBuilder.create()
                .withTitle("Company Budget Allocation FY2026")
                .addLayer(model, new SunburstRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
