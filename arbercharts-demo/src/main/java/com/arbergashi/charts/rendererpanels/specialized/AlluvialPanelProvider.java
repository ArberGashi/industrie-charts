package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultFlowChartModel;
import com.arbergashi.charts.render.specialized.AlluvialRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import java.util.List;

public class AlluvialPanelProvider {
    public static ArberChartPanel create() {
        List<DefaultFlowChartModel.DefaultNode> nodes = List.of(
            new DefaultFlowChartModel.DefaultNode("s1", "Q1: Starter"),
            new DefaultFlowChartModel.DefaultNode("p1", "Q1: Pro"),
            new DefaultFlowChartModel.DefaultNode("e1", "Q1: Enterprise"),
            new DefaultFlowChartModel.DefaultNode("s2", "Q2: Starter"),
            new DefaultFlowChartModel.DefaultNode("p2", "Q2: Pro"),
            new DefaultFlowChartModel.DefaultNode("e2", "Q2: Enterprise"),
            new DefaultFlowChartModel.DefaultNode("s3", "Q3: Starter"),
            new DefaultFlowChartModel.DefaultNode("p3", "Q3: Pro"),
            new DefaultFlowChartModel.DefaultNode("e3", "Q3: Enterprise")
        );

        List<DefaultFlowChartModel.DefaultLink> links = List.of(
            new DefaultFlowChartModel.DefaultLink("s1", "s2", 45),
            new DefaultFlowChartModel.DefaultLink("s1", "p2", 12),
            new DefaultFlowChartModel.DefaultLink("p1", "p2", 28),
            new DefaultFlowChartModel.DefaultLink("p1", "e2", 10),
            new DefaultFlowChartModel.DefaultLink("e1", "e2", 18),
            new DefaultFlowChartModel.DefaultLink("s2", "s3", 40),
            new DefaultFlowChartModel.DefaultLink("s2", "p3", 14),
            new DefaultFlowChartModel.DefaultLink("p2", "p3", 26),
            new DefaultFlowChartModel.DefaultLink("p2", "e3", 12),
            new DefaultFlowChartModel.DefaultLink("e2", "e3", 20)
        );

        DefaultFlowChartModel model = new DefaultFlowChartModel(nodes, links);
        return new ArberChartPanel(model, new AlluvialRenderer());
    }
}
