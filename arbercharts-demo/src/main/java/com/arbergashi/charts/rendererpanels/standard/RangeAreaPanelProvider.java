package com.arbergashi.charts.rendererpanels.standard;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.standard.RangeRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;

public class RangeAreaPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel berlin = new DefaultChartModel("Berlin");
        DefaultChartModel london = new DefaultChartModel("London");
        DefaultChartModel paris = new DefaultChartModel("Paris");

        for (int i = 0; i < 21; i++) {
            double heatwave = (i >= 12 && i <= 15) ? 6.0 : 0.0;
            double avgBerlin = 18 + Math.sin(i * 0.25) * 7 + heatwave;
            double avgLondon = 16 + Math.sin(i * 0.23 + 0.4) * 6;
            double avgParis = 20 + Math.sin(i * 0.27 + 0.8) * 7 + heatwave * 0.6;

            berlin.addPoint(i, avgBerlin, avgBerlin - 6, avgBerlin + 6, 0, String.format("Day %d", i + 1));
            london.addPoint(i, avgLondon, avgLondon - 5, avgLondon + 5, 0, String.format("Day %d", i + 1));
            paris.addPoint(i, avgParis, avgParis - 6, avgParis + 6, 0, String.format("Day %d", i + 1));
        }

        return ArberChartBuilder.create()
                .withTitle("Range Area - 3-Week Temperature Range")
                .addLayer(berlin, new RangeRenderer())
                .addLayer(london, new RangeRenderer())
                .addLayer(paris, new RangeRenderer())
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
