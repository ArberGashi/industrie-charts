package com.arbergashi.charts.render;
/**
 * Render band for a layer.
 *
 * <p>This allows the panel to render certain layers between the grid and the
 * main data layers without changing the default painter's algorithm.</p>
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public enum LayerBand {
    /** Standard data layers. */
    DATA,
    /** Layers rendered after the grid but before the main data layers. */
    PRE_DATA
}
