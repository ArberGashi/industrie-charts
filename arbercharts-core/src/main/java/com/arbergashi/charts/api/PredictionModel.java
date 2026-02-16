package com.arbergashi.charts.api;

import com.arbergashi.charts.model.ChartModel;
/**
 * Strategy interface for generating predicted points.
  * @since 1.5.0
  * @author Arber Gashi
  * @version 2.0.0
 */
public interface PredictionModel {

    /**
     * Writes predicted points into the provided buffer.
     *
     * @param model   source model
     * @param context plot context
     * @param out     reusable prediction buffer
     */
    void predict(ChartModel model, PlotContext context, PredictionBuffer out);
}
