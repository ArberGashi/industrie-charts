package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.circular.GaugeRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import java.awt.Color;
import java.util.List;

public class GaugeChartPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("API Error Budget");
        model.addPoint(0, 82.0, 0, "Remaining Budget: 82%");

        GaugeRenderer renderer = new GaugeRenderer();
        renderer.setRange(0, 100);
        renderer.setValue(82.0);
        renderer.setUnit("%");
        renderer.setBands(List.of(
                new GaugeRenderer.Band(0, 60, new Color(239, 68, 68)),
                new GaugeRenderer.Band(60, 80, new Color(245, 158, 11)),
                new GaugeRenderer.Band(80, 100, new Color(34, 197, 94))
        ));

        return ArberChartBuilder.create()
                .withTitle("Error Budget Remaining")
                .addLayer(model, renderer)
                .withTooltips(true)
                .build().withAnimations(true);
    }
}
