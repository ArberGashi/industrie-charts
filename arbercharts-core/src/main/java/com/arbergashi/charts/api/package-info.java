/**
 * Public API of the ArberCharts framework.
 *
 * <p>This package contains the stable entry points and contracts that consumers should use
 * to build and render charts.</p>
 *
 * <h2>Main concepts</h2>
 * <ul>
 *   <li><b>{@link com.arbergashi.charts.api.ArberChartBuilder}:</b> fluent builder to create
 *       {@link com.arbergashi.charts.ui.ArberChartPanel} instances.</li>
 *   <li><b>{@link com.arbergashi.charts.api.ChartTheme}:</b> theme contract used by renderers,
 *       grids, legends, tooltips, and overlays.</li>
 *   <li><b>{@link com.arbergashi.charts.api.PlotContext}:</b> immutable view of the current
 *       plot bounds and coordinate mapping used during rendering.</li>
 * </ul>
 *
 * <p><b>Compatibility note:</b> The framework avoids global/static theme state. Themes are
 * provided per chart instance via builder/panel APIs.</p>
 */
package com.arbergashi.charts.api;
