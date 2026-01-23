package com.arbergashi.charts.rendererpanels;

import com.arbergashi.charts.ui.ArberChartPanel;

/**
 * Wrapper panels can expose their embedded chart through this interface.
 */
public interface ChartHost {
    ArberChartPanel getChartPanel();
}
