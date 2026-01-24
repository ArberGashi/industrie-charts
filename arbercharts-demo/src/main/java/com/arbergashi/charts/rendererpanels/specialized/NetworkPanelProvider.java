package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultFlowChartModel;
import com.arbergashi.charts.render.specialized.NetworkRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import java.util.List;

public class NetworkPanelProvider {
    public static ArberChartPanel create() {
        // Enterprise Network Topology – Data center infrastructure
        List<DefaultFlowChartModel.DefaultNode> nodes = List.of(
            new DefaultFlowChartModel.DefaultNode("fw", "Firewall"),
            new DefaultFlowChartModel.DefaultNode("lb", "Load Balancer"),
            new DefaultFlowChartModel.DefaultNode("web1", "Web Server 1"),
            new DefaultFlowChartModel.DefaultNode("web2", "Web Server 2"),
            new DefaultFlowChartModel.DefaultNode("api1", "API Gateway"),
            new DefaultFlowChartModel.DefaultNode("db1", "Primary DB"),
            new DefaultFlowChartModel.DefaultNode("db2", "Replica DB"),
            new DefaultFlowChartModel.DefaultNode("cache", "Redis Cache"),
            new DefaultFlowChartModel.DefaultNode("queue", "Message Queue")
        );

        List<DefaultFlowChartModel.DefaultLink> links = List.of(
            new DefaultFlowChartModel.DefaultLink("fw", "lb", 95.0),
            new DefaultFlowChartModel.DefaultLink("lb", "web1", 55.0),
            new DefaultFlowChartModel.DefaultLink("lb", "web2", 40.0),
            new DefaultFlowChartModel.DefaultLink("web1", "api1", 35.0),
            new DefaultFlowChartModel.DefaultLink("web2", "api1", 30.0),
            new DefaultFlowChartModel.DefaultLink("api1", "db1", 28.0),
            new DefaultFlowChartModel.DefaultLink("api1", "cache", 40.0),
            new DefaultFlowChartModel.DefaultLink("api1", "queue", 18.0),
            new DefaultFlowChartModel.DefaultLink("db1", "db2", 15.0)
        );

        DefaultFlowChartModel model = new DefaultFlowChartModel(nodes, links);

        return ArberChartBuilder.create()
                .withTitle("Enterprise Network Topology")
                .addLayer(model, new NetworkRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
