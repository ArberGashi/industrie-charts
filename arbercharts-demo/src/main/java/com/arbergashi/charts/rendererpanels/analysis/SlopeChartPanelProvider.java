package com.arbergashi.charts.rendererpanels.analysis;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.analysis.SlopeRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

public class SlopeChartPanelProvider {
    public static ArberChartPanel create() {
        // Performance Comparison: Q3 2025 vs Q4 2025
        // Multiple product lines showing improvement trends

        DefaultChartModel productA = new DefaultChartModel("Product A");
        productA.addPoint(0, 68.5, 0, "Q3");
        productA.addPoint(1, 89.2, 0, "Q4");

        DefaultChartModel productB = new DefaultChartModel("Product B");
        productB.addPoint(0, 45.3, 0, "Q3");
        productB.addPoint(1, 78.6, 0, "Q4");

        DefaultChartModel productC = new DefaultChartModel("Product C");
        productC.addPoint(0, 82.1, 0, "Q3");
        productC.addPoint(1, 91.4, 0, "Q4");

        DefaultChartModel productD = new DefaultChartModel("Product D");
        productD.addPoint(0, 54.7, 0, "Q3");
        productD.addPoint(1, 63.2, 0, "Q4");

        DefaultChartModel productE = new DefaultChartModel("Product E");
        productE.addPoint(0, 72.4, 0, "Q3");
        productE.addPoint(1, 69.8, 0, "Q4");

        return ArberChartBuilder.create()
                .withTitle("Slope Chart - Quarterly Performance")
                .addLayer(productA, new SlopeRenderer())
                .addLayer(productB, new SlopeRenderer())
                .addLayer(productC, new SlopeRenderer())
                .addLayer(productD, new SlopeRenderer())
                .addLayer(productE, new SlopeRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
