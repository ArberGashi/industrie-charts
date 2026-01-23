package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.render.financial.ATRRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

public class ATRChartPanelProvider {
    public static ArberChartPanel create() {
        return FinancialIndicatorPanelProvider.create(
                new ATRRenderer(),
                "ATR - AURX Average True Range",
                false);
    }
}
