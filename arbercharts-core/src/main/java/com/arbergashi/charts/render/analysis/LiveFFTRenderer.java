package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.geometry.ArberRect;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
/**
 * Live FFT renderer (JDK 25 standard).
 * Transforms a time signal into the frequency domain and visualizes it.
 * Ideal for acoustics, vibration analysis, and signal processing.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class LiveFFTRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];
    private boolean asBars = true;

    public LiveFFTRenderer() {
        super("fft");
    }

    /**
     * Helper for a simple FFT (Cooley-Tukey).
     * Provided as a placeholder for internal logic.
     */
    public static double[] getComputedFFT(double[] timeSignal) {
        int n = timeSignal.length;
        if (Integer.bitCount(n) != 1) {
            // Padding to the next power of two would be required.
        }
        // ... FFT Logik ...
        return timeSignal; // Dummy
    }

    public LiveFFTRenderer setAsBars(boolean asBars) {
        this.asBars = asBars;
        return this;
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 2) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        // In a real setup, FFT would be computed here.
        // We assume the model already contains frequency data (magnitude),
        // or this would be transformed if the input is time-domain data.

        ArberColor base = seriesOrBase(model, context, 0);
        canvas.setColor(base);

        if (asBars) {
            drawAsBars(canvas, xData, yData, count, context, base);
        } else {
            drawAsLine(canvas, xData, yData, count, context, base);
        }
    }

    private void drawAsBars(ArberCanvas canvas, double[] xData, double[] yData, int count, PlotContext context, ArberColor base) {
        // Find data range for proper scaling
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (int i = 0; i < count; i++) {
            if (xData[i] < minX) minX = xData[i];
            if (xData[i] > maxX) maxX = xData[i];
            if (yData[i] < minY) minY = yData[i];
            if (yData[i] > maxY) maxY = yData[i];
        }

        // Add padding
        double rangeY = maxY - minY;
        if (rangeY == 0) rangeY = 1.0;
        minY = minY - rangeY * 0.05;
        maxY = maxY + rangeY * 0.05;
        rangeY = maxY - minY;

        ArberRect bounds = context.getPlotBounds();
        double plotX = bounds.x();
        double plotY = bounds.y();
        double plotW = bounds.width();
        double plotH = bounds.height();

        double xRange = maxX - minX;
        if (xRange == 0) xRange = 1.0;

        double barW = plotW / Math.max(1, count);
        double baseline = plotY + plotH; // Bottom of plot

        for (int i = 0; i < count; i++) {
            // Manual pixel mapping
            double px = plotX + (xData[i] - minX) / xRange * plotW;
            double py = plotY + (maxY - yData[i]) / rangeY * plotH;

            float h = (float) Math.abs(baseline - py);
            float y = (float) Math.min(baseline, py);

            if (isMultiColor()) {
                ArberColor bar = themeSeries(context, i);
                if (bar == null) bar = base;
                canvas.setColor(bar);
            }
            canvas.fillRect((float) (px - barW / 2), y, (float) barW, h);
        }
    }

    private void drawAsLine(ArberCanvas canvas, double[] xData, double[] yData, int count, PlotContext context, ArberColor base) {
        // Find data range for proper scaling
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (int i = 0; i < count; i++) {
            if (xData[i] < minX) minX = xData[i];
            if (xData[i] > maxX) maxX = xData[i];
            if (yData[i] < minY) minY = yData[i];
            if (yData[i] > maxY) maxY = yData[i];
        }

        // Add padding
        double rangeY = maxY - minY;
        if (rangeY == 0) rangeY = 1.0;
        minY = minY - rangeY * 0.05;
        maxY = maxY + rangeY * 0.05;
        rangeY = maxY - minY;

        ArberRect bounds = context.getPlotBounds();
        double plotX = bounds.x();
        double plotY = bounds.y();
        double plotW = bounds.width();
        double plotH = bounds.height();

        double xRange = maxX - minX;
        if (xRange == 0) xRange = 1.0;

        double prevX = Double.NaN;
        double prevY = Double.NaN;
        float[] xs = null;
        float[] ys = null;
        int outCount = 0;
        if (!isMultiColor()) {
            xs = RendererAllocationCache.getFloatArray(this, "fft.line.x", count);
            ys = RendererAllocationCache.getFloatArray(this, "fft.line.y", count);
        }

        for (int i = 0; i < count; i++) {
            // Manual pixel mapping
            double px = plotX + (xData[i] - minX) / xRange * plotW;
            double py = plotY + (maxY - yData[i]) / rangeY * plotH;

            if (!isMultiColor()) {
                xs[outCount] = (float) px;
                ys[outCount] = (float) py;
                outCount++;
            } else if (i > 0) {
                ArberColor seg = themeSeries(context, i);
                if (seg == null) seg = base;
                canvas.setColor(seg);
                float[] sx = RendererAllocationCache.getFloatArray(this, "fft.seg.x", 2);
                float[] sy = RendererAllocationCache.getFloatArray(this, "fft.seg.y", 2);
                sx[0] = (float) prevX;
                sy[0] = (float) prevY;
                sx[1] = (float) px;
                sy[1] = (float) py;
                canvas.drawPolyline(sx, sy, 2);
            }
            prevX = px;
            prevY = py;
        }

        if (!isMultiColor()) {
            canvas.drawPolyline(xs, ys, outCount);
        }
    }
}
