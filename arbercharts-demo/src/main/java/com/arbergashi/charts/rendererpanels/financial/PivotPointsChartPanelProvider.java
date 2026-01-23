package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.render.financial.PivotPointsRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

public class PivotPointsChartPanelProvider {
    public static ArberChartPanel create() {
        return FinancialIndicatorPanelProvider.create(
                new PivotPointsRenderer(),
                "Pivot Points - AURX Support and Resistance",
                false);
    }
}
