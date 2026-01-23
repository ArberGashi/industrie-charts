package com.arbergashi.charts.rendererpanels.statistical;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.statistical.ViolinPlotRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class ViolinPlotPanelProvider {
    public static ArberChartPanel create() {
        // A/B Test Results – Conversion rate distribution across test variants
        DefaultChartModel model = new DefaultChartModel("Conversion Rate (%)");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 102);

        String[] variants = {"Control", "Variant A", "Variant B", "Variant C", "Variant D"};
        double[] baselines = {3.0, 3.7, 4.4, 3.6, 4.9};

        for (int i = 0; i < variants.length; i++) {
            double median = baselines[i] + rand.nextDouble() * 0.4;
            double spread = 0.7 + rand.nextDouble() * 0.5;
            double min = Math.max(0.5, median - spread - rand.nextDouble() * 0.3);
            double max = median + spread + rand.nextDouble() * 0.5;
            model.addPoint(i, median, min, max, 0,
                String.format("%s: %.2f%% (median)", variants[i], median));
        }

        return ArberChartBuilder.create()
                .withTitle("A/B Test Conversion Rate Distribution")
                .addLayer(model, new ViolinPlotRenderer())
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
