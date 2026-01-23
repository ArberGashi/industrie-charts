package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.circular.DonutRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;

public class DonutChartPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("AI Infrastructure Budget");
        model.addPoint(0, 40, 0, "GPU Compute - $104M (40%)");
        model.addPoint(1, 22, 0, "Data Storage - $57M (22%)");
        model.addPoint(2, 16, 0, "Networking - $42M (16%)");
        model.addPoint(3, 12, 0, "Power & Cooling - $31M (12%)");
        model.addPoint(4, 6, 0, "Security - $16M (6%)");
        model.addPoint(5, 4, 0, "Observability - $10M (4%)");

        DonutRenderer renderer = new DonutRenderer();
        renderer.setCenterText("$260M");
        renderer.setCenterSubText("Total Budget");

        return ArberChartBuilder.create()
                .withTitle("AI Infrastructure Investment 2026")
                .addLayer(model, renderer)
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
