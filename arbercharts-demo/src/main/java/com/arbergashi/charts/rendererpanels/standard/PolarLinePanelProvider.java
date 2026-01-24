package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.circular.PolarLineRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class PolarLinePanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Antenna Pattern");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 137);
        for (int i = 0; i <= 360; i += 3) {
            double mainLobe = 58 + 24 * Math.cos(Math.toRadians(i - 20));
            double sideLobes = 6 * Math.sin(Math.toRadians(i * 6));
            double r = mainLobe + sideLobes + rand.nextGaussian() * 1.5;
            model.addPoint(i, Math.max(5, r), 0, String.format("%d deg", i));
        }

        return ArberChartBuilder.create()
                .withTitle("Polar Line - Antenna Radiation Pattern")
                .addLayer(model, new PolarLineRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
