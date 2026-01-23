package com.arbergashi.charts.ui;

/**
 * Export handler for charts.
 *
 * <p>Core does not present file choosers; applications provide an export handler
 * to integrate their own UI or file selection workflow.</p>
 */
@FunctionalInterface
public interface ChartExportHandler {

    /**
     * Executes an export operation for the given chart.
     *
     * @param panel  chart panel to export
     * @param format export format identifier (e.g., "png", "svg", "pdf")
     */
    void export(ArberChartPanel panel, String format);
}
