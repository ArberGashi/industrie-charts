package com.arbergashi.charts.render;
/**
 * Zero-allocation tooltip content provider.
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public interface TooltipContentProvider {
    void getContent(StringBuilder target, TooltipContext ctx);
}
