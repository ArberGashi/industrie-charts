/**
 * Rendering layer for ArberCharts.
 *
 * <p>Contains renderer implementations organized by domain (financial, medical, statistical,
 * circular, specialized, standard). Renderers are designed for zero-allocation execution and
 * should honor PlotContext clipping and scaling contracts.</p>
 *
 * <p>Thread-safety: Renderers may be reused across frames and must avoid shared mutable state
 * unless explicitly documented. Per-instance caches should remain thread-confined.</p>
 *
 * @author Arber Gashi
 * @version 1.7.0
 * @since 2026-01-30
 */
package com.arbergashi.charts.render;
