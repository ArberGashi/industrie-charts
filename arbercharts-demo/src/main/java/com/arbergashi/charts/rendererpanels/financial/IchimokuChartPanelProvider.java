package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.render.financial.IchimokuRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

public class IchimokuChartPanelProvider {
    public static ArberChartPanel create() {
        return FinancialIndicatorPanelProvider.create(
                new IchimokuRenderer(),
                "Ichimoku Cloud - AURX Trend Analysis",
                false);
    }
}
