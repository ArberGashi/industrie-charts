package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.financial.CandlestickRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;

public class CandlestickChartPanelProvider {
    public static ArberChartPanel create() {
        DemoPanelUtils.PriceBundle bundle = DemoPanelUtils.generatePriceSeriesMinimal(120, DemoPanelUtils.DEMO_SEED + 701);
        DefaultChartModel model = bundle.priceModel;
        model.setName("AURX Daily OHLC");

        return ArberChartBuilder.create()
                .withTitle("Candlestick - AURX Daily OHLC")
                .addLayer(model, new CandlestickRenderer())
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
