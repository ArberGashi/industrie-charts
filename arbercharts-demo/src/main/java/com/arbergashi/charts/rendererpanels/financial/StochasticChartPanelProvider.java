package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.render.financial.StochasticRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

public class StochasticChartPanelProvider {
    public static ArberChartPanel create() {
        return FinancialIndicatorPanelProvider.create(
                new StochasticRenderer(),
                "Stochastic Oscillator - AURX Momentum",
                false);
    }
}
