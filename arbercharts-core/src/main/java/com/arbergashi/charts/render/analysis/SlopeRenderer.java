package com.arbergashi.charts.render.analysis;


import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartAssets;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
 * Slope chart renderer (minimal): expects exactly two points.
 * Connects both points with a line and marks the endpoints.
 * Useful for simple before/after slope comparisons.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public final class SlopeRenderer extends BaseRenderer {

    private final double[] pBuffer = new double[2];
    private final Line2D.Double lineCache = new Line2D.Double();
    private final Ellipse2D.Double ellipseCache = new Ellipse2D.Double();

    private Font cachedFont;
    private float lastFontSize = -1f;
    private Font lastBaseFont;

    public SlopeRenderer() {
        super("slope");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count != 2) return;

        double[] xData = model.getXData();
        double[] yData = model.getYData();

        context.mapToPixel(xData[0], yData[0], pBuffer);
        double ax = pBuffer[0], ay = pBuffer[1];
        context.mapToPixel(xData[1], yData[1], pBuffer);
        double bx = pBuffer[0], by = pBuffer[1];

        Color c = seriesOrBase(model, context, 0);
        float w = ChartAssets.getFloat("chart.render.slope.width", 2.0f);

        g2.setColor(ColorUtils.withAlpha(c, 0.9f));
        g2.setStroke(getCachedStroke(ChartScale.scale(w), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        lineCache.setLine(ax, ay, bx, by);
        g2.draw(lineCache);

        double r = ChartScale.scale(ChartAssets.getFloat("chart.render.slope.radius", 4.0f));
        if (isMultiColor()) {
            Color c0 = themeSeries(context, 0);
            if (c0 == null) c0 = c;
            g2.setColor(c0);
        } else {
            g2.setColor(c);
        }
        ellipseCache.setFrame(ax - r, ay - r, r * 2, r * 2);
        g2.fill(ellipseCache);
        if (isMultiColor()) {
            Color c1 = themeSeries(context, 1);
            if (c1 == null) c1 = c;
            g2.setColor(c1);
        }
        ellipseCache.setFrame(bx - r, by - r, r * 2, r * 2);
        g2.fill(ellipseCache);

        if (ChartAssets.getBoolean("chart.render.slope.labels", true)) {
            Font baseFont = g2.getFont();
            float targetSize = ChartScale.uiFontSize(baseFont, 10f);

            if (cachedFont == null || lastBaseFont != baseFont || lastFontSize != targetSize) {
                cachedFont = baseFont.deriveFont(Font.PLAIN, targetSize);
                lastBaseFont = baseFont;
                lastFontSize = targetSize;
            }

            Color textColor = resolveTheme(context).getForeground();
            String labelA = model.getLabel(0);
            if (labelA != null && !labelA.isBlank()) {
                drawLabel(g2, labelA, cachedFont, textColor, (float) (ax + r + ChartScale.scale(3.0)), (float) ay);
            }
            String labelB = model.getLabel(1);
            if (labelB != null && !labelB.isBlank()) {
                drawLabel(g2, labelB, cachedFont, textColor, (float) (bx + r + ChartScale.scale(3.0)), (float) by);
            }
        }
    }
}
