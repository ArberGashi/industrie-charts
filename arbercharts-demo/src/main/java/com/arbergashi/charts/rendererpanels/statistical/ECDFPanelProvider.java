package com.arbergashi.charts.rendererpanels.statistical;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.statistical.ECDFRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.Random;

public class ECDFPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel model = new DefaultChartModel("Empirical CDF");
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 105);
        for (int i = 0; i < 280; i++) {
            double v = 35 * Math.exp(rand.nextGaussian() * 0.25) + rand.nextGaussian() * 2.0;
            model.addPoint(v, v, 0, String.format("Latency %.1fms", v));
        }

        return ArberChartBuilder.create()
                .withTitle("Empirical CDF – Latency Samples")
                .addLayer(model, new ECDFRenderer())
                .build();
    }
}
