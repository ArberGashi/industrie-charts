package com.arbergashi.charts.render.grid;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;

/**
 * Strategy interface for grid rendering via a platform-neutral canvas.
 * Allows for custom grid implementations (e.g. standard, medical, polar, etc.).
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public interface GridLayer {
    /**
     * Renders the chart grid for the given plot context.
     *
     * @param canvas rendering context
     * @param context plot context with bounds and axis ranges
     */
    void renderGrid(ArberCanvas canvas, PlotContext context);

    /**
     * Renders the grid into a spatial batch for headless or 3D pipelines.
     *
     * @param builder spatial path batch builder
     * @param context plot context with bounds and axis ranges
     * @param config  batch configuration
     */
    default void renderGridBatch(SpatialPathBatchBuilder builder, PlotContext context, GridBatchConfig config) {
    }
}
