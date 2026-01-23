package com.arbergashi.charts.render.analysis;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;
import com.arbergashi.charts.tools.RendererAllocationCache;

/**
 * Fourier overlay renderer.
 * Overlays dominant frequencies or an approximated Fourier series on a line chart.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class FourierOverlayRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];
    private Stroke cachedStroke;
    private float lastScale = -1f;

    public FourierOverlayRenderer() {
        super("fourierOverlay");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count < 10) return;
        double[] xData = model.getXData();

        Color baseColor = seriesOrBase(model, context, 0);
        if (isMultiColor()) {
            Color alt = themeSeries(context, 1);
            if (alt != null) baseColor = alt;
        }
        g2.setColor(ColorUtils.withAlpha(baseColor, 0.6f));

        float currentScale = ChartScale.scale(1.0f);
        if (cachedStroke == null || lastScale != currentScale) {
            float dashLen = ChartScale.scale(5f);
            float[] dash = RendererAllocationCache.getFloatArray(this, "dash_cachedStroke", 2);
            dash[0] = dashLen;
            dash[1] = dashLen;
            cachedStroke = RendererAllocationCache.getBasicStroke(this, "cachedStroke", ChartScale.scale(1.5f), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f);
            lastScale = currentScale;
        }
        g2.setStroke(cachedStroke);

        // Simuliere eine Fourier-Approximation (Sinus-Summe)
        Path2D path = getPathCache();
        boolean first = true;

        double minX = xData[0];
        double maxX = xData[count - 1];
        double rangeX = maxX - minX;

        for (int i = 0; i < 200; i++) {
            double x = minX + (i / 199.0) * rangeX;
            double t = (x - minX) / rangeX * 2 * Math.PI;

            // Fourier-Reihe: sin(t) + 1/3*sin(3t) + 1/5*sin(5t)
            double y = 50 + 20 * (Math.sin(t) + (1.0 / 3.0) * Math.sin(3 * t) + (1.0 / 5.0) * Math.sin(5 * t));

            context.mapToPixel(x, y, pBuffer);
            if (first) {
                path.moveTo(pBuffer[0], pBuffer[1]);
                first = false;
            } else {
                path.lineTo(pBuffer[0], pBuffer[1]);
            }
        }

        g2.draw(path);
    }
}
