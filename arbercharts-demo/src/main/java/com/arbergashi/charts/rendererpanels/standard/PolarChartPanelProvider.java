package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.circular.PolarRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class PolarChartPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Wind Speed (km/h)");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 10);

        String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                               "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};

        for (int i = 0; i < 16; i++) {
            int angle = i * 22 + 11; // 22.5 degree sectors
            // Simulate prevailing westerly winds
            double baseSpeed = 25 + 15 * Math.cos(Math.toRadians(angle - 270));
            double gust = rand.nextDouble() * 6;
            double speed = baseSpeed + gust;
            model.addPoint(angle, speed, 0, String.format("%s: %.1f km/h (gust %.1f)", directions[i], speed, gust));
        }

        return ArberChartBuilder.create()
                .withTitle("Wind Speed Distribution by Direction")
                .addLayer(model, new PolarRenderer())
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
