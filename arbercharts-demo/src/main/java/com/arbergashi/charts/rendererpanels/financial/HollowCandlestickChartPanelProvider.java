package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.financial.CandlestickHollowRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;

public class HollowCandlestickChartPanelProvider {
    public static ArberChartPanel create() {
        DemoPanelUtils.PriceBundle bundle = DemoPanelUtils.generatePriceSeries(110, DemoPanelUtils.DEMO_SEED + 742);
        DefaultChartModel model = bundle.priceModel;
        model.setName("AURX Hollow Candles");

        return ArberChartBuilder.create()
                .withTitle("Hollow Candlestick - AURX Momentum Shift")
                .addLayer(model, new CandlestickHollowRenderer())
                .withTooltips(true)
                .withLegend(true)
                .build().withAnimations(true);
    }
}
