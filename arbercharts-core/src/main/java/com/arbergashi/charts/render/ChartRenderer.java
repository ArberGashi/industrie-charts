package com.arbergashi.charts.render;

import com.arbergashi.charts.api.DefaultPlotContext;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.model.ChartModel;

import com.arbergashi.charts.api.types.ArberPoint;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
/**
 * Defines the contract for custom chart renderers.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public interface ChartRenderer {
    /**
     * Core render entry point using ArberCanvas (headless/bridge).
     *
     * <p>Default implementation is a no-op until renderers migrate to ArberCanvas.</p>
     */
    default void render(ArberCanvas canvas, ChartModel model, PlotContext context) {
    }

    /**
     * Returns the nearest point index to the given pixel, if supported.
     *
     * @param pixel mouse/crosshair position in component coordinates
     * @param model data model for this layer
     * @param context current plot context
     * @return optional point index
     */
    Optional<Integer> getPointAt(ArberPoint pixel, ChartModel model, PlotContext context);

    /**
     * Returns the formatted tooltip text for a given data point index.
     * The renderer is responsible for knowing the data's semantics.
     * <p>
     * Default implementation returns null; renderers that need custom tooltips may override.
     *
     * @param index The index of the data point.
     * @param model The data model.
     * @return A formatted string for the tooltip, or null if no tooltip should be shown.
     */
    default String getTooltipText(int index, ChartModel model) {
        return null;
    }

    /**
     * Contributes structured focus values for interactive UI elements.
     *
     * <p>This is used by the interactive legend and advanced tooltips. The default implementation returns
     * an empty map to keep existing renderers source-compatible.</p>
     *
     * <p>Typical keys for financial charts: "Open", "High", "Low", "Close", "Volume".
     * Analysis renderers may contribute indicator values (e.g. "MACD", "Signal", "Histogram").</p>
     *
     * @param index focused point index
     * @param model the layer model
     * @param context plot context
     * @return map of key/value pairs to display
     */
    default Map<String, Object> getFocusValues(int index, ChartModel model, PlotContext context) {
        return Collections.emptyMap();
    }

    /**
     * Optional Y-range override for autoscaling.
     *
     * <p>Renderers may return a {minY, maxY} array when the visible range should be based on
     * derived data rather than the model's raw Y-values (e.g. indicators like MACD).</p>
     *
     * @param model the layer model
     * @return {minY, maxY} or null to use model Y-values
     */
    default double[] getPreferredYRange(ChartModel model) {
        return null;
    }

    /**
     * Whether this renderer can draw an empty-state placeholder when its model is empty.
     */
    default boolean supportsEmptyState() {
        return false;
    }

    /**
     * Renders a renderer-specific empty state. Called only when {@link #supportsEmptyState()} is true.
     *
     * @param canvas canvas target
     * @param model data model for this layer (empty)
     * @param context current plot context
     */
    default void renderEmptyState(ArberCanvas canvas, ChartModel model, PlotContext context) {}

    /**
     * Returns the human-readable renderer name for legends/tooltips.
     *
     * @return renderer name
     */
    String getName();

    /**
     * Indicates whether a legend entry should be created for this renderer.
     *
     * @return true if the renderer should appear in the legend
     */
    default boolean isLegendRequired() {
        return true;
    }

    /**
     * Clears any hover/highlight state.
     */
    default void clearHover() {
    }

    /**
     * Creates a default plot context for rendering.
     *
     * @param bounds plot bounds in pixel coordinates
     * @param model data model for this layer
     * @return a plot context instance
     */
    default PlotContext getContext(ArberRect bounds, ChartModel model) {
        return new DefaultPlotContext(bounds, model, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
    }
}
