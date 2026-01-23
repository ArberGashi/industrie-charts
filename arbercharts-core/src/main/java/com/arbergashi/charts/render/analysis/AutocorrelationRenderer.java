package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;

import java.awt.*;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Autocorrelation (ACF) renderer.
 *
 * <p>Renders a classic correlation-by-lag bar chart computed from the Y values.
 * Points are interpreted as an ordered series. The X values are ignored.</p>
 *
 * <p><b>Performance:</b> O(n * L) where L is limited to {@code &lt;= 128} by default.</p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2024-06-01
 */
public final class AutocorrelationRenderer extends BaseRenderer {

    private double[] centeredBuffer = new double[256];

    public AutocorrelationRenderer() {
        super("autocorrelation");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 4) return;
        double[] yData = model.getYData();

        int maxLag = Math.min(128, count / 2);

        // Mean
        double mean = 0.0;
        for (int i = 0; i < count; i++) mean += yData[i];
        mean /= count;

        // Precompute centered series once (reduces inner-loop work).
        if (centeredBuffer.length < count) centeredBuffer = RendererAllocationCache.getDoubleArray(this, "centeredBuffer", Math.max(count, centeredBuffer.length * 2));

        for (int i = 0; i < count; i++) centeredBuffer[i] = yData[i] - mean;

        // Variance (denominator)
        double denom = 0.0;
        for (int i = 0; i < count; i++) {
            double d = centeredBuffer[i];
            denom = Math.fma(d, d, denom);
        }
        if (denom <= 1e-12) return;

        Color c = seriesOrBase(model, context, 0);

        // CRITICAL: Use LAG-based X-axis (0 to maxLag) instead of model X-data
        double plotX = context.plotBounds().getX();
        double plotY = context.plotBounds().getY();
        double plotW = context.plotBounds().getWidth();
        double plotH = context.plotBounds().getHeight();

        // Manual scaling for LAG axis (X: 0 to maxLag)
        double xScale = plotW / (maxLag + 1.0);

        // Manual scaling for ACF axis (Y: -1 to 1)
        double yMin = -1.0;
        double yMax = 1.0;
        double yRange = yMax - yMin;

        // Baseline at ACF = 0
        double baseY = plotY + plotH * (yMax - 0.0) / yRange;

        double barW = Math.max(ChartScale.scale(2.0), xScale * 0.8);

        for (int lag = 1; lag <= maxLag; lag++) {
            double num = 0.0;
            for (int i = 0; i < count - lag; i++) {
                num = Math.fma(centeredBuffer[i], centeredBuffer[i + lag], num);
            }
            double acf = num / denom; // [-1, 1]

            // Manual pixel mapping
            double px = plotX + lag * xScale;
            double py = plotY + plotH * (yMax - acf) / yRange;

            // Clamp to plot bounds
            if (px < plotX || px > plotX + plotW) {
                continue;
            }

            double top = Math.min(py, baseY);
            double h = Math.abs(py - baseY);


            if (isMultiColor()) {
                Color bar = themeSeries(context, lag);
                if (bar == null) bar = c;
                g2.setPaint(getCachedGradient(bar, (float) h));
            } else {
                g2.setPaint(getCachedGradient(c, (float) h));
            }
            g2.fill(getRect(px - barW * 0.5, top, barW, h));
        }
    }
}
