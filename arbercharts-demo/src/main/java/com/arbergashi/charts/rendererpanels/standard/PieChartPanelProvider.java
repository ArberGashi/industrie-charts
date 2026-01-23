package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.circular.PieRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;

public class PieChartPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Cloud Market Share 2026");
        model.addPoint(0, 32.1, 0, "Amazon AWS");
        model.addPoint(1, 24.3, 0, "Microsoft Azure");
        model.addPoint(2, 11.4, 0, "Google Cloud");
        model.addPoint(3, 4.1, 0, "Alibaba Cloud");
        model.addPoint(4, 3.0, 0, "Oracle Cloud");
        model.addPoint(5, 2.5, 0, "IBM Cloud");
        model.addPoint(6, 1.9, 0, "Tencent Cloud");
        model.addPoint(7, 20.7, 0, "Others");

        PieRenderer renderer = new PieRenderer();

        return ArberChartBuilder.create()
                .withTitle("Cloud Infrastructure Market Share 2026")
                .addLayer(model, renderer)
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
