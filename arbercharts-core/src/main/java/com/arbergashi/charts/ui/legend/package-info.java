/**
 * Legend UI components.
 *
 * <h2>Overview</h2>
 * <p>This package contains the framework's legend implementations:
 * <ul>
 *   <li>{@link com.arbergashi.charts.ui.legend.InteractiveLegendOverlay} for overlay legends</li>
 *   <li>{@link com.arbergashi.charts.ui.legend.DockedLegendPanel} for docked legends</li>
 * </ul>
 * </p>
 *
 * <h2>Interactivity</h2>
 * <ul>
 *   <li><b>Click series name:</b> toggles series visibility</li>
 *   <li><b>Alt+Click series name:</b> solos a series (optional, see configuration)</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>Legend sizing and layout can be tuned via {@code ChartAssets} keys. Docked legends also support
 * a density preset via {@code Chart.legend.dock.density=compact|dense}.</p>
 *
 * <p><b>Solo mode:</b> can be disabled via {@code Chart.legend.soloEnabled=false}.</p>
 */
package com.arbergashi.charts.ui.legend;
