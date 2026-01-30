package com.arbergashi.charts.render.analysis;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
/**
 * Marker overlay for minimum/maximum (y) values within a series.
 * Useful for sparklines and monitoring dashboards.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class MinMaxMarkerRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];

    public MinMaxMarkerRenderer() {
        super("minMaxMarker");
    }

    @Override
    public boolean isLegendRequired() {
        return false;
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        int minIdx = 0;
        int maxIdx = 0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < count; i++) {
            double y = yData[i];
            if (y < min) {
                min = y;
                minIdx = i;
            }
            if (y > max) {
                max = y;
                maxIdx = i;
            }
        }

        double r = ChartScale.scale(ChartAssets.getFloat("chart.render.minmax.radius", 4.0f));
        final ChartTheme theme = getResolvedTheme(context);
        ArberColor base = theme.getAxisLabelColor();
        ArberColor minC = base;
        ArberColor maxC = getSeriesColor(model);
        if (isMultiColor()) {
            ArberColor minBase = themeSeries(context, 0);
            ArberColor maxBase = themeSeries(context, 1);
            if (minBase != null) minC = minBase;
            if (maxBase != null) maxC = maxBase;
        }

        if (minIdx == maxIdx) {
            drawDot(canvas, context, xData[minIdx], yData[minIdx], r, maxC);
            return;
        }

        if (ChartAssets.getBoolean("chart.render.minmax.showMin", true)) {
            drawDot(canvas, context, xData[minIdx], yData[minIdx], r, minC);
        }
        if (ChartAssets.getBoolean("chart.render.minmax.showMax", true)) {
            drawDot(canvas, context, xData[maxIdx], yData[maxIdx], r, maxC);
        }
    }

    private void drawDot(ArberCanvas canvas, PlotContext context, double x, double y, double r, ArberColor c) {
        context.mapToPixel(x, y, pBuffer);
        canvas.setColor(c);
        canvas.fillRect((float) (pBuffer[0] - r), (float) (pBuffer[1] - r), (float) (r * 2), (float) (r * 2));
    }
}
