package com.arbergashi.charts.render.legend;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import java.util.List;

/**
 * Swing legend data associated with individual chart layers.
 * Renderer identifiers describe renderer types; entry identifiers describe independent series.
 */
public interface LayerLegendContext extends LegendChartContext {
    /** Returns the current series entries in paint order. */
    List<Entry> getEntries();

    /** Returns the plot context used to draw the entry. */
    PlotContext getContext(Entry entry);

    /**
     * A series identity and its corresponding model and renderer.
     *
     * @param id stable host series identifier
     * @param name display name
     * @param model data model belonging to this series
     * @param renderer renderer belonging to this series
     */
    record Entry(String id, String name, ChartModel model, BaseRenderer renderer) { }
}
