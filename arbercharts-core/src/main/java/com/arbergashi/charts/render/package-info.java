/**
 * Rendering layer of the ArberCharts framework.
 *
 * <p>Renderers are responsible for drawing a {@link com.arbergashi.charts.model.ChartModel}
 * into a plot area defined by {@link com.arbergashi.charts.api.PlotContext} using colors and
 * fonts from {@link com.arbergashi.charts.api.ChartTheme}.</p>
 *
 * <p><b>Contract:</b> Renderers must be fast, allocation-aware, and must not depend on demo-only
 * resources or global theme state.</p>
 */
package com.arbergashi.charts.render;
