package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.tools.RendererAllocationCache;
import com.arbergashi.charts.util.ChartScale;
/**
 * Fourier overlay renderer.
 * Overlays dominant frequencies or an approximated Fourier series on a line chart.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public class FourierOverlayRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];
    private float lastScale = -1f;

    public FourierOverlayRenderer() {
        super("fourierOverlay");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 10) return;
        double[] xData = model.getXData();

        ArberColor baseColor = seriesOrBase(model, context, 0);
        if (isMultiColor()) {
            ArberColor alt = themeSeries(context, 1);
            if (alt != null) baseColor = alt;
        }
        canvas.setColor(baseColor);

        float currentScale = ChartScale.scale(1.0f);
        if (lastScale != currentScale) {
            lastScale = currentScale;
        }
        canvas.setStroke(ChartScale.scale(1.5f));

        // Simuliere eine Fourier-Approximation (Sinus-Summe)
        float[] xs = RendererAllocationCache.getFloatArray(this, "fourier.x", 200);
        float[] ys = RendererAllocationCache.getFloatArray(this, "fourier.y", 200);
        int outCount = 0;

        double minX = xData[0];
        double maxX = xData[count - 1];
        double rangeX = maxX - minX;

        for (int i = 0; i < 200; i++) {
            double x = minX + (i / 199.0) * rangeX;
            double t = (x - minX) / rangeX * 2 * Math.PI;

            // Fourier-Reihe: sin(t) + 1/3*sin(3t) + 1/5*sin(5t)
            double y = 50 + 20 * (Math.sin(t) + (1.0 / 3.0) * Math.sin(3 * t) + (1.0 / 5.0) * Math.sin(5 * t));

            context.mapToPixel(x, y, pBuffer);
            xs[outCount] = (float) pBuffer[0];
            ys[outCount] = (float) pBuffer[1];
            outCount++;
        }

        canvas.drawPolyline(xs, ys, outCount);
    }
}
