package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.api.types.ArberColor;
import com.arbergashi.charts.core.rendering.ArberCanvas;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorRegistry;
/**
 * <h1>IchimokuCloudRenderer</h1>
 * <p>
 * Implements the Ichimoku Kinko Hyo indicator (Cloud Chart).
 * Consists of:
 * <ul>
 *     <li>Tenkan-sen (Conversion Line)</li>
 *     <li>Kijun-sen (Base Line)</li>
 *     <li>Senkou Span A (Leading Span A)</li>
 *     <li>Senkou Span B (Leading Span B)</li>
 *     <li>Chikou Span (Lagging Span)</li>
 *     <li>Kumo (Cloud) - The area between Span A and Span B</li>
 * </ul>
 * </p>
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
  * Part of the Zero-Allocation Render Path. High-frequency execution safe.
 *
 */
public final class IchimokuCloudRenderer extends BaseRenderer {

    private final double[] pA = new double[2];
    private final double[] pB = new double[2];
    private final double[] pPrevA = new double[2];
    private final double[] pPrevB = new double[2];
    private final float[] quadX = new float[4];
    private final float[] quadY = new float[4];
    private transient float[] lineX;
    private transient float[] lineY;

    public IchimokuCloudRenderer() {
        super("ichimoku");
    }

    @Override/**
 * @since 1.5.0
 */
    protected void drawData(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n < 52) return; // Requires at least 52 periods for Span B

        // Note: The model must provide the calculated Ichimoku values.
        // We assume the following mapping:
        // component 1: Tenkan
        // component 2: Kijun
        // component 3: Span A
        // component 4: Span B
        // component 5: Chikou

        drawCloud(canvas, model, context);
        drawLine(canvas, model, context, 1, "tenkan");
        drawLine(canvas, model, context, 2, "kijun");
        drawLine(canvas, model, context, 5, "chikou");
    }

    private void drawCloud(ArberCanvas canvas, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        ArberColor upColor = ColorRegistry.applyAlpha(themeBullish(context), 0.2f);
        ArberColor downColor = ColorRegistry.applyAlpha(themeBearish(context), 0.2f);

        for (int i = 1; i < n; i++) {
            double spanA_curr = model.getValue(i, 3);
            double spanB_curr = model.getValue(i, 4);
            double spanA_prev = model.getValue(i - 1, 3);
            double spanB_prev = model.getValue(i - 1, 4);

            if (Double.isNaN(spanA_curr) || Double.isNaN(spanB_curr)) continue;

            context.mapToPixel(model.getX(i-1), spanA_prev, pPrevA);
            context.mapToPixel(model.getX(i-1), spanB_prev, pPrevB);
            context.mapToPixel(model.getX(i), spanA_curr, pA);
            context.mapToPixel(model.getX(i), spanB_curr, pB);

            quadX[0] = (float) pPrevA[0];
            quadY[0] = (float) pPrevA[1];
            quadX[1] = (float) pA[0];
            quadY[1] = (float) pA[1];
            quadX[2] = (float) pB[0];
            quadY[2] = (float) pB[1];
            quadX[3] = (float) pPrevB[0];
            quadY[3] = (float) pPrevB[1];

            canvas.setColor(spanA_curr >= spanB_curr ? upColor : downColor);
            canvas.fillPolygon(quadX, quadY, 4);
        }
    }

    private void drawLine(ArberCanvas canvas, ChartModel model, PlotContext context, int component, String type) {
        int n = model.getPointCount();
        ensureLineCapacity(n);
        int count = 0;

        for (int i = 0; i < n; i++) {
            double val = model.getValue(i, component);
            if (Double.isNaN(val)) continue;

            context.mapToPixel(model.getX(i), val, pA);
            lineX[count] = (float) pA[0];
            lineY[count] = (float) pA[1];
            count++;
        }

        if (count < 2) return;
        canvas.setColor(getLineColor(type, context));
        canvas.setStroke(ChartScale.scale(1.0f));
        canvas.drawPolyline(lineX, lineY, count);
    }

    private void ensureLineCapacity(int count) {
        if (lineX == null || lineX.length < count) {
            lineX = new float[count];
            lineY = new float[count];
        }
    }

    private ArberColor getLineColor(String type, PlotContext context) {
        return switch (type) {
            case "tenkan" -> themeSeries(context, 0);
            case "kijun" -> themeSeries(context, 1);
            case "chikou" -> themeSeries(context, 2);
            default -> themeGrid(context);
        };
    }
}
