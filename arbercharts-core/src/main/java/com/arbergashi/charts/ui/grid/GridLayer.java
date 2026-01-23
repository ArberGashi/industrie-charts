package com.arbergashi.charts.ui.grid;

import com.arbergashi.charts.api.PlotContext;

import java.awt.*;

/**
 * Strategy interface for grid rendering in ArberChartPanel.
 * Allows for custom grid implementations (e.g. standard, medical, polar, etc.).
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public interface GridLayer {
    /**
     * Renders the chart grid for the given plot context.
     *
     * @param g graphics context
     * @param context plot context with bounds and axis ranges
     */
    void renderGrid(Graphics2D g, PlotContext context);
}
