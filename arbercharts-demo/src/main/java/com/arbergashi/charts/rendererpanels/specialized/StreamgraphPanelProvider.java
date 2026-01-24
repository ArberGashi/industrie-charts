package com.arbergashi.charts.rendererpanels.specialized;

import com.arbergashi.charts.model.DefaultMultiDimensionalChartModel;
import com.arbergashi.charts.render.specialized.StreamgraphRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StreamgraphPanelProvider {
    public static ArberChartPanel create() {
        List<String> series = List.of("Search", "Direct", "Social", "Email", "Referral");
        List<double[]> rows = new ArrayList<>();
        Random rand = new Random(DemoPanelUtils.DEMO_SEED + 116);

        for (int i = 0; i < 140; i++) {
            int day = i % 7;
            double weekend = (day == 5 || day == 6) ? -10.0 : 0.0;
            double base = 40 + Math.sin(i * 0.14) * 12 + weekend;
            double[] row = new double[series.size()];
            for (int s = 0; s < series.size(); s++) {
                double phase = i * (0.12 + s * 0.03);
                row[s] = base + 8 * Math.sin(phase) + s * 4 + rand.nextGaussian() * 2.5;
            }
            rows.add(row);
        }

        DefaultMultiDimensionalChartModel model = new DefaultMultiDimensionalChartModel(rows, series);

        return ArberChartBuilder.create()
                .withTitle("Traffic Mix – Streamgraph Overview")
                .withTooltips(true)
                .withLegend(true)
                .addLayer(model, new StreamgraphRenderer())
                .build()
                .withAnimations(true);
    }
}
