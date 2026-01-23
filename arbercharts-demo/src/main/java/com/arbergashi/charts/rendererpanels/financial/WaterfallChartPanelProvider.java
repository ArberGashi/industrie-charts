package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.financial.WaterfallRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;

public class WaterfallChartPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Cash Flow");
        model.addPoint(0, 6200, 0, "Opening Cash");
        model.addPoint(1, 1580, 0, "Subscription Revenue");
        model.addPoint(2, 620, 0, "Services Revenue");
        model.addPoint(3, -880, 0, "COGS");
        model.addPoint(4, -540, 0, "Marketing");
        model.addPoint(5, -420, 0, "R and D");
        model.addPoint(6, -460, 0, "Ops and Support");
        model.addPoint(7, -350, 0, "Taxes");
        model.addPoint(8, 0, 1, "Closing Cash"); // Weight 1 marks total

        return ArberChartBuilder.create()
                .withTitle("Waterfall - Quarterly Cash Flow (USDk)")
                .addLayer(model, new WaterfallRenderer())
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
