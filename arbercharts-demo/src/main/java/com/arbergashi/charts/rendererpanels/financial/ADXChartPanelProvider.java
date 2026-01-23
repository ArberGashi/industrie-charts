package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.render.financial.ADXRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

public class ADXChartPanelProvider {
    public static ArberChartPanel create() {
        return FinancialIndicatorPanelProvider.create(
                new ADXRenderer(),
                "ADX - AURX Average Directional Index",
                false);
    }
}
