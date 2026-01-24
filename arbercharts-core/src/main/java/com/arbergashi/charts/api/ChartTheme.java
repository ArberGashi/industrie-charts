package com.arbergashi.charts.api;

import java.awt.*;

/**
 * Chart theme interface for providing colors and fonts to renderers.
 * <p>
 * This interface defines the contract for chart theming. Implementations should be immutable
 * to ensure thread-safety and predictable behavior during runtime theme switching.
 * </p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-16
 */
public interface ChartTheme {

    /**
     * Gets the background color for the chart panel.
     *
     * @return The background color
     */
    Color getBackground();

    /**
     * Gets the foreground color for text and lines.
     *
     * @return The foreground color
     */
    Color getForeground();

    /**
     * Gets the grid color for axes and grid lines.
     *
     * @return The grid color
     */
    Color getGridColor();

    /**
     * Gets the color for axis labels.
     *
     * @return The axis label color
     */
    Color getAxisLabelColor();

    /**
     * Gets the accent color (primary highlight color).
     *
     * @return The accent color
     */
    Color getAccentColor();

    /**
     * Gets a series color by index.
     * <p>
     * Implementations should cycle through available colors using modulo arithmetic
     * to ensure a valid color is always returned.
     * </p>
     *
     * @param index The series index (0-based)
     * @return The series color for the given index
     */
    Color getSeriesColor(int index);

    /**
     * Gets the base font for chart text.
     *
     * @return The base font
     */
    Font getBaseFont();

    /**
     * Gets the bullish color for financial charts.
     * <p>
     * Default implementation returns green. Override for custom financial themes.
     * </p>
     *
     * @return The bullish color (default: green)
     */
    default Color getBullishColor() {
        return com.arbergashi.charts.util.ColorRegistry.of(0, 150, 50, 255);
    }

    /**
     * Gets the bearish color for financial charts.
     * <p>
     * Default implementation returns red. Override for custom financial themes.
     * </p>
     *
     * @return The bearish color (default: red)
     */
    default Color getBearishColor() {
        return com.arbergashi.charts.util.ColorRegistry.of(215, 50, 0, 255);
    }
}
