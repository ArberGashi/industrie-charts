package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.render.financial.FibonacciRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

public class FibonacciChartPanelProvider {
    public static ArberChartPanel create() {
        return FinancialIndicatorPanelProvider.create(
                new FibonacciRenderer(),
                "Fibonacci - AURX Retracement Levels",
                false);
    }
}
