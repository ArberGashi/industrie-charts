package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.circular.NightingaleRoseRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;

public class NightingaleRosePanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Revenue (M$)");
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                           "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        double[] values = {14.0, 13.2, 16.8, 19.1, 18.4, 20.6,
                           24.2, 22.7, 19.5, 17.3, 23.6, 21.1};

        for (int i = 0; i < months.length; i++) {
            model.addPoint(i, values[i], 0, String.format("%s: $%.1fM", months[i], values[i]));
        }

        return ArberChartBuilder.create()
                .withTitle("Monthly Revenue Distribution 2026")
                .addLayer(model, new NightingaleRoseRenderer())
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
