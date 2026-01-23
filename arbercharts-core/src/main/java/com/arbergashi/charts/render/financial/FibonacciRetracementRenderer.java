package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;

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
 */
public final class FibonacciRetracementRenderer extends BaseRenderer {

    private final double[] levels = {0.0, 0.236, 0.382, 0.5, 0.618, 0.786, 1.0};
    private final double[] p1 = new double[2];
    private final double[] p2 = new double[2];
    private final float[] dashPattern = {5f, 5f};
    private final NumberFormat percentFormat = NumberFormat.getPercentInstance();
    private final java.text.DecimalFormat valueFormat = new java.text.DecimalFormat("0.00");

    public FibonacciRetracementRenderer() {
        super("fibonacci");
        percentFormat.setMinimumFractionDigits(1);
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        // We need a high and a low for the retracement.
        // In a simple model, these could be the first two points or special properties.
        // Here we assume the model provides high and low for the entire range.
        double high = model.getDataRange()[3];
        double low = model.getDataRange()[2];
        double range = high - low;

        if (range <= 0) return;

        Rectangle2D plotBounds = context.plotBounds();
        g2.setFont(getCachedFont(10f, Font.PLAIN));
        FontMetrics fm = g2.getFontMetrics();

        for (double level : levels) {
            double val = high - (level * range);
            context.mapToPixel(context.minX(), val, p1);
            context.mapToPixel(context.maxX(), val, p2);

            // Draw line
            g2.setColor(getLevelColor(level, context));
            g2.setStroke(getCachedStroke(ChartScale.scale(1.0f), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, dashPattern, 0f));
            g2.draw(getLine(plotBounds.getX(), p1[1], plotBounds.getMaxX(), p1[1]));

            // Draw label (zero-allocation number formatting)
            String label = percentFormat.format(level) + " (" + valueFormat.format(val) + ")";
            g2.drawString(label, (float) (plotBounds.getMaxX() - fm.stringWidth(label) - 5), (float) (p1[1] - 2));
        }
    }

    private Color getLevelColor(double level, PlotContext context) {
        if (level == 0.0 || level == 1.0) return themeGrid(context);
        if (level == 0.5) return themeSeries(context, 1);
        if (level == 0.618) return themeSeries(context, 2);
        return com.arbergashi.charts.util.ColorUtils.withAlpha(themeGrid(context), 0.6f);
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }
}
