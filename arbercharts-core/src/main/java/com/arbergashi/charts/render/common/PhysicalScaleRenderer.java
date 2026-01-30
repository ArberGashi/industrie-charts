package com.arbergashi.charts.render.common;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;

/**
 * Draws a small physical scale marker (mm-based) when physical scaling is enabled.
 * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class PhysicalScaleRenderer extends BaseRenderer {
    private static final String KEY_ENABLED = "Chart.scale.physical.enabled";

    public PhysicalScaleRenderer() {
        super("physicalScale");
    }

    @Override
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        if (!ChartAssets.getBoolean(KEY_ENABLED, false)) {
            return;
        }
        ArberRect bounds = context.getPlotBounds();
        if (bounds == null || bounds.getWidth() <= 1 || bounds.getHeight() <= 1) {
            return;
        }

        // Physical scale visualization is UI-bridge responsibility.
        // Core remains headless and only signals intent.
    }
}
