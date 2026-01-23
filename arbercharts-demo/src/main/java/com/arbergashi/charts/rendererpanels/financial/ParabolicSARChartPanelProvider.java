package com.arbergashi.charts.rendererpanels.financial;

import com.arbergashi.charts.render.financial.ParabolicSARRenderer;
import com.arbergashi.charts.ui.ArberChartPanel;

public class ParabolicSARChartPanelProvider {
    public static ArberChartPanel create() {
        return FinancialIndicatorPanelProvider.create(
                new ParabolicSARRenderer(),
                "Parabolic SAR - AURX Stop and Reverse",
                false);
    }
}
