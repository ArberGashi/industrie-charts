package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.internal.AnalysisWorker;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.model.FinancialChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * Professional, zero-allocation Bollinger Bands renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class BollingerBandsRenderer extends BaseRenderer {

    private final double[] p0 = new double[2];
    private transient float[] pathX;
    private transient float[] pathY;
    private transient float[] rangeX;
    private transient float[] rangeY;

    private long lastModelStamp = -1;
    private int cachedPointCount;
    private double[] yValues;
    private double[] smaValues;
    private double[] upperValues;
    private double[] lowerValues;
    private double[] xValues;

    public BollingerBandsRenderer() {
        super("bollinger");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        ensureCache(model);
        if (cachedPointCount < 2) return;

        final double viewMinX = context.getPlotBounds().minX();
        final double viewMaxX = context.getPlotBounds().maxX();

        ArberColor base = getSeriesColor(model);

        drawFillChannel(canvas, context, base, viewMinX, viewMaxX);

        canvas.setStroke(ChartScale.scale(1.0f));
        canvas.setColor(ColorRegistry.applyAlpha(base, 0.4f));
        drawPath(canvas, context, upperValues, viewMinX, viewMaxX);
        drawPath(canvas, context, lowerValues, viewMinX, viewMaxX);

        canvas.setStroke(ChartScale.scale(1.5f));
        canvas.setColor(base);
        drawPath(canvas, context, smaValues, viewMinX, viewMaxX);
    }

    private void drawFillChannel(ArberCanvas canvas, PlotContext context, ArberColor base, double viewMinX, double viewMaxX) {
        int upperCount = 0;
        int lowerCount = 0;
        ensureRangeCapacity(cachedPointCount * 2);

        for (int i = 0; i < cachedPointCount; i++) {
            if (Double.isNaN(upperValues[i])) continue;
            context.mapToPixel(xValues[i], upperValues[i], p0);
            if (p0[0] < viewMinX) continue;
            if (p0[0] > viewMaxX) break;
            rangeX[upperCount] = (float) p0[0];
            rangeY[upperCount] = (float) p0[1];
            upperCount++;
        }
        if (upperCount == 0) return;

        for (int i = cachedPointCount - 1; i >= 0; i--) {
            if (Double.isNaN(lowerValues[i])) continue;
            context.mapToPixel(xValues[i], lowerValues[i], p0);
            if (p0[0] < viewMinX) break;
            if (p0[0] > viewMaxX) continue;
            rangeX[upperCount + lowerCount] = (float) p0[0];
            rangeY[upperCount + lowerCount] = (float) p0[1];
            lowerCount++;
        }
        int total = upperCount + lowerCount;
        if (total < 3) return;

        float fillAlpha = ChartAssets.getFloat("chart.financial.bollinger.fillAlpha", 0.10f);
        canvas.setColor(ColorRegistry.applyAlpha(base, fillAlpha));
        canvas.fillPolygon(rangeX, rangeY, total);
    }

    private void drawPath(ArberCanvas canvas, PlotContext context, double[] values, double viewMinX, double viewMaxX) {
        int count = 0;
        ensurePathCapacity(cachedPointCount);
        for (int i = 0; i < cachedPointCount; i++) {
            if (Double.isNaN(values[i])) continue;
            context.mapToPixel(xValues[i], values[i], p0);
            if (p0[0] < viewMinX) continue;
            if (p0[0] > viewMaxX) break;
            pathX[count] = (float) p0[0];
            pathY[count] = (float) p0[1];
            count++;
        }
        if (count >= 2) {
            canvas.drawPolyline(pathX, pathY, count);
        }
    }

    private void ensurePathCapacity(int count) {
        if (pathX == null || pathX.length < count) {
            pathX = new float[count];
            pathY = new float[count];
        }
    }

    private void ensureRangeCapacity(int count) {
        if (rangeX == null || rangeX.length < count) {
            rangeX = new float[count];
            rangeY = new float[count];
        }
    }

    private void ensureCache(ChartModel model) {
        final long stamp = model.getUpdateStamp();
        if (stamp == lastModelStamp && cachedPointCount == model.getPointCount()) {
            return;
        }
        lastModelStamp = stamp;
        cachedPointCount = model.getPointCount();
        final int n = cachedPointCount;

        int period = ChartAssets.getInt("chart.financial.bollinger.period", 20);
        if (n < period) {
            cachedPointCount = 0;
            return;
        }

        if (xValues == null || xValues.length < n) {
            xValues = new double[n];
            yValues = new double[n];
            smaValues = new double[n];
            upperValues = new double[n];
            lowerValues = new double[n];
        }

        final FinancialChartModel fin = (model instanceof FinancialChartModel) ? (FinancialChartModel) model : null;
        if (fin != null) {
            for (int i = 0; i < n; i++) {
                xValues[i] = model.getX(i);
                yValues[i] = fin.getClose(i);
            }
        } else {
            for (int i = 0; i < n; i++) {
                xValues[i] = model.getX(i);
                yValues[i] = model.getY(i);
            }
        }

        double stdDevFactor = ChartAssets.getFloat("chart.financial.bollinger.stddev", 2.0f);
        AnalysisWorker.getCalculatedBollingerBands(yValues, period, stdDevFactor, smaValues, upperValues, lowerValues);
    }
}
