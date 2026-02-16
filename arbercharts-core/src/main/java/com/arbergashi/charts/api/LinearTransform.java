package com.arbergashi.charts.api;

/**
 * Identity transform that delegates directly to PlotContext.
 *
 * <p>Platform-independent and headless-certified. No AWT/Swing dependencies.</p>
 *
 * @since 2.0.0
  * @author Arber Gashi
  * @version 2.0.0
 */
public final class LinearTransform implements CoordinateTransformer {
    @Override
    public void mapToPixel(PlotContext context, double x, double y, double[] out) {
        if (context == null || out == null) return;
        context.mapToPixel(x, y, out);
    }

    @Override
    public void mapToData(PlotContext context, double pixelX, double pixelY, double[] out) {
        if (context == null || out == null) return;
        context.mapToData(pixelX, pixelY, out);
    }
}
