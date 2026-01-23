package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.model.DefaultChartModel;
import com.arbergashi.charts.render.financial.MACDRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;
import com.arbergashi.charts.api.ArberChartBuilder;
import com.arbergashi.charts.rendererpanels.DemoPanelUtils;

public class MACDChartPanelProvider {
    public static ArberChartPanel create() {
        DefaultChartModel priceModel = DemoPanelUtils.generateRegimeCloseSeries(
                "AURX Daily Close", 220, DemoPanelUtils.DEMO_SEED + 888, 176.0);

        return ArberChartBuilder.create()
                .withTitle("MACD - AURX Daily")
                .addLayer(priceModel, new MACDRenderer())
                .withTooltips(true)
                .withLegend(true)
                .withAnimations(true)
                .build();
    }
}
