package com.arbergashi.charts.render.financial;


import com.arbergashi.charts.api.ChartTheme;
import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Professional, zero-allocation volume bar renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public final class VolumeRenderer extends BaseRenderer {

    private final double[] pxTop = new double[2];
    private final double[] pxBase = new double[2];

    public VolumeRenderer() {
        super("volume");
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n == 0) return;

        final Rectangle2D viewBounds = g.getClipBounds() != null ? g.getClipBounds() : context.plotBounds();
        final double leftX = viewBounds.getMinX();
        final double rightX = viewBounds.getMaxX();

        final double w = context.plotBounds().getWidth();
        final double barWidth = (w / (double) n) * 0.75;

        final ChartTheme theme = resolveTheme(context);
        final Color bullishColor = theme.getBullishColor();
        final Color bearishColor = theme.getBearishColor();
        final Stroke borderStroke = getCachedStroke(ChartScale.scale(0.5f));

        for (int i = 0; i < n; i++) {
            context.mapToPixel(model.getX(i), model.getY(i), pxTop);

            final double x = pxTop[0] - barWidth / 2.0;
            if (x + barWidth < leftX || x > rightX) continue;

            context.mapToPixel(model.getX(i), 0, pxBase);

            final boolean bullish = model.getValue(i, 2) >= 0; // weight = price change
            final Color barColor = bullish ? bullishColor : bearishColor;

            final double topY = pxTop[1];
            final double baseY = pxBase[1];
            final double barHeight = Math.abs(baseY - topY);
            final double y = Math.min(topY, baseY);

            final var bar = getRect(x, y, barWidth, barHeight);
            g.setPaint(getCachedGradient(barColor, (float) barHeight));
            g.fill(bar);

            g.setColor(ColorUtils.adjustBrightness(barColor, 0.7f));
            g.setStroke(borderStroke);
            g.draw(bar);
        }
    }
}
