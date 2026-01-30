package com.arbergashi.charts.render.grid;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;

/**
 * Statistical grid layer.
 *
 * <p>Part of the Zero-Allocation Render Path. High-frequency execution safe.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class StatisticalGridLayer extends DefaultGridLayer {
    @Override
    public void renderGridBatch(SpatialPathBatchBuilder builder, PlotContext context, GridBatchConfig config) {
        super.renderGridBatch(builder, context, config);
    }
}
