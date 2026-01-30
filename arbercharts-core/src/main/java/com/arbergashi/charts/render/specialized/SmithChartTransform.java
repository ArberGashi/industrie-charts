package com.arbergashi.charts.render.specialized;

import com.arbergashi.charts.api.CoordinateTransformer;
import com.arbergashi.charts.api.PlotContext;
/**
 * Smith chart coordinate transform (unit circle mapping).
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
  * @since 1.5.0
  * @author Arber Gashi
  * @version 1.7.0
 */
public final class SmithChartTransform implements CoordinateTransformer {
    @Override
    public void mapToPixel(PlotContext context, double x, double y, double[] out) {
        double radius = Math.min(context.getPlotBounds().getWidth(), context.getPlotBounds().getHeight()) * 0.5;
        double cx = context.getPlotBounds().getCenterX();
        double cy = context.getPlotBounds().getCenterY();
        out[0] = cx + x * radius;
        out[1] = cy - y * radius;
    }

    @Override
    public void mapToData(PlotContext context, double pixelX, double pixelY, double[] out) {
        double radius = Math.min(context.getPlotBounds().getWidth(), context.getPlotBounds().getHeight()) * 0.5;
        double cx = context.getPlotBounds().getCenterX();
        double cy = context.getPlotBounds().getCenterY();
        if (radius == 0) radius = 1.0;
        out[0] = (pixelX - cx) / radius;
        out[1] = (cy - pixelY) / radius;
    }
}
