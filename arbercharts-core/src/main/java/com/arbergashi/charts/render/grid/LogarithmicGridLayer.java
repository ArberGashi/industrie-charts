package com.arbergashi.charts.render.grid;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.engine.spatial.SpatialPathBatchBuilder;

/**
 * Logarithmic grid layer.
 *
 * <p>Part of the Zero-Allocation Render Path. High-frequency execution safe.</p>
 *
 * @since 1.7.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class LogarithmicGridLayer extends DefaultGridLayer {
    private static final double LOG_EPS = Double.MIN_NORMAL;

    @Override
    protected void mapToPixel(PlotContext context, double x, double y, double[] out) {
        if (context != null && context.isLogarithmicY()) {
            y = getSafeLogValue(context, y);
        }
        super.mapToPixel(context, x, y, out);
    }

    @Override
    protected void mapToSpatial(PlotContext context, double x, double y, double z, double[] out) {
        if (context != null && context.isLogarithmicY()) {
            y = getSafeLogValue(context, y);
        }
        super.mapToSpatial(context, x, y, z, out);
    }

    @Override
    public void renderGridBatch(SpatialPathBatchBuilder builder, PlotContext context, GridBatchConfig config) {
        if (builder == null || context == null) return;
        if (context.getMinX() <= 0 || context.getMinY() <= 0) {
            super.renderGridBatch(builder, context, config);
            return;
        }
        super.renderGridBatch(builder, context, config);
    }

    private static double getSafeLogValue(PlotContext context, double y) {
        if (!Double.isFinite(y) || y <= 0.0) {
            double minY = context != null ? context.getMinY() : LOG_EPS;
            if (Double.isFinite(minY) && minY > 0.0) {
                return Math.max(LOG_EPS, minY);
            }
            return LOG_EPS;
        }
        return y;
    }
}
