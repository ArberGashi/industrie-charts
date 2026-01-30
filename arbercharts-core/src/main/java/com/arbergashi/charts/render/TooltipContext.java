package com.arbergashi.charts.render;

import com.arbergashi.charts.api.AxisConfig;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;

/**
 * Tooltip payload for renderer-provided content.
  * @author Arber Gashi
  * @version 1.7.0
  * @since 2026-01-30
 */
public final class TooltipContext {
    private int index;
    private ChartModel model;
    private PlotContext plotContext;
    private AxisConfig xAxis;
    private AxisConfig yAxis;

    public TooltipContext(int index, ChartModel model, PlotContext plotContext, AxisConfig xAxis, AxisConfig yAxis) {
        this.index = index;
        this.model = model;
        this.plotContext = plotContext;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
    }

    public int getIndex() {
        return index;
    }

    public TooltipContext setIndex(int index) {
        this.index = index;
        return this;
    }

    public ChartModel getModel() {
        return model;
    }

    public TooltipContext setModel(ChartModel model) {
        this.model = model;
        return this;
    }

    public PlotContext getPlotContext() {
        return plotContext;
    }

    public TooltipContext setPlotContext(PlotContext plotContext) {
        this.plotContext = plotContext;
        return this;
    }

    public AxisConfig getXAxis() {
        return xAxis;
    }

    public TooltipContext setXAxis(AxisConfig xAxis) {
        this.xAxis = xAxis;
        return this;
    }

    public AxisConfig getYAxis() {
        return yAxis;
    }

    public TooltipContext setYAxis(AxisConfig yAxis) {
        this.yAxis = yAxis;
        return this;
    }
}
