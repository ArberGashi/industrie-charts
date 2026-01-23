package com.arbergashi.charts.ui.legend;

import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.util.List;

/**
 * Minimal chart data access for legend rendering.
 *
 * <p>This keeps legend UI independent from any specific panel implementation.</p>
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
