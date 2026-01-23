/**
 * Swing UI components of the ArberCharts framework.
 *
 * <p>This package contains the main chart panel, overlays (tooltip, crosshair, legend),
 * input handling (pan/zoom), and exporting utilities. The primary entry point is
 * {@link com.arbergashi.charts.ui.ArberChartPanel}.</p>
 *
 * <h2>Threading</h2>
 * <p>All UI interaction must occur on the Swing EDT. Models may be updated from background
 * threads, but must notify listeners in a safe manner depending on the application.</p>
 */
package com.arbergashi.charts.ui;
