package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.render.financial.BollingerBandsRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

public class BollingerBandsChartPanelProvider {
    public static ArberChartPanel create() {
        return FinancialIndicatorPanelProvider.create(
                new BollingerBandsRenderer(),
                "Bollinger Bands - AURX Volatility",
                false);
    }
}
