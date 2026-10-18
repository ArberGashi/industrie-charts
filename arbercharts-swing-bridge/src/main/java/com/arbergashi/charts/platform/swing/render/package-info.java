/**
 * Explicit scale conversions for Swing and application-owned raster surfaces.
 *
 * <p>{@link com.arbergashi.charts.platform.swing.render.HighDpiRenderer} caches
 * one scale for the process, derived from a component's default graphics
 * transform. It provides numeric coordinate, font-size and stroke-width helpers.
 * It does not maintain per-monitor state, monitor display changes or render icons.
 *
 * <p>A Swing paint context can already include its device transform. Preserve
 * that transform instead of applying the same scale again. Explicit scaling is
 * appropriate only when the host owns an unscaled target and allocates its pixel
 * dimensions accordingly; the class documentation contains an offscreen example.
 *
 * <p>{@code resetCache()} clears the shared scale for the next lookup. Hosts must
 * coordinate that reset with their display lifecycle and validate their own
 * mixed-monitor behavior. These helpers do not establish visual acceptance or
 * pixel-identical rendering on every display.
 *
 * @see com.arbergashi.charts.platform.swing.render.HighDpiRenderer
 * @see java.awt.GraphicsConfiguration#getDefaultTransform()
 * @since 2.0.0
 */
package com.arbergashi.charts.platform.swing.render;

