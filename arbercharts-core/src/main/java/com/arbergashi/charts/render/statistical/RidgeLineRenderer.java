package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * RidgeLine Renderer for JDK 25.
 * Known as "Joyplot" - ideal for comparing distributions over time.
 * Utilizes a vertical offset based on the x-value to create overlapping ridges.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2026-01-01
 */
public class RidgeLineRenderer extends BaseRenderer {

    private transient Path2D.Double path;
    private transient double[] xs;
    private transient double[] ys;

    public RidgeLineRenderer() {
        super("ridgeline");
    }

    private Path2D.Double getPath() {
        if (path == null) path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        return path;
    }

    @Override
    protected void drawData(Graphics2D g2, ChartModel model, PlotContext context) {
        int n = model.getPointCount();
        if (n == 0) return;
        if (xs == null || xs.length < n) {
            xs = com.arbergashi.charts.tools.RendererAllocationCache.getDoubleArray(this, "ridgeline.xs", n);
            ys = com.arbergashi.charts.tools.RendererAllocationCache.getDoubleArray(this, "ridgeline.ys", n);
        }
        final double baselineY = context.plotBounds().getMaxY();
        final double[] pix = pBuffer();
        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            double y = model.getY(i);
            context.mapToPixel(x, y, pix);
            xs[i] = pix[0];
            ys[i] = pix[1] - (x * 0.5);
        }
        Path2D.Double poly = getPath();
        poly.reset();
        poly.moveTo(xs[0], baselineY);
        poly.lineTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) poly.lineTo(xs[i], ys[i]);
        poly.lineTo(xs[n - 1], baselineY);
        poly.closePath();
        Color baseColor = seriesOrBase(model, context, 0);
        g2.setPaint(getCachedGradient(ColorUtils.withAlpha(baseColor, 0.8f),
                (float) context.plotBounds().getHeight()));
        g2.fill(poly);
        Color multiStroke = themeSeries(context, 1);
        if (multiStroke == null) multiStroke = baseColor;
        Color strokeColor = isMultiColor() ? ColorUtils.withAlpha(multiStroke, 0.7f) : themeBackground(context);
        g2.setColor(strokeColor);
        g2.setStroke(getCachedStroke(ChartScale.scale(1.5f)));
        g2.draw(poly);
    }
}
