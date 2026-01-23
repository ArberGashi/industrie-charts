/**
 * Data model layer of the ArberCharts framework.
 *
 * <p>Models expose chart data in a renderer-friendly form. The primary design goals are:</p>
 * <ul>
 *   <li><b>Performance:</b> prefer primitive arrays and predictable iteration to minimize GC pressure.</li>
 *   <li><b>Flexibility:</b> support standard XY series, financial OHLC, medical signals, and multi-dimensional data.</li>
 *   <li><b>Contracts:</b> renderers must iterate by {@link com.arbergashi.charts.model.ChartModel#getPointCount()},
 *       not by array length.</li>
 * </ul>
 */
package com.arbergashi.charts.model;
