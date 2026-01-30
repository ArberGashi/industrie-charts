package com.arbergashi.charts.render.legend;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.util.List;
/**
 * Minimal chart data access for legend rendering.
 *
 * <p>This keeps legend UI independent from any specific panel implementation.</p>
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public interface LegendChartContext {

    /**
     * Returns the chart model providing legend labels and series metadata.
     *
     * @return current chart model
     */
    ChartModel getModel();

    /**
     * Returns the active renderers for this chart, in layer order.
     *
     * @return renderer list (never null)
     */
    List<BaseRenderer> getRenderers();
}
