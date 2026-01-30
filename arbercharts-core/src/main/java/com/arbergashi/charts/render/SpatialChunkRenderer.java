package com.arbergashi.charts.render;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.engine.spatial.SpatialChunkConsumer;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;
import com.arbergashi.charts.model.ChartModel;

/**
 * Marker interface for renderers that can consume spatial chunks directly.
 *
 * <p>Part of the Zero-Allocation Render Path. High-frequency execution safe.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public interface SpatialChunkRenderer extends ChartRenderer, SpatialChunkConsumer {
    /**
     * Streams the model through the spatial pipeline into the provided consumer.
     *
     * @param model model to map
     * @param context plot context
     * @param consumer chunk consumer
     */
    default void renderSpatial(ChartModel model, PlotContext context, SpatialChunkConsumer consumer) {
    }

    /**
     * Optional access to a spatial path batch builder for this renderer.
     */
    default SpatialPathBatchBuilder getSpatialPathBatchBuilder() {
        return null;
    }
}
