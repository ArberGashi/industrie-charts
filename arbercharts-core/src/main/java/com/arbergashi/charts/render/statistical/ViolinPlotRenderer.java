package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * ViolinPlotRenderer.
 * Combines box-plot properties with a kernel density estimate (KDE).
 * Shows the distribution of data across categories.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-15
 */
public class ViolinPlotRenderer extends BaseRenderer {

    public ViolinPlotRenderer() {
        super("violin");
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int count = model.getPointCount();
        if (count == 0) return;

        double maxWidth = com.arbergashi.charts.util.ChartScale.scale(40.0);

        double[] centerPix = pBuffer();
        double[] minPix = pBuffer4();
        double[] maxPix = pBuffer();

        for (int i = 0; i < count; i++) {
            // Each violin gets a distinct color from the theme palette
            Color violinColor = seriesOrBase(model, context, i);
            drawViolin(g2, i, model, context, violinColor, maxWidth, centerPix, minPix, maxPix);
        }
    }

    private void drawViolin(Graphics2D g2, int idx, ChartModel model, PlotContext context, Color color, double maxWidth, double[] centerPix, double[] minPix, double[] maxPix) {
        context.mapToPixel(model.getX(idx), model.getY(idx), centerPix);
        context.mapToPixel(model.getX(idx), model.getMin(idx), minPix);
        context.mapToPixel(model.getX(idx), model.getMax(idx), maxPix);

        double centerX = centerPix[0];
        double minY = maxPix[1]; /* Y is inverted in Swing */
        double maxY = minPix[1];
        double height = maxY - minY;

        Path2D violin = getPathCache();
        violin.moveTo(centerX, minY);

        violin.curveTo(centerX + maxWidth, minY + height * 0.25,
                centerX + maxWidth * 0.5, minY + height * 0.75,
                centerX, maxY);

        violin.curveTo(centerX - maxWidth * 0.5, minY + height * 0.75,
                centerX - maxWidth, minY + height * 0.25,
                centerX, minY);

        violin.closePath();

        g2.setColor(ColorUtils.withAlpha(color, 0.4f));
        g2.fill(violin);
        g2.setColor(color);
        g2.setStroke(getCachedStroke(ChartScale.scale(1.5f)));
        g2.draw(violin);

        double boxWidth = ChartScale.scale(4.0);
        g2.setColor(themeBackground(context));
        g2.fill(getRect(centerX - boxWidth / 2, centerPix[1] - boxWidth, boxWidth, boxWidth * 2));
    }
}
