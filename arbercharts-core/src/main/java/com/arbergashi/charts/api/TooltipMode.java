package com.arbergashi.charts.api;
/**
 * Tooltip behavior modes.
 * @author Arber Gashi
 * @version 1.0.0
  * @since 1.5.0
 */
public enum TooltipMode {
    /** Tooltip follows the pointer with smart edge avoidance. */
    FLOATING,
    /** Tooltip is pinned to a fixed corner (e.g., top-left) to avoid blocking the chart. */
    STICKY
}
