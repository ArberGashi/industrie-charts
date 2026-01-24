package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.circular.GaugeBandsRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;

public class GaugeBandsChartPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Service Health");
        model.addPoint(0, 94.3, 0, "SLO Score");

        GaugeBandsRenderer renderer = new GaugeBandsRenderer();

        return ArberChartBuilder.create()
                .withTitle("Service Health Score")
                .addLayer(model, renderer)
                .withTooltips(true)
                .build().withAnimations(true);
    }
}
