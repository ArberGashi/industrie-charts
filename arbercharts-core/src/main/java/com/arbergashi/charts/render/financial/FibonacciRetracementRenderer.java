package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * <h1>FibonacciRetracementRenderer</h1>
 * <p>
 * Draws Fibonacci retracement levels (0%, 23.6%, 38.2%, 50%, 61.8%, 78.6%, 100%).
 * Typically used as an overlay for candlestick or line charts.
 * </p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class FibonacciRetracementRenderer extends BaseRenderer {

    private final double[] levels = {0.0, 0.236, 0.382, 0.5, 0.618, 0.786, 1.0};
    private final double[] p1 = new double[2];
    private final double[] p2 = new double[2];
    private final float[] dashPattern = {5f, 5f};
    private final float[] lineX = new float[2];
    private final float[] lineY = new float[2];

    public FibonacciRetracementRenderer() {
        super("fibonacci");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        // We need a high and a low for the retracement.
        // In a simple model, these could be the first two points or special properties.
        // Here we assume the model provides high and low for the entire range.
        double high = model.getDataRange()[3];
        double low = model.getDataRange()[2];
        double range = high - low;

        if (range <= 0) return;

        com.arbergashi.charts.core.geometry.ArberRect plotBounds = context.getPlotBounds();

        for (double level : levels) {
            double val = high - (level * range);
            context.mapToPixel(context.getMinX(), val, p1);
            context.mapToPixel(context.getMaxX(), val, p2);

            // Draw line
            canvas.setColor(getLevelColor(level, context));
            canvas.setStroke(ChartScale.scale(1.0f));
            lineX[0] = (float) plotBounds.x();
            lineY[0] = (float) p1[1];
            lineX[1] = (float) plotBounds.maxX();
            lineY[1] = (float) p1[1];
            canvas.drawPolyline(lineX, lineY, 2);
        }
    }

    private ArberColor getLevelColor(double level, PlotContext context) {
        if (level == 0.0 || level == 1.0) return themeGrid(context);
        if (level == 0.5) return themeSeries(context, 1);
        if (level == 0.618) return themeSeries(context, 2);
        return ColorRegistry.applyAlpha(themeGrid(context), 0.6f);
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }
}
