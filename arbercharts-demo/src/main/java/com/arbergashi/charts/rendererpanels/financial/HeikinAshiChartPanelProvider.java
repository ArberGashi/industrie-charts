package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.financial.HeikinAshiRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;

public class HeikinAshiChartPanelProvider {
    public static ArberChartPanel create() {
        DemoPanelUtils.PriceBundle bundle = DemoPanelUtils.generatePriceSeries(140, DemoPanelUtils.DEMO_SEED + 710);
        DefaultChartModel model = bundle.priceModel;
        model.setName("AURX Heikin-Ashi");

        return ArberChartBuilder.create()
                .withTitle("Heikin-Ashi - AURX Trend View")
                .addLayer(model, new HeikinAshiRenderer())
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
