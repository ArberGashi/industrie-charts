package com.arbergashi.charts.render.statistical;

import com.arbergashi.charts.api.PlotContext;
import com.arbergashi.charts.model.ChartModel;
import com.arbergashi.charts.render.BaseRenderer;
import com.arbergashi.charts.util.ChartScale;
import com.arbergashi.charts.util.ColorUtils;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Professional, zero-allocation confidence interval renderer.
 *
 * @author Arber Gashi
 * @version 1.0.0
 * @since 2025-06-01
 */
public class ConfidenceIntervalRenderer extends BaseRenderer {

    // Buffer arrays for pixel mapping
    private final double[] topBuf = new double[2];
    private final double[] bottomBuf = new double[2];
    private final double[] meanBuf = new double[2];
    private Shape lastRenderedCloud;
    // Cached dashed stroke for outlines (lazy init)
    private transient Stroke dashedStroke;

    public ConfidenceIntervalRenderer() {
        super("confidence");
    }

    @Override
    protected void drawData(Graphics2D g, ChartModel model, PlotContext context) {
        final int n = model.getPointCount();
        if (n < 2) return;

        final Color meanColor = seriesOrBase(model, context, 0);
        Color boundsColor = isMultiColor() ? themeSeries(context, 1) : meanColor;
        if (boundsColor == null) boundsColor = meanColor;

        Path2D pth = getPathCache();
        pth.reset();

        // Build upper path and draw immediately (uses single shared path cache)
        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            context.mapToPixel(x, model.getValue(i, 4), topBuf); // max
            if (i == 0) pth.moveTo(topBuf[0], topBuf[1]);
            else pth.lineTo(topBuf[0], topBuf[1]);
        }
        // Draw upper path
        g.setColor(ColorUtils.withAlpha(boundsColor, 0.4f));
        g.draw(pth);

        // Build lower path and draw
        pth.reset();
        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            context.mapToPixel(x, model.getValue(i, 3), bottomBuf); // min
            if (i == 0) pth.moveTo(bottomBuf[0], bottomBuf[1]);
            else pth.lineTo(bottomBuf[0], bottomBuf[1]);
        }
        g.setColor(ColorUtils.withAlpha(boundsColor, 0.4f));
        g.draw(pth);

        // Build and draw mean line
        pth.reset();
        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            context.mapToPixel(x, model.getY(i), meanBuf);
            if (i == 0) pth.moveTo(meanBuf[0], meanBuf[1]);
            else pth.lineTo(meanBuf[0], meanBuf[1]);
        }
        g.setStroke(getSeriesStroke());
        g.setColor(meanColor);
        g.draw(pth);

        // Build filled cloud using upper and lower sequences
        Path2D cloudPath = getPathCache();
        cloudPath.reset();
        // upper
        for (int i = 0; i < n; i++) {
            double x = model.getX(i);
            context.mapToPixel(x, model.getValue(i, 4), topBuf);
            if (i == 0) cloudPath.moveTo(topBuf[0], topBuf[1]);
            else cloudPath.lineTo(topBuf[0], topBuf[1]);
        }
        // lower reversed
        for (int i = n - 1; i >= 0; i--) {
            double x = model.getX(i);
            context.mapToPixel(x, model.getValue(i, 3), bottomBuf);
            cloudPath.lineTo(bottomBuf[0], bottomBuf[1]);
        }
        cloudPath.closePath();
        this.lastRenderedCloud = cloudPath;

        g.setPaint(getCachedGradient(boundsColor, (float) (context.plotBounds() == null ? 0 : context.plotBounds().getHeight())));
        g.fill(cloudPath);

        // Use a cached dashed stroke to avoid allocations
        if (dashedStroke == null) {
            float[] dash = com.arbergashi.charts.tools.RendererAllocationCache.getFloatArray(this, "confidence.dash", 2);
            dash[0] = ChartScale.scale(5f);
            dash[1] = ChartScale.scale(3f);
            dashedStroke = com.arbergashi.charts.tools.RendererAllocationCache.getBasicStroke(this, "confidence.dashedStroke", ChartScale.scale(0.8f), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1.0f, dash, 0f);
        }
        g.setStroke(dashedStroke);
        g.setColor(ColorUtils.withAlpha(boundsColor, 0.4f));
        g.draw(cloudPath);
    }

    public Shape getRenderedShape(ChartModel model, PlotContext context) {
        return lastRenderedCloud;
    }
}
