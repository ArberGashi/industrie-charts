package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.circular.SemiDonutRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;

public class SemiDonutChartPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("SLO Compliance");
        model.addPoint(0, 96.2, 0, "Latency SLO");
        
        SemiDonutRenderer renderer = new SemiDonutRenderer();
        renderer.setValue(0.962);

        return ArberChartBuilder.create()
                .withTitle("Latency SLO Compliance")
                .addLayer(model, renderer)
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
