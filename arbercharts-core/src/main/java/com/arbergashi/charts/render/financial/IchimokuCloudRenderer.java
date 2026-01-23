package com.arbergashi.charts.render.financial;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;

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
 */
public final class IchimokuCloudRenderer extends BaseRenderer {

    private final double[] pA = new double[2];
    private final double[] pB = new double[2];
    private final double[] pPrevA = new double[2];
    private final double[] pPrevB = new double[2];

    public IchimokuCloudRenderer() {
        super("ichimoku");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n < 52) return; // Requires at least 52 periods for Span B

        // Note: The model must provide the calculated Ichimoku values.
        // We assume the following mapping:
        // component 1: Tenkan
        // component 2: Kijun
        // component 3: Span A
        // component 4: Span B
        // component 5: Chikou

        drawCloud(g2, model, context);
        drawLine(g2, model, context, 1, "tenkan");
        drawLine(g2, model, context, 2, "kijun");
        drawLine(g2, model, context, 5, "chikou");
    }

    private void drawCloud(Graphics2D g2, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        Path2D cloudPath = getPathCache();
        
        Color upColor = ColorUtils.withAlpha(themeBullish(context), 0.2f);
        Color downColor = ColorUtils.withAlpha(themeBearish(context), 0.2f);

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

            cloudPath.reset();
            cloudPath.moveTo(pPrevA[0], pPrevA[1]);
            cloudPath.lineTo(pA[0], pA[1]);
            cloudPath.lineTo(pB[0], pB[1]);
            cloudPath.lineTo(pPrevB[0], pPrevB[1]);
            cloudPath.closePath();

            g2.setColor(spanA_curr >= spanB_curr ? upColor : downColor);
            g2.fill(cloudPath);
        }
    }

    private void drawLine(Graphics2D g2, ChartModel model, PlotContext context, int component, String type) {
        int n = model.getPointCount();
        Path2D path = getPathCache();
        boolean moved = false;

        for (int i = 0; i < n; i++) {
            double val = model.getValue(i, component);
            if (Double.isNaN(val)) continue;

            context.mapToPixel(model.getX(i), val, pA);
            if (!moved) {
                path.moveTo(pA[0], pA[1]);
                moved = true;
            } else {
                path.lineTo(pA[0], pA[1]);
            }
        }

        g2.setColor(getLineColor(type, context));
        g2.setStroke(getLineStroke(type));
        g2.draw(path);
    }

    private Color getLineColor(String type, PlotContext context) {
        return switch (type) {
            case "tenkan" -> themeSeries(context, 0);
            case "kijun" -> themeSeries(context, 1);
            case "chikou" -> themeSeries(context, 2);
            default -> themeGrid(context);
        };
    }

    private Stroke getLineStroke(String type) {
        return getCachedStroke(1.0f);
    }
}
