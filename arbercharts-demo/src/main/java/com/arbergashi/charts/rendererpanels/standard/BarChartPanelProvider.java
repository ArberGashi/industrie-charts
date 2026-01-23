package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.GroupedBarRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;

import java.util.Random;

public class BarChartPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel modelA = new DefaultChartModel("Enterprise Plan");
        DefaultChartModel modelB = new DefaultChartModel("Business Plan");
        String[] categories = {"North America", "Europe", "APAC", "LATAM", "MENA", "ANZ"};

        Random r = new Random(DemoPanelUtils.DEMO_SEED + 2);
        double[] base = {560, 470, 410, 240, 170, 150};

        for (int i = 0; i < categories.length; i++) {
            double a = base[i] + r.nextGaussian() * 28 + 35;
            double b = base[i] * 0.76 + r.nextGaussian() * 22;
            modelA.addPoint(i, a, 0, String.format("%s (Enterprise)", categories[i]));
            modelB.addPoint(i, b, 0, String.format("%s (Business)", categories[i]));
        }

        GroupedBarRenderer renderer = new GroupedBarRenderer();

        return ArberChartBuilder.create()
                .withTitle("Bar Chart - ARR by Region (k$)")
                .addLayer(modelA, renderer)
                .addLayer(modelB, renderer)
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
