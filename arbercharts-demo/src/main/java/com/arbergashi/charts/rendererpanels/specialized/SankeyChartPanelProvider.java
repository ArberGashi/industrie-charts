package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultFlowChartModel;
import com.arbergashi.charts.render.specialized.SankeyRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import java.util.List;

public class SankeyChartPanelProvider {
    public static ArberChartPanel create() {
        // Energy Flow – Power generation to consumption sectors
        List<DefaultFlowChartModel.DefaultNode> nodes = List.of(
            new DefaultFlowChartModel.DefaultNode("solar", "Solar (140 GW)"),
            new DefaultFlowChartModel.DefaultNode("wind", "Wind (210 GW)"),
            new DefaultFlowChartModel.DefaultNode("nuclear", "Nuclear (100 GW)"),
            new DefaultFlowChartModel.DefaultNode("gas", "Natural Gas (155 GW)"),
            new DefaultFlowChartModel.DefaultNode("hydro", "Hydro (65 GW)"),
            new DefaultFlowChartModel.DefaultNode("grid", "Power Grid"),
            new DefaultFlowChartModel.DefaultNode("residential", "Residential"),
            new DefaultFlowChartModel.DefaultNode("commercial", "Commercial"),
            new DefaultFlowChartModel.DefaultNode("industrial", "Industrial"),
            new DefaultFlowChartModel.DefaultNode("transport", "Transport"),
            new DefaultFlowChartModel.DefaultNode("losses", "T&D Losses")
        );

        List<DefaultFlowChartModel.DefaultLink> links = List.of(
            new DefaultFlowChartModel.DefaultLink("solar", "grid", 140),
            new DefaultFlowChartModel.DefaultLink("wind", "grid", 210),
            new DefaultFlowChartModel.DefaultLink("nuclear", "grid", 100),
            new DefaultFlowChartModel.DefaultLink("gas", "grid", 155),
            new DefaultFlowChartModel.DefaultLink("hydro", "grid", 65),
            new DefaultFlowChartModel.DefaultLink("grid", "residential", 210),
            new DefaultFlowChartModel.DefaultLink("grid", "commercial", 170),
            new DefaultFlowChartModel.DefaultLink("grid", "industrial", 185),
            new DefaultFlowChartModel.DefaultLink("grid", "transport", 35),
            new DefaultFlowChartModel.DefaultLink("grid", "losses", 40)
        );

        DefaultFlowChartModel model = new DefaultFlowChartModel(nodes, links);

        return ArberChartBuilder.create()
                .withTitle("Energy Flow – Power Generation to Consumption")
                .addLayer(model, new SankeyRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
