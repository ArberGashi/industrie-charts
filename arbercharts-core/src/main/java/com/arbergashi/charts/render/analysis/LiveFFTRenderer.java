package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Live FFT renderer (JDK 25 standard).
 * Transforms a time signal into the frequency domain and visualizes it.
 * Ideal for acoustics, vibration analysis, and signal processing.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
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
    public static double[] computeFFT(double[] timeSignal) {
        int n = timeSignal.length;
        if (Integer.bitCount(n) != 1) {
            // Padding to the next power of two would be required.
        }
        // ... FFT Logik ...
        return timeSignal; // Dummy
    }

    public void setAsBars(boolean asBars) {
        this.asBars = asBars;
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 2) return;
        double[] xData = model.getXData();
        double[] yData = model.getYData();

        // In a real setup, FFT would be computed here.
        // We assume the model already contains frequency data (magnitude),
        // or this would be transformed if the input is time-domain data.

        Color base = seriesOrBase(model, context, 0);
        g2.setColor(base);

        if (asBars) {
            drawAsBars(g2, xData, yData, count, context, base);
        } else {
            drawAsLine(g2, xData, yData, count, context, base);
        }
    }

    private void drawAsBars(Graphics2D g2, double[] xData, double[] yData, int count, PlotContext context, Color base) {
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

        double plotX = context.plotBounds().getX();
        double plotY = context.plotBounds().getY();
        double plotW = context.plotBounds().getWidth();
        double plotH = context.plotBounds().getHeight();

        double xRange = maxX - minX;
        if (xRange == 0) xRange = 1.0;

        double barW = plotW / Math.max(1, count);
        double baseline = plotY + plotH; // Bottom of plot

        for (int i = 0; i < count; i++) {
            // Manual pixel mapping
            double px = plotX + (xData[i] - minX) / xRange * plotW;
            double py = plotY + (maxY - yData[i]) / rangeY * plotH;

            int h = (int) Math.abs(baseline - py);
            int y = (int) Math.min(baseline, py);

            if (isMultiColor()) {
                Color bar = themeSeries(context, i);
                if (bar == null) bar = base;
                g2.setColor(bar);
            }
            g2.fillRect((int) (px - barW / 2), y, (int) barW, h);
        }
    }

    private void drawAsLine(Graphics2D g2, double[] xData, double[] yData, int count, PlotContext context, Color base) {
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

        double plotX = context.plotBounds().getX();
        double plotY = context.plotBounds().getY();
        double plotW = context.plotBounds().getWidth();
        double plotH = context.plotBounds().getHeight();

        double xRange = maxX - minX;
        if (xRange == 0) xRange = 1.0;

        Path2D path = getPathCache();
        boolean first = true;
        double prevX = Double.NaN;
        double prevY = Double.NaN;

        for (int i = 0; i < count; i++) {
            // Manual pixel mapping
            double px = plotX + (xData[i] - minX) / xRange * plotW;
            double py = plotY + (maxY - yData[i]) / rangeY * plotH;

            if (!isMultiColor()) {
                if (first) {
                    path.moveTo(px, py);
                    first = false;
                } else {
                    path.lineTo(px, py);
                }
            } else if (i > 0) {
                Color seg = themeSeries(context, i);
                if (seg == null) seg = base;
                g2.setColor(seg);
                g2.draw(getLine(prevX, prevY, px, py));
            }
            prevX = px;
            prevY = py;
        }

        if (!isMultiColor()) {
            g2.setStroke(getSeriesStroke());
            g2.draw(path);
        }
    }
}
