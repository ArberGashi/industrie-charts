package com.arbergashi.charts.domain.legend;
/**
 * Defines whether the legend is rendered as an overlay within the chart canvas or docked
 * outside the plot area.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Platform-independent and headless-certified. No AWT/Swing dependencies.
 *
 */
public enum LegendPlacement {
    /** Legend is painted over the chart (overlay). */
    OVERLAY,
    /** Legend is laid out outside the plot area (docked). */
    DOCKED
}
