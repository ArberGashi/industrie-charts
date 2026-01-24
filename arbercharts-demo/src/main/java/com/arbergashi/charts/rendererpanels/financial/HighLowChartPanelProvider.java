package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.financial.HighLowRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;

public class HighLowChartPanelProvider {
    public static ArberChartPanel create() {
        DemoPanelUtils.PriceBundle bundle = DemoPanelUtils.generatePriceSeriesMinimal(90, DemoPanelUtils.DEMO_SEED + 709);
        DefaultChartModel model = bundle.priceModel;
        model.setName("AURX Daily Range");

        return ArberChartBuilder.create()
                .withTitle("High/Low - AURX Daily Range")
                .addLayer(model, new HighLowRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
