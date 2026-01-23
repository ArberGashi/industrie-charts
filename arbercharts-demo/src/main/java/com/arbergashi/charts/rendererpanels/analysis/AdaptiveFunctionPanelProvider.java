package com.arbergashi.charts.rendererpanels.analysis;

import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.analysis.AdaptiveFunctionRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

public class AdaptiveFunctionPanelProvider {
    public static ArberChartPanel create() {
        // Complex function with varying curvature to demonstrate adaptive sampling quality.
        // We add a few anchor points to define a stable domain and scale for the panel.
        DefaultChartModel anchors = new DefaultChartModel("f(x) = sin(x^2) * cos(x)");

        double minX = -3.5;
        double maxX = 3.5;
        double step = (maxX - minX) / 28.0;

        for (double x = minX; x <= maxX + 1e-9; x += step) {
            double y = Math.sin(x * x) * Math.cos(x);
            anchors.addPoint(x, y, 0, String.format("x=%.2f", x));
        }

        AdaptiveFunctionRenderer renderer = new AdaptiveFunctionRenderer(x -> Math.sin(x * x) * Math.cos(x));

        return ArberChartBuilder.create()
                .withTitle("Adaptive Function - High-Curvature Sampling")
                .addLayer(anchors, renderer)
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
