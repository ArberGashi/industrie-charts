package com.arbergashi.charts.platform.export;

import com.arbergashi.charts.platform.swing.ArberChartPanel;

/**
 * Export handler for charts.
 *
 * <p>Core does not present file choosers; applications provide an export handler
 * to integrate their own UI or file selection workflow.</p>
 */
@FunctionalInterface/**
 * @since 1.5.0
 */
public interface ChartExportHandler {

    /**
     * Executes an export operation for the given chart.
     *
     * @param panel  chart panel to export
     * @param format export format identifier (e.g., "png", "svg", "pdf")
     */
    void export(ArberChartPanel panel, String format);
}
